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

## Build
```bash
./gradlew assembleDebug       # Build
./gradlew test                # Unit tests
./gradlew connectedAndroidTest  # Instrumented tests
```

## Critical Patterns (same as iOS)
- sessionId: GameCoordinator uses incrementing ID to prevent stale callbacks
- WebRTC: null listeners before close() to prevent crashes
- Physics: wall-jump graze detection (overlapY < 4px → horizontal resolution)
- Jump input: 50ms delay between true/false sends
- PlayerState: optional fields for cross-platform compatibility

## Cross-Platform Compatibility
- Same STUN, DataChannel, JSON protocol as Web/iOS
- Web↔iOS↔Android 3-way cross-platform play
