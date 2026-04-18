package jp.riverapp.blockpanic.game

import android.os.Handler
import android.os.HandlerThread
import jp.riverapp.blockpanic.engine.*
import kotlin.random.Random

/**
 * Local game engine with 60fps game loop on a dedicated HandlerThread.
 * Port from iOS LocalGameEngine.swift
 *
 * CRITICAL: All input writes must be posted to the game thread to avoid race conditions.
 * The game loop (tick) runs on HandlerThread, UI input comes from main thread.
 */
class LocalGameEngine {
    var gameState: GameState = GameState()
        private set

    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private var lastTime: Long = 0
    private var broadcastAccum: Double = 0.0
    private var running = false

    // Callbacks (invoked on game thread, post to main if needed)
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
    /** 死亡通知を発火済みの playerId. 毎tickでの重複発火を防止し、死亡演出後にプレイヤーを削除する */
    private val remoteDeathFired = mutableSetOf<String>()

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
        remoteDeathFired.clear()
        start()
    }

    fun join(name: String): String {
        playerName = name
        // 古い自分の死体を削除（rejoin時に死んだプレイヤーが残ってuserCountが増え続ける問題を防止）
        if (lastSelfId.isNotEmpty()) {
            val old = gameState.getPlayer(lastSelfId)
            if (old != null && !old.alive) {
                gameState.removePlayer(lastSelfId)
            }
        }
        val id = "p-${System.currentTimeMillis()}-${Random.nextInt(0, 0xFFFF).toString(36)}"
        val player = gameState.addPlayer(id, name)
        playerId = player.id
        lastSelfId = player.id
        return id
    }

    /**
     * Apply local player input. Posts to game thread for thread safety.
     * CRITICAL: Must not write player.input directly from main thread.
     */
    fun applyInput(left: Boolean, right: Boolean, jump: Boolean) {
        handler?.post {
            val player = gameState.getPlayer(playerId) ?: return@post
            player.input = InputState(left, right, jump)
        }
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

    /**
     * Apply remote player input. Posts to game thread for thread safety.
     */
    fun applyRemoteInput(peerId: String, left: Boolean, right: Boolean, jump: Boolean) {
        handler?.post {
            val pid = peerPlayerMap[peerId] ?: return@post
            val player = gameState.getPlayer(pid) ?: return@post
            player.input = InputState(left, right, jump)
        }
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

        // Check if remote players died (一度だけ発火. その後 1秒で削除して死亡演出と重複発火を両立)
        for ((peerId, pid) in peerPlayerMap.toList()) {
            val p = gameState.getPlayer(pid) ?: continue
            if (p.alive) continue
            if (remoteDeathFired.contains(pid)) continue
            remoteDeathFired.add(pid)
            val score = p.score
            onRemotePlayerDied?.invoke(peerId, score)
            val deadPid = pid
            handler?.postDelayed({
                gameState.removePlayer(deadPid)
                remoteDeathFired.remove(deadPid)
                // peerPlayerMap が古い pid を指していれば解除 (rejoin で更新されていれば触らない)
                val staleKeys = peerPlayerMap.entries.filter { it.value == deadPid }.map { it.key }
                for (k in staleKeys) peerPlayerMap.remove(k)
            }, 1000)
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
