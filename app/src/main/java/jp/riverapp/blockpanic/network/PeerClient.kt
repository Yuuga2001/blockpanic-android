package jp.riverapp.blockpanic.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import jp.riverapp.blockpanic.engine.StateMessage
import kotlinx.coroutines.*
import org.webrtc.*
import java.nio.ByteBuffer
import java.nio.charset.Charset
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * WebRTC client: connects to host room via signaling.
 * Port from iOS PeerClient.swift
 * CRITICAL: null all callbacks in disconnect(), sessionId pattern.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PeerClient {
    private val TAG = "PeerClient"
    private val signaling = SignalingClient()
    private val gson = Gson()
    private val mainHandler = Handler(Looper.getMainLooper())
    val peerId = "peer-${System.currentTimeMillis()}-${(0..0xFFFFFF).random().toString(36)}"

    private var pc: PeerConnection? = null
    private var channel: DataChannel? = null
    private var pollJob: Job? = null
    private var intentionalClose = false

    // Callbacks
    var onStateUpdate: ((StateMessage) -> Unit)? = null
    var onGameOverEvent: ((Int) -> Unit)? = null
    var onJoinedEvent: ((String) -> Unit)? = null
    var onErrorEvent: ((String) -> Unit)? = null
    var onDisconnectEvent: (() -> Unit)? = null

    val isConnected: Boolean
        get() = channel?.state() == DataChannel.State.OPEN

    /**
     * Connect to room. Suspends until DataChannel is open.
     * Throws PeerError on timeout/room not found.
     */
    suspend fun connect(roomId: String) {
        val pc = WebRTCConfig.factory.createPeerConnection(
            WebRTCConfig.stunConfig,
            object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate?) {}
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    if (state == PeerConnection.IceConnectionState.DISCONNECTED ||
                        state == PeerConnection.IceConnectionState.FAILED ||
                        state == PeerConnection.IceConnectionState.CLOSED) {
                        if (!intentionalClose) {
                            mainHandler.post { onDisconnectEvent?.invoke() }
                        }
                    }
                }
                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
                override fun onAddStream(stream: MediaStream?) {}
                override fun onRemoveStream(stream: MediaStream?) {}
                override fun onDataChannel(dc: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
            }
        ) ?: throw PeerError.ICE_FAILED
        this.pc = pc

        // Create DataChannel
        val channel = pc.createDataChannel("game", WebRTCConfig.dataChannelConfig)
            ?: throw PeerError.ICE_FAILED
        this.channel = channel

        // Wait for DataChannel open using continuation
        suspendCancellableCoroutine<Unit> { cont ->
            var resumed = false

            channel.registerObserver(object : DataChannel.Observer {
                override fun onBufferedAmountChange(previousAmount: Long) {}
                override fun onStateChange() {
                    when (channel.state()) {
                        DataChannel.State.OPEN -> {
                            if (!resumed) { resumed = true; cont.resume(Unit) {} }
                        }
                        DataChannel.State.CLOSED -> {
                            if (!intentionalClose) {
                                mainHandler.post { onDisconnectEvent?.invoke() }
                            }
                        }
                        else -> {}
                    }
                }
                override fun onMessage(buffer: DataChannel.Buffer) {
                    val data = ByteArray(buffer.data.remaining())
                    buffer.data.get(data)
                    mainHandler.post { handleMessage(data) }
                }
            })

            // Create offer
            val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
            scope.launch {
                try {
                    val offer = suspendCancellableCoroutine<SessionDescription> { offerCont ->
                        pc.createOffer(object : SdpObserver {
                            override fun onCreateSuccess(desc: SessionDescription?) {
                                desc?.let { offerCont.resume(it) {} }
                                    ?: offerCont.resumeWithException(PeerError.ICE_FAILED)
                            }
                            override fun onCreateFailure(error: String?) {
                                offerCont.resumeWithException(Exception(error))
                            }
                            override fun onSetSuccess() {}
                            override fun onSetFailure(error: String?) {}
                        }, MediaConstraints())
                    }

                    // Set local description
                    suspendCancellableCoroutine<Unit> { setCont ->
                        pc.setLocalDescription(object : SdpObserver {
                            override fun onSetSuccess() { setCont.resume(Unit) {} }
                            override fun onSetFailure(error: String?) { setCont.resumeWithException(Exception(error)) }
                            override fun onCreateSuccess(desc: SessionDescription?) {}
                            override fun onCreateFailure(error: String?) {}
                        }, offer)
                    }

                    // Wait for ICE gathering
                    val sdp = WebRTCConfig.waitForICEGathering(pc) ?: run {
                        if (!resumed) { resumed = true; cont.resumeWithException(PeerError.ICE_FAILED) }
                        return@launch
                    }

                    // Post offer to signaling
                    signaling.postSignal(roomId, peerId, "offer", sdp)

                    // Poll for answer (1s interval)
                    var pollCount = 0
                    pollJob = scope.launch {
                        while (isActive) {
                            delay(1000)
                            pollCount++
                            try {
                                // Every 3 polls, check room existence
                                if (pollCount % 3 == 0) {
                                    val rooms = signaling.listRooms()
                                    if (rooms.none { it.roomId == roomId }) {
                                        pollJob?.cancel()
                                        if (!resumed) { resumed = true; cont.resumeWithException(PeerError.ROOM_NOT_FOUND) }
                                        return@launch
                                    }
                                }

                                val signal = signaling.getSignal(roomId, peerId)
                                if (signal?.answer != null) {
                                    pollJob?.cancel()
                                    val answerDesc = SessionDescription(SessionDescription.Type.ANSWER, signal.answer)
                                    suspendCancellableCoroutine<Unit> { ansCont ->
                                        pc.setRemoteDescription(object : SdpObserver {
                                            override fun onSetSuccess() { ansCont.resume(Unit) {} }
                                            override fun onSetFailure(error: String?) { ansCont.resumeWithException(Exception(error)) }
                                            override fun onCreateSuccess(desc: SessionDescription?) {}
                                            override fun onCreateFailure(error: String?) {}
                                        }, answerDesc)
                                    }
                                    return@launch
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Signal polling error: $e")
                            }
                        }
                    }

                    // 10s timeout
                    delay(10000)
                    if (channel.state() != DataChannel.State.OPEN) {
                        pollJob?.cancel()
                        if (!resumed) { resumed = true; cont.resumeWithException(PeerError.TIMEOUT) }
                    }
                } catch (e: Exception) {
                    if (!resumed) { resumed = true; cont.resumeWithException(e) }
                }
            }
        }
    }

    private fun handleMessage(data: ByteArray) {
        try {
            val json = gson.fromJson(String(data), Map::class.java)
            when (json["type"]) {
                "state" -> {
                    val stateMsg = gson.fromJson(String(data), StateMessage::class.java) ?: return
                    onStateUpdate?.invoke(stateMsg)
                }
                "gameover" -> {
                    val score = (json["score"] as? Number)?.toInt() ?: 0
                    onGameOverEvent?.invoke(score)
                }
                "joined" -> {
                    val playerId = json["playerId"] as? String ?: return
                    onJoinedEvent?.invoke(playerId)
                }
                "error" -> {
                    val message = json["message"] as? String ?: "Unknown error"
                    onErrorEvent?.invoke(message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Message parse error: $e")
        }
    }

    fun send(message: Map<String, Any>) {
        val ch = channel ?: return
        if (ch.state() != DataChannel.State.OPEN) return
        try {
            val json = gson.toJson(message)
            val buf = ByteBuffer.wrap(json.toByteArray(Charset.forName("UTF-8")))
            ch.send(DataChannel.Buffer(buf, false))
        } catch (_: Exception) {}
    }

    fun sendInput(left: Boolean, right: Boolean, jump: Boolean) {
        send(mapOf("type" to "input", "left" to left, "right" to right, "jump" to jump))
    }

    fun sendJoin(name: String) {
        send(mapOf("type" to "join", "name" to name))
    }

    fun sendGiveUp() {
        send(mapOf("type" to "giveup"))
    }

    /**
     * CRITICAL: null all callbacks AND delegates BEFORE close() to prevent crashes.
     */
    fun disconnect() {
        intentionalClose = true
        // Null all callbacks (prevent stale callback interference)
        onStateUpdate = null
        onGameOverEvent = null
        onJoinedEvent = null
        onErrorEvent = null
        onDisconnectEvent = null
        // Cancel polling
        pollJob?.cancel()
        pollJob = null
        // Unregister observers before close
        channel?.unregisterObserver()
        // Close resources
        channel?.close()
        pc?.close()
        pc = null
        channel = null
    }
}

sealed class PeerError(message: String) : Exception(message) {
    object TIMEOUT : PeerError("Connection timeout")
    object ROOM_NOT_FOUND : PeerError("Room no longer exists")
    object ICE_FAILED : PeerError("ICE gathering failed")
}
