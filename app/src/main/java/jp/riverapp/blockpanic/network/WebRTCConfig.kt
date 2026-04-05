package jp.riverapp.blockpanic.network

import android.content.Context
import kotlinx.coroutines.delay
import org.webrtc.*

/**
 * WebRTC configuration matching Web/iOS versions.
 * Port from iOS WebRTCConfig.swift
 */
object WebRTCConfig {
    private var initialized = false

    lateinit var factory: PeerConnectionFactory
        private set

    val stunConfig: PeerConnection.RTCConfiguration by lazy {
        PeerConnection.RTCConfiguration(
            listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
        ).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
    }

    val dataChannelConfig: DataChannel.Init by lazy {
        DataChannel.Init().apply {
            ordered = true
        }
    }

    fun initialize(context: Context) {
        if (initialized) return
        initialized = true

        val initOptions = PeerConnectionFactory.InitializationOptions.builder(context)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initOptions)

        factory = PeerConnectionFactory.builder()
            .createPeerConnectionFactory()
    }

    /** Wait for ICE gathering to complete (5s timeout, same as iOS/Web) */
    suspend fun waitForICEGathering(pc: PeerConnection): String? {
        if (pc.iceGatheringState() == PeerConnection.IceGatheringState.COMPLETE) {
            return pc.localDescription?.description
        }

        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 5000) {
            if (pc.iceGatheringState() == PeerConnection.IceGatheringState.COMPLETE) {
                return pc.localDescription?.description
            }
            delay(100)
        }
        // Timeout -- return whatever we have
        return pc.localDescription?.description
    }
}
