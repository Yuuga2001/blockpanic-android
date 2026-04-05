package jp.riverapp.blockpanic.game

import android.os.Handler
import android.os.HandlerThread
import jp.riverapp.blockpanic.engine.*
import kotlin.random.Random

/**
 * Local game engine with 60fps game loop on a dedicated HandlerThread.
 * Port from iOS LocalGameEngine.swift
 */
class LocalGameEngine {
    var gameState: GameState = GameState()
        private set

    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private var lastTime: Long = 0
    private var broadcastAccum: Double = 0.0
    private var running = false

    // Callbacks
    var onState: ((List<PlayerState>, List<BlockState>, List<CoinState>, List<MysteryItemState>, Int) -> Unit)? = null
    var onStateMessage: ((StateMessage) -> Unit)? = null
    var onGameOver: ((Int) -> Unit)? = null
    var onRemotePlayerDied: ((String, Int) -> Unit)? = null

    var playerId: String = ""
        private set
    /** Death animation: keep self ID after death */
    var lastSelfId: String = ""
        private set
    var playerName: String = ""
        private set

    /** peerId -> playerId mapping (remote player management) */
    private val peerPlayerMap = mutableMapOf<String, String>()

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            tick()
            handler?.postDelayed(this, 16) // ~60fps
        }
    }

    fun start() {
        if (running) return
        running = true
        lastTime = System.nanoTime()
        val ht = HandlerThread("GameEngineThread").also { it.start() }
        handlerThread = ht
        handler = Handler(ht.looper)
        handler?.post(tickRunnable)
    }

    fun stop() {
        running = false
        handler?.removeCallbacksAndMessages(null)
        handlerThread?.quitSafely()
        handlerThread = null
        handler = null
    }

    fun restart() {
        stop()
        gameState = GameState()
        playerId = ""
        peerPlayerMap.clear()
        start()
    }

    fun join(name: String): String {
        playerName = name
        val id = "p-${System.currentTimeMillis()}-${Random.nextInt(0, 0xFFFF).toString(36)}"
        val player = gameState.addPlayer(id, name)
        playerId = player.id
        lastSelfId = player.id
        return id
    }

    fun applyInput(left: Boolean, right: Boolean, jump: Boolean) {
        val player = gameState.getPlayer(playerId) ?: return
        player.input = InputState(left, right, jump)
    }

    // MARK: - Remote player management (host mode)

    fun addRemotePlayer(peerId: String, name: String): String {
        val id = "p-${System.currentTimeMillis()}-${Random.nextInt(0, 0xFFFF).toString(36)}"
        gameState.addPlayer(id, name)
        peerPlayerMap[peerId] = id
        return id
    }

    fun removeRemotePlayer(peerId: String) {
        val pid = peerPlayerMap.remove(peerId) ?: return
        gameState.removePlayer(pid)
    }

    fun applyRemoteInput(peerId: String, left: Boolean, right: Boolean, jump: Boolean) {
        val pid = peerPlayerMap[peerId] ?: return
        val player = gameState.getPlayer(pid) ?: return
        player.input = InputState(left, right, jump)
    }

    fun playerIdForPeer(peerId: String): String? = peerPlayerMap[peerId]

    val totalPlayerCount: Int get() = 1 + peerPlayerMap.size

    fun giveUp() {
        val player = gameState.getPlayer(playerId) ?: return
        player.alive = false
        val score = player.score
        playerId = ""
        onGameOver?.invoke(score)
    }

    fun remoteGiveUp(peerId: String) {
        val pid = peerPlayerMap[peerId] ?: return
        val player = gameState.getPlayer(pid) ?: return
        player.alive = false
    }

    private fun tick() {
        val now = System.nanoTime()
        if (lastTime == 0L) { lastTime = now; return }
        val dt = (now - lastTime) / 1_000_000.0 // ms
        lastTime = now

        val dtMs = dt.coerceAtMost(50.0)
        gameState.tick(dtMs)

        // Check if local player died
        if (playerId.isNotEmpty()) {
            val p = gameState.getPlayer(playerId)
            if (p != null && !p.alive) {
                val score = p.score
                playerId = ""
                onGameOver?.invoke(score)
            }
        }

        // Check if remote players died
        for ((peerId, pid) in peerPlayerMap) {
            val p = gameState.getPlayer(pid)
            if (p != null && !p.alive) {
                onRemotePlayerDied?.invoke(peerId, p.score)
            }
        }

        // Broadcast at 20Hz
        broadcastAccum += dtMs
        if (broadcastAccum >= C.broadcastMs) {
            broadcastAccum -= C.broadcastMs
            val state = gameState.getWorldState()
            onState?.invoke(state.players, state.blocks, state.coins, state.mysteryItems, state.roomElapsed)

            val msg = StateMessage(
                playerId = playerId,
                players = state.players,
                blocks = state.blocks,
                coins = state.coins,
                mysteryItems = state.mysteryItems,
                roomElapsed = state.roomElapsed,
                timestamp = System.currentTimeMillis().toDouble()
            )
            onStateMessage?.invoke(msg)
        }
    }
}
