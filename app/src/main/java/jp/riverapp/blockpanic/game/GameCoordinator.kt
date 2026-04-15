package jp.riverapp.blockpanic.game

import android.os.Handler
import android.os.Looper
import jp.riverapp.blockpanic.engine.*
import jp.riverapp.blockpanic.i18n.L
import jp.riverapp.blockpanic.model.GameMode
import jp.riverapp.blockpanic.model.GameRecord
import jp.riverapp.blockpanic.model.GameRecordStore
import jp.riverapp.blockpanic.network.PeerClient
import jp.riverapp.blockpanic.network.PeerHost
import jp.riverapp.blockpanic.network.SignalingClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

/**
 * 3-mode game orchestrator (local/p2pHost/p2pClient).
 * Uses Kotlin StateFlow instead of @Published.
 * Port from iOS GameCoordinator.swift
 */
enum class GameScreen {
    START, ROOM_LIST, CONNECTING, GAME, GAME_OVER, HOST_DISCONNECTED,
    RECORDS, HOW_TO_PLAY, SETTINGS, LANGUAGE_SELECT
}

class GameCoordinator {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _screenState = MutableStateFlow(GameScreen.START)
    val screenState: StateFlow<GameScreen> = _screenState

    var currentScreen: GameScreen
        get() = _screenState.value
        set(value) { _screenState.value = value }

    var survivalTime: Int = 0
    var finalScore: Int = 0
    var playerCount: Int = 0
    var roomElapsed: Int = 0
    var connectionError: String? = null
    var roomName: String = ""

    // Latest state for rendering (read by GameSurfaceView on render thread)
    @Volatile var players: List<PlayerState> = emptyList()
    @Volatile var blocks: List<BlockState> = emptyList()
    @Volatile var coins: List<CoinState> = emptyList()
    @Volatile var mysteryItems: List<MysteryItemState> = emptyList()

    var engine = LocalGameEngine()
        private set
    private var peerHost: PeerHost? = null
    private var peerClient: PeerClient? = null
    var mode: GameMode = GameMode.LOCAL
        private set
    private var joinedAt: Long = 0
    private var lastStateTime: Long = 0
    private var hostTimeoutJob: Job? = null
    private var sessionId: Int = 0
    /// バックグラウンド遷移時の遅延クリーンアップ用
    private var backgroundCleanupJob: Job? = null

    val effectivePlayerId: String
        get() = if (mode == GameMode.P2P_CLIENT) clientPlayerId else engine.playerId

    val effectiveLastSelfId: String
        get() = if (mode == GameMode.P2P_CLIENT) clientLastSelfId else engine.lastSelfId

    private var clientPlayerId: String = ""
    private var clientLastSelfId: String = ""
    private var clientPlayerName: String = ""

    init {
        engine.start()
        setupEngineCallbacks()
    }

    /** Called by GameSurfaceView's render thread to tick the engine at 60fps */
    fun tickIfHost() {
        // The engine runs on its own HandlerThread, so nothing needed here.
        // The render thread just reads the latest state.
    }

    private fun setupEngineCallbacks() {
        engine.onState = { players, blocks, coins, mystery, roomElapsed ->
            mainHandler.post {
                this.players = players
                this.blocks = blocks
                this.coins = coins
                this.mysteryItems = mystery
                this.roomElapsed = roomElapsed
                this.playerCount = players.count { it.alive }
                if (joinedAt > 0 && engine.playerId.isNotEmpty() && currentScreen == GameScreen.GAME) {
                    survivalTime = ((System.currentTimeMillis() - joinedAt) / 1000).toInt()
                }
            }
        }

        engine.onStateMessage = { stateMsg ->
            peerHost?.broadcastState(stateMsg)
        }

        engine.onGameOver = { score ->
            mainHandler.post {
                finalScore = score
                if (joinedAt > 0) {
                    survivalTime = ((System.currentTimeMillis() - joinedAt) / 1000).toInt()
                }
                saveRecord()
                currentScreen = GameScreen.GAME_OVER
            }
        }

        engine.onRemotePlayerDied = { peerId, score ->
            peerHost?.sendGameOver(peerId, score)
        }
    }

    // MARK: - Solo mode

    fun startSolo(name: String) {
        mode = GameMode.LOCAL
        val n = name.ifEmpty { "Player${Random.nextInt(100, 999)}" }
        engine.join(n)
        joinedAt = System.currentTimeMillis()
        currentScreen = GameScreen.GAME
    }

    // MARK: - Host mode

    fun startHost(name: String) {
        mode = GameMode.P2P_HOST
        val n = name.ifEmpty { "Player${Random.nextInt(100, 999)}" }

        engine.join(n)
        joinedAt = System.currentTimeMillis()

        val host = PeerHost()
        peerHost = host

        host.onPeerJoin = { peerId, peerName ->
            val pid = engine.addRemotePlayer(peerId, peerName)
            host.sendJoined(peerId, pid)
            scope.launch { host.updatePlayerCount() }
        }

        host.onPeerInput = { peerId, left, right, jump ->
            engine.applyRemoteInput(peerId, left, right, jump)
        }

        host.onPeerGiveUp = { peerId ->
            engine.remoteGiveUp(peerId)
        }

        host.onPeerDisconnect = { peerId ->
            engine.removeRemotePlayer(peerId)
            scope.launch { host.updatePlayerCount() }
        }

        currentScreen = GameScreen.CONNECTING
        scope.launch {
            try {
                val roomId = host.createRoom(n)
                roomName = n
                currentScreen = GameScreen.GAME
            } catch (e: Exception) {
                connectionError = e.message
                currentScreen = GameScreen.START
            }
        }
    }

    // MARK: - Client mode

    fun joinRoom(roomId: String, name: String, hostName: String = "") {
        hostTimeoutJob?.cancel()
        hostTimeoutJob = null
        lastStateTime = 0
        peerClient?.disconnect()
        peerClient = null

        mode = GameMode.P2P_CLIENT
        sessionId++
        val currentSession = sessionId
        val n = name.ifEmpty { "Player${Random.nextInt(100, 999)}" }
        clientPlayerName = n
        clientPlayerId = ""
        clientLastSelfId = ""
        roomName = hostName.ifEmpty { roomId }

        engine.stop()

        val client = PeerClient()
        peerClient = client

        client.onStateUpdate = { state ->
            mainHandler.post {
                if (sessionId != currentSession) return@post
                players = state.players
                blocks = state.blocks
                coins = state.coins
                mysteryItems = state.mysteryItems
                roomElapsed = state.roomElapsed
                playerCount = state.players.count { it.alive }
                lastStateTime = System.currentTimeMillis()

                if (joinedAt > 0 && clientPlayerId.isNotEmpty() && currentScreen == GameScreen.GAME) {
                    survivalTime = ((System.currentTimeMillis() - joinedAt) / 1000).toInt()
                }
            }
        }

        client.onJoinedEvent = { playerId ->
            mainHandler.post {
                if (sessionId != currentSession) return@post
                clientPlayerId = playerId
                clientLastSelfId = playerId
                joinedAt = System.currentTimeMillis()
                currentScreen = GameScreen.GAME
                startHostTimeoutMonitor()
            }
        }

        client.onGameOverEvent = { score ->
            mainHandler.post {
                if (sessionId != currentSession) return@post
                finalScore = score
                if (joinedAt > 0) {
                    survivalTime = ((System.currentTimeMillis() - joinedAt) / 1000).toInt()
                }
                saveRecord()
                currentScreen = GameScreen.GAME_OVER
            }
        }

        client.onDisconnectEvent = {
            mainHandler.post {
                if (sessionId != currentSession) return@post
                handleHostDisconnect()
            }
        }

        client.onErrorEvent = { message ->
            mainHandler.post {
                if (sessionId != currentSession) return@post
                connectionError = message
                currentScreen = GameScreen.START
            }
        }

        currentScreen = GameScreen.CONNECTING
        scope.launch {
            try {
                // ホスト生存確認（ゴーストルーム防止）
                val freshRooms = SignalingClient().listRooms()
                val now = System.currentTimeMillis() / 1000.0
                val target = freshRooms.firstOrNull { it.roomId == roomId }
                if (target == null || (now - target.lastHeartbeat) >= 45) {
                    connectionError = L("room_expired")
                    currentScreen = GameScreen.START
                    return@launch
                }

                client.connect(roomId)
                client.sendJoin(n)
            } catch (e: Exception) {
                connectionError = e.message
                currentScreen = GameScreen.START
            }
        }
    }

    // MARK: - Client input

    fun applyInput(left: Boolean, right: Boolean, jump: Boolean) {
        if (mode == GameMode.P2P_CLIENT) {
            peerClient?.sendInput(left, right, jump)
        } else {
            engine.applyInput(left, right, jump)
        }
    }

    // MARK: - Host disconnect detection (client mode)

    private fun startHostTimeoutMonitor() {
        hostTimeoutJob?.cancel()
        lastStateTime = System.currentTimeMillis()
        val monitorSession = sessionId
        hostTimeoutJob = scope.launch {
            while (isActive) {
                delay(1000)
                if (sessionId != monitorSession) break
                if (lastStateTime > 0 && System.currentTimeMillis() - lastStateTime > 5000) {
                    handleHostDisconnect()
                    break
                }
            }
        }
    }

    private fun handleHostDisconnect() {
        if (mode != GameMode.P2P_CLIENT) return
        if (currentScreen != GameScreen.GAME && currentScreen != GameScreen.CONNECTING) return
        sessionId++
        hostTimeoutJob?.cancel()
        hostTimeoutJob = null
        lastStateTime = 0
        peerClient?.disconnect()
        peerClient = null
        currentScreen = GameScreen.HOST_DISCONNECTED
    }

    // MARK: - Lifecycle

    fun giveUp() {
        if (mode == GameMode.P2P_CLIENT) {
            peerClient?.sendGiveUp()
        } else {
            engine.giveUp()
        }
    }

    fun retry() {
        when (mode) {
            GameMode.P2P_HOST -> rejoinAsHost()
            GameMode.P2P_CLIENT -> rejoinAsClient()
            GameMode.LOCAL -> returnToStart()
        }
    }

    private fun rejoinAsHost() {
        val name = engine.playerName
        engine.join(name)
        joinedAt = System.currentTimeMillis()
        currentScreen = GameScreen.GAME
    }

    private fun rejoinAsClient() {
        val client = peerClient
        if (client == null || !client.isConnected) {
            returnToStart()
            return
        }
        clientPlayerId = ""
        client.sendJoin(clientPlayerName)
    }

    fun exitRoom() {
        sessionId++
        hostTimeoutJob?.cancel()
        hostTimeoutJob = null
        peerClient?.disconnect()
        peerClient = null
        clientPlayerId = ""
        clientLastSelfId = ""
        clientPlayerName = ""

        engine.restart()
        setupEngineCallbacks()
        mode = GameMode.LOCAL
        currentScreen = GameScreen.START
    }

    fun leaveRoom() {
        hostTimeoutJob?.cancel()
        hostTimeoutJob = null
        peerHost?.let { host ->
            scope.launch { host.destroy() }
            peerHost = null
        }
        peerClient?.disconnect()
        peerClient = null
        clientPlayerId = ""
        clientLastSelfId = ""
        roomName = ""
        connectionError = null

        engine.restart()
        setupEngineCallbacks()
        mode = GameMode.LOCAL
        currentScreen = GameScreen.START
    }

    private fun returnToStart() {
        sessionId++
        hostTimeoutJob?.cancel()
        hostTimeoutJob = null
        peerClient?.disconnect()
        peerClient = null
        clientPlayerId = ""
        clientLastSelfId = ""

        engine.restart()
        setupEngineCallbacks()
        mode = GameMode.LOCAL
        currentScreen = GameScreen.START
    }

    fun showRoomList() { currentScreen = GameScreen.ROOM_LIST }
    fun showRecords() { currentScreen = GameScreen.RECORDS }
    fun showHowToPlay() { currentScreen = GameScreen.HOW_TO_PLAY }
    fun showSettings() { currentScreen = GameScreen.SETTINGS }
    fun showLanguageSelect() { currentScreen = GameScreen.LANGUAGE_SELECT }

    // MARK: - Record saving

    private fun saveRecord() {
        val name = if (mode == GameMode.P2P_CLIENT) clientPlayerName
        else engine.playerName.ifEmpty { "Player" }

        val record = GameRecord(
            survivalTime = survivalTime,
            score = finalScore,
            mode = GameRecordStore.modeString(mode),
            playerName = name
        )
        GameRecordStore.save(record)
    }

    /** ホストモード時にルームを即座に破棄（runBlockingで確実に完了） */
    fun cleanupHostRoom() {
        val host = peerHost ?: return
        if (host.roomId.isEmpty()) return
        peerHost = null
        backgroundCleanupJob?.cancel()
        backgroundCleanupJob = null
        runBlocking {
            try { host.destroy() } catch (_: Exception) {}
        }
    }

    /** バックグラウンド遷移時に5秒猶予付きでルーム削除をスケジュール */
    fun scheduleBackgroundCleanup() {
        if (peerHost == null || mode != GameMode.P2P_HOST) return
        backgroundCleanupJob = scope.launch {
            delay(5000)
            cleanupHostRoom()
        }
    }

    /** フォアグラウンド復帰時にスケジュールされた削除をキャンセル */
    fun cancelBackgroundCleanup() {
        backgroundCleanupJob?.cancel()
        backgroundCleanupJob = null
    }

    fun destroy() {
        scope.cancel()
        peerHost?.let { host ->
            runBlocking { host.destroy() }
        }
        peerClient?.disconnect()
        engine.stop()
    }
}
