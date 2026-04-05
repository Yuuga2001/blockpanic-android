package jp.riverapp.blockpanic.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import jp.riverapp.blockpanic.engine.C
import jp.riverapp.blockpanic.engine.StateMessage
import kotlinx.coroutines.*
import org.webrtc.*
import java.nio.ByteBuffer
import java.nio.charset.Charset
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * WebRTC host: creates room, accepts peer connections, broadcasts game state.
 * Port from iOS PeerHost.swift
 * CRITICAL: null listeners before close() to prevent crashes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PeerHost {
    private val TAG = "PeerHost"
    private val signaling = SignalingClient()
    private val gson = Gson()
    private val mainHandler = Handler(Looper.getMainLooper())

    var roomId: String = ""
        private set
    private val peers = mutableMapOf<String, Peer>()
    private var pollJob: Job? = null
    private var heartbeatJob: Job? = null
    private val answeredPeers = mutableSetOf<String>()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Callbacks
    var onPeerJoin: ((String, String) -> Unit)? = null      // (peerId, name)
    var onPeerInput: ((String, Boolean, Boolean, Boolean) -> Unit)? = null
    var onPeerGiveUp: ((String) -> Unit)? = null
    var onPeerDisconnect: ((String) -> Unit)? = null

    private data class Peer(
        val id: String,
        val pc: PeerConnection,
        var channel: DataChannel?,
        var playerId: String,
        var connected: Boolean
    )

    suspend fun createRoom(hostName: String): String {
        val id = signaling.createRoom(hostName)
        roomId = id

        // Poll offers every 2s
        pollJob = scope.launch {
            while (isActive) {
                delay(2000)
                pollForOffers()
            }
        }

        // Heartbeat every 30s
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(30000)
                updatePlayerCount()
            }
        }

        return id
    }

    private suspend fun pollForOffers() {
        if (roomId.isEmpty()) return
        try {
            val signals = signaling.getSignals(roomId)
            for (signal in signals) {
                if (signal.peerId in answeredPeers) continue
                val offer = signal.offer ?: continue

                if (1 + getConnectedPeerCount() >= C.maxRoomPlayers) break

                answeredPeers.add(signal.peerId)
                try {
                    handleOffer(signal.peerId, offer)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to handle offer: $e")
                    answeredPeers.remove(signal.peerId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Polling error: $e")
        }
    }

    private suspend fun handleOffer(peerId: String, offerSdp: String) {
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
                        mainHandler.post { handlePeerDisconnect(peerId) }
                    }
                }
                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
                override fun onAddStream(stream: MediaStream?) {}
                override fun onRemoveStream(stream: MediaStream?) {}
                override fun onDataChannel(dc: DataChannel?) {
                    dc?.let { mainHandler.post { setupChannel(peerId, it) } }
                }
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
            }
        ) ?: return

        peers[peerId] = Peer(peerId, pc, null, "", false)

        // Set remote description (offer)
        val offerDesc = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
        suspendCancellableCoroutine<Unit> { cont ->
            pc.setRemoteDescription(object : SdpObserver {
                override fun onSetSuccess() { cont.resume(Unit) {} }
                override fun onSetFailure(error: String?) { cont.resumeWithException(Exception(error)) }
                override fun onCreateSuccess(desc: SessionDescription?) {}
                override fun onCreateFailure(error: String?) {}
            }, offerDesc)
        }

        // Create answer
        val answer = suspendCancellableCoroutine<SessionDescription> { cont ->
            pc.createAnswer(object : SdpObserver {
                override fun onCreateSuccess(desc: SessionDescription?) {
                    desc?.let { cont.resume(it) {} } ?: cont.resumeWithException(Exception("No answer created"))
                }
                override fun onCreateFailure(error: String?) { cont.resumeWithException(Exception(error)) }
                override fun onSetSuccess() {}
                override fun onSetFailure(error: String?) {}
            }, MediaConstraints())
        }

        // Set local description
        suspendCancellableCoroutine<Unit> { cont ->
            pc.setLocalDescription(object : SdpObserver {
                override fun onSetSuccess() { cont.resume(Unit) {} }
                override fun onSetFailure(error: String?) { cont.resumeWithException(Exception(error)) }
                override fun onCreateSuccess(desc: SessionDescription?) {}
                override fun onCreateFailure(error: String?) {}
            }, answer)
        }

        // Wait for ICE gathering
        val sdp = WebRTCConfig.waitForICEGathering(pc) ?: return

        // Post answer to signaling server
        signaling.postSignal(roomId, peerId, "answer", sdp)
    }

    private fun setupChannel(peerId: String, channel: DataChannel) {
        val peer = peers[peerId] ?: return
        peers[peerId] = peer.copy(channel = channel)

        channel.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(previousAmount: Long) {}
            override fun onStateChange() {
                when (channel.state()) {
                    DataChannel.State.OPEN -> {
                        peers[peerId]?.let { peers[peerId] = it.copy(connected = true) }
                    }
                    DataChannel.State.CLOSED -> mainHandler.post { handlePeerDisconnect(peerId) }
                    else -> {}
                }
            }
            override fun onMessage(buffer: DataChannel.Buffer) {
                val data = ByteArray(buffer.data.remaining())
                buffer.data.get(data)
                mainHandler.post { handlePeerMessage(peerId, data) }
            }
        })
    }

    private fun handlePeerMessage(peerId: String, data: ByteArray) {
        try {
            val json = gson.fromJson(String(data), Map::class.java)
            when (json["type"]) {
                "join" -> {
                    val name = ((json["name"] as? String) ?: "Player").take(20).trim().ifEmpty { "Player" }
                    onPeerJoin?.invoke(peerId, name)
                }
                "input" -> {
                    val left = json["left"] as? Boolean ?: return
                    val right = json["right"] as? Boolean ?: return
                    val jump = json["jump"] as? Boolean ?: return
                    onPeerInput?.invoke(peerId, left, right, jump)
                }
                "giveup" -> onPeerGiveUp?.invoke(peerId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Message parse error: $e")
        }
    }

    private fun handlePeerDisconnect(peerId: String) {
        val peer = peers.remove(peerId) ?: return
        // CRITICAL: null delegates before close() to prevent crash
        peer.channel?.unregisterObserver()
        peer.pc.close()
        onPeerDisconnect?.invoke(peerId)
    }

    // MARK: - State broadcast

    fun broadcastState(state: StateMessage) {
        for ((_, peer) in peers) {
            if (!peer.connected) continue
            val channel = peer.channel ?: continue
            if (channel.state() != DataChannel.State.OPEN) continue

            val peerState = state.copy(playerId = peer.playerId)
            try {
                val json = gson.toJson(peerState)
                val buf = ByteBuffer.wrap(json.toByteArray(Charset.forName("UTF-8")))
                channel.send(DataChannel.Buffer(buf, false))
            } catch (_: Exception) {}
        }
    }

    fun sendJoined(peerId: String, playerId: String) {
        val peer = peers[peerId] ?: return
        peers[peerId] = peer.copy(playerId = playerId)

        val channel = peer.channel ?: return
        if (channel.state() != DataChannel.State.OPEN) return
        val msg = gson.toJson(mapOf("type" to "joined", "playerId" to playerId))
        val buf = ByteBuffer.wrap(msg.toByteArray(Charset.forName("UTF-8")))
        channel.send(DataChannel.Buffer(buf, false))
    }

    fun sendGameOver(peerId: String, score: Int) {
        val channel = peers[peerId]?.channel ?: return
        if (channel.state() != DataChannel.State.OPEN) return
        val msg = gson.toJson(mapOf("type" to "gameover", "score" to score))
        val buf = ByteBuffer.wrap(msg.toByteArray(Charset.forName("UTF-8")))
        channel.send(DataChannel.Buffer(buf, false))
    }

    fun getConnectedPeerCount(): Int = peers.values.count { it.connected }

    suspend fun updatePlayerCount() {
        if (roomId.isEmpty()) return
        val count = 1 + getConnectedPeerCount()
        try { signaling.updateRoom(roomId, count) } catch (_: Exception) {}
    }

    suspend fun destroy() {
        pollJob?.cancel()
        heartbeatJob?.cancel()
        // CRITICAL: null delegates before close
        for ((_, peer) in peers) {
            peer.channel?.unregisterObserver()
            peer.pc.close()
        }
        peers.clear()
        val rid = roomId
        roomId = ""
        if (rid.isNotEmpty()) {
            try { signaling.deleteRoom(rid) } catch (_: Exception) {}
        }
        // Null all callbacks
        onPeerJoin = null
        onPeerInput = null
        onPeerGiveUp = null
        onPeerDisconnect = null
    }
}
