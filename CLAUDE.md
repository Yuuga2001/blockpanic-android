# Block Panic Android — Claude Code Context

## Overview
- **App**: Block Panic — テトリスブロック落下サバイバルアクションゲーム
- **Platform**: Android 8.0+ (API 26+), Kotlin, Jetpack Compose + Canvas
- **Web版**: https://blockpanic.riverapp.jp/
- **iOS版**: 別リポジトリ `Yuuga2001/blockpanic-ios`
- **Repository**: GitHub `Yuuga2001/blockpanic-android`, branch `main`

## Architecture
- **Engine**: iOS版の Swift を Kotlin に 1:1 ポート (`engine/`)
- **Rendering**: Android Canvas (SurfaceView + game thread 60fps)
- **UI**: Jetpack Compose
- **Networking**: WebRTC P2P (stream-webrtc-android) + OkHttp REST
- **i18n**: 独自辞書方式 15言語
- **Data**: SharedPreferences + Gson

## Key Directories
```
app/src/main/java/jp/riverapp/blockpanic/
├── engine/          # Game logic (1:1 port)
├── game/            # LocalGameEngine, GameCoordinator
├── rendering/       # GameSurfaceView, CameraController
├── network/         # WebRTC + SignalingClient
├── ui/              # Jetpack Compose screens
├── model/           # GameRecord
├── i18n/            # Localization (15 languages)
└── MainActivity.kt
```

## Build & Test
```bash
./gradlew assembleDebug           # Build
./gradlew testDebugUnitTest       # Unit tests (252 tests, 23 files)
./gradlew connectedAndroidTest    # Instrumented tests
```

## Critical Patterns (same as iOS)
- sessionId: GameCoordinator uses incrementing ID to prevent stale callbacks. `leaveRoom()` も sessionId を増分して古い PeerHost の遅延コールバックを弾く
- WebRTC: null listeners before close() to prevent crashes
- Physics: wall-jump graze detection (overlapY < 4px → horizontal resolution)
- Jump input: 50ms delay between true/false sends
- PlayerState: optional fields for cross-platform compatibility
- remoteDeathFired: LocalGameEngine は死亡したリモートプレイヤーの通知を 1 度だけ発火 (重複通知による無限バイブレーションと棒人間大量生成を防止)
- AppError 8 カテゴリ: OFFLINE / TIMEOUT / ROOM_NOT_FOUND / ROOM_EXPIRED / ROOM_FULL / HOST_UNAVAILABLE / SERVER_ERROR / GENERIC. ErrorDialog で統一表示
- PeerHost.onNetworkLost: signaling polling 5回 / heartbeat 3回連続失敗で 1 度発火. leaveRoomWithError(kind) でルーム退出 + ダイアログ
- leaveRoomWithError(kind): leaveRoom + _appError.value 設定の合成メソッド. 順序依存を吸収
- pendingDestroyJob: leaveRoom の destroy を次の createRoom で join、順序保証

## Known Fixes

### Input Race Condition
- **問題**: メインスレッドから直接 player.input を書き換えると、ゲームスレッドの tick 中に競合
- **修正**: `LocalGameEngine.applyInput()` / `applyRemoteInput()` は `handler?.post {}` でゲームスレッドにディスパッチ

### Head Scaling (sqrt scaling)
- **問題**: BIG/MINI エフェクトで頭のサイズが体と不釣り合い
- **修正**: `headScale = max(sqrt(heightMultiplier), 0.7)` で比例スケーリング（最小値0.7）

### DataChannel Observer Timing
- **問題**: DataChannel の observer を mainHandler.post 経由で登録すると、接続直後のメッセージを取りこぼす
- **修正**: observer は DataChannel 作成直後に即座に登録（mainHandler を経由しない）

### Background Game Visibility
- **問題**: Compose の不透明背景が SurfaceView のゲーム画面を隠す
- **修正**: ゲーム画面の Compose レイヤーは `Color.Transparent` を使用

### Stick Figure Pose Y-Axis
- **問題**: iOS (SpriteKit Y-up) と Android (Canvas Y-down) で棒人間の関節座標が上下反転
- **修正**: Y座標オフセットを反転して変換（`playerHeight - offset` パターン）

### ConcurrentModificationException
- **問題**: `players` LinkedHashMap の反復中に追加/削除が発生
- **修正**: 全ての反復箇所で `.toList()` コピーを作成してから処理

## Thread Safety Patterns

### ゲームスレッド分離
- `LocalGameEngine` は `HandlerThread("GameEngineThread")` で専用スレッドを作成
- 全入力は `handler?.post {}` でゲームスレッドに投入
- メインスレッドから直接 `player.input` を変更してはならない

### コレクション安全
- `GameState.players` は `LinkedHashMap` — 反復中の変更は禁止
- `getAlivePlayers()`, `getCPUPlayers()`, `getUserCount()`, `getCPUCount()` は全て `.toList()` でコピー作成
- `tick()` 内部も `players.values.toList()` で安全な反復
- `getWorldState()` も同パターン

### Volatile フィールド
- `GameCoordinator` のレンダリング用フィールド（`players`, `blocks`, `coins`, `mysteryItems`）は `@Volatile`
- レンダースレッドからの読み取りとメインスレッドからの書き込みの可視性を保証

## Cross-Platform Compatibility
- Same STUN, DataChannel, JSON protocol as Web/iOS
- Web↔iOS↔Android 3-way cross-platform play
