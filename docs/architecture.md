# Architecture

## System Overview

```
+---------------------------------------------+
|                Android App                   |
|  +----------+  +----------+  +-----------+  |
|  | Jetpack  |  | Canvas   |  |  WebRTC   |  |
|  | Compose  |<>|SurfaceVw |  | P2P Host/ |  |
|  |          |  |          |  |   Client  |  |
|  +----+-----+  +----+-----+  +-----+-----+  |
|       |              |              |         |
|  +----+--------------+--------------+------+  |
|  |         GameCoordinator                 |  |
|  |  (3-mode: local / p2pHost / p2pClient)  |  |
|  +-----------------+----------------------+   |
|                    |                          |
|  +-----------------+----------------------+   |
|  |          LocalGameEngine               |   |
|  | (HandlerThread 60fps + 20Hz broadcast) |   |
|  +-----------------+----------------------+   |
|                    |                          |
|  +-----------------+----------------------+   |
|  |     Engine (1:1 TypeScript port)       |   |
|  |  GameState / Physics / TetrisSystem    |   |
|  +----------------------------------------+   |
+---------------------------------------------+
         |                          |
    REST API                   WebRTC P2P
    (signaling)              (game data)
         |                          |
    +----+----+              +------+------+
    |  Lambda |              |  Web Client |
    |DynamoDB |              |  or other   |
    +---------+              | iOS/Android |
                             +-------------+
```

## Game Modes

### Local (Solo)
- LocalGameEngine runs full physics at 60fps on a dedicated HandlerThread
- CPU bots auto-fill to 5 minimum players
- State broadcast at 20Hz to GameCoordinator via callback
- GameCoordinator updates @Volatile fields for render thread

### P2P Host
- Same as Local, plus:
- PeerHost polls for WebRTC offers every 2s
- Broadcasts StateMessage to all connected peers at 20Hz
- Each peer receives customized playerId in their state

### P2P Client
- LocalGameEngine is STOPPED (no local physics)
- PeerClient connects via WebRTC DataChannel
- Receives StateMessage at 20Hz -> directly updates @Volatile vars
- Sends input messages on change
- Host timeout detection: 5s without state -> HOST_DISCONNECTED

## Data Flow

```
Input -> GameCoordinator.applyInput()
  +- Local/Host: engine.applyInput() -> handler.post { player.input = InputState }
  +- Client: peerClient.sendInput() -> WebRTC -> Host

Engine Tick (60fps, on HandlerThread):
  applyInput -> updatePhysics -> resolveBlockCollisions -> resolvePlayerCollisions
  -> updateCoins -> updateMysteryItems -> updateEffects -> respawnCPU

State Broadcast (20Hz):
  GameState.getWorldState() -> onState callback -> mainHandler.post -> @Volatile vars
  (Host mode: also -> PeerHost.broadcastState() -> WebRTC -> clients)
```

## Rendering Pipeline (Canvas + SurfaceView, 60fps)

GameSurfaceView runs its own render thread:
1. Background fill
2. Block rendering - Tetromino blocks with piece-type colors
3. Item rendering - Coins (gold) + mystery items (rainbow HSL cycle)
4. Player rendering - Animated stick figures (4 poses: idle/run/jump/fall)
5. Crush animations - Death effect
6. Effect popups - +200/BIG/MINI/JUMP floating text
7. HUD overlay - Time/Score/Players/Room (via Compose overlay)

### Stick Figure Poses
- **IDLE**: standing straight, arms at sides
- **RUN**: alternating leg/arm positions (based on inputVx, not vx)
- **JUMP**: arms up, legs tucked (vy < -2 && !onGround)
- **FALL**: arms spread, legs down (vy >= -2 && !onGround)

### Head Scaling
- `headScale = max(sqrt(heightMultiplier), 0.7)` - proportional sqrt scaling
- `headRadius = 6.0 * headScale` - applied to circle draw

## Thread Model

```
Main Thread (UI)
  +- Jetpack Compose UI
  +- GameCoordinator callbacks (via mainHandler.post)

Game Thread (HandlerThread)
  +- LocalGameEngine tick (60fps)
  +- Physics, collision, state updates
  +- Input applied via handler.post

Render Thread (SurfaceView)
  +- Reads @Volatile fields from GameCoordinator
  +- Canvas draw operations
  +- Independent of game thread timing
```

## Localization

- 15 languages via `L("key")` function
- Dictionary-based (no Android resources strings.xml) for consistency with Web/iOS
- LocalizationManager: device language detection -> SharedPreferences persistence
- Fallback chain: selected language -> English

## 効果バッジ HUD

[GameSurfaceView.kt](../app/src/main/java/jp/riverapp/blockpanic/rendering/GameSurfaceView.kt)
の `updateAndDrawEffectBadge` が `drawHUD` から呼ばれ、Room 行の下にバッジを描画。

- 持続系は効果名 + プログレスバー (`effectBadgeTextPaint` / `effectProgressFillPaint`)
- 即時系 (`POINTS`, `COIN`) はクライアント側で 3秒間表示 (`selfEffectDisplay` を保持)
- 自プレイヤーが死亡したら `selfEffectDisplay` を即座にクリア
- 状態は `@Volatile` で game-thread と render-thread の可視性を保証

## エラー処理

[AppError.kt](../app/src/main/java/jp/riverapp/blockpanic/network/AppError.kt)
で 8 カテゴリに分類 (`OFFLINE` / `TIMEOUT` / `ROOM_NOT_FOUND` / `ROOM_EXPIRED` /
`ROOM_FULL` / `HOST_UNAVAILABLE` / `SERVER_ERROR` / `GENERIC`)。

`classifyError` は IOException (UnknownHost / ConnectException) / SocketTimeoutException /
SignalingError / HTTP status から自動分類する。

GameCoordinator の `MutableStateFlow<AppErrorKind?>` が nil でなければ
[ErrorDialog.kt](../app/src/main/java/jp/riverapp/blockpanic/ui/ErrorDialog.kt)
を Compose overlay 表示。

### セッション中通信断の検知 (PeerHost)

Web / iOS と同じ閾値 (companion object 定数化):
- `getSignals` (2秒間隔) 5回連続失敗 = 約10秒で `onNetworkLost`
- `updateRoom` (30秒間隔) 3回連続失敗 = 約90秒で発火

発火後は `leaveRoomWithError(kind)` でルームから退出。

## リモートプレイヤー死亡ガード (Web と同一パターン)

[LocalGameEngine.kt](../app/src/main/java/jp/riverapp/blockpanic/game/LocalGameEngine.kt)
の `remoteDeathFired: MutableSet<String>` により、死亡したリモートプレイヤーの
`onRemotePlayerDied` は 1 度だけ発火する。

- 旧実装ではホストが tick ごとに `sendGameOver` を送信していた
  → クライアント側でバイブレーション無限発火 / 再参加時に棒人間大量生成 というバグ
- 現在は 1 度発火後 1秒で GameState からプレイヤーを削除

## ゲームオーバー画面のボタン (モード別)

| モード | PLAY AGAIN | 副ボタン |
|--------|-----------|---------|
| ソロ (LOCAL) | `rejoinLocal()` — 盤面維持 | 「タイトルへ戻る」 `backToTitle()` |
| ホスト (P2P_HOST) | `rejoinAsHost()` | 「ルームを閉じる」 `leaveRoom()` |
| 参加者 (P2P_CLIENT) | `rejoinAsClient()` | 「退出する」 `exitRoom()` |

## 世界ランキング列幅

[LeaderboardScreen.kt](../app/src/main/java/jp/riverapp/blockpanic/ui/LeaderboardScreen.kt)
の各列は `horizontalScroll` 内で固定幅。名前欄 144dp / Mode 108dp にしているため
Online(Member) モード表記でも改行されない。
