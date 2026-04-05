# Block Panic Android

[Block Panic](https://blockpanic.riverapp.jp/) の Android ネイティブアプリ版。Kotlin + Jetpack Compose + Android Canvas で構築。

## Features

- テトリスブロックを避けて生き延びるサバイバルアクション
- 最大10人のリアルタイムP2P対戦（WebRTC）
- Web版・iOS版とのクロスプラットフォーム対戦
- コイン収集 & ミステリーアイテム
- CPU対戦モード
- プレイ記録の自動保存
- 15言語対応
- 棒人間アニメーション

## Requirements

- Android 8.0+ (API 26)
- Android Studio

## Setup

```bash
git clone https://github.com/Yuuga2001/blockpanic-android.git
cd blockpanic-android
./gradlew assembleDebug
```

## Testing

252 unit tests across 23 test files.

```bash
./gradlew testDebugUnitTest           # Unit tests
./gradlew connectedAndroidTest        # Instrumented tests (emulator required)
```

## Known Issues / Fixes

| Issue | Root Cause | Fix |
|-------|-----------|-----|
| Input race condition | Main thread writing player.input during game thread tick | `handler?.post {}` for all input writes |
| Head scaling mismatch | Linear scaling made BIG/MINI heads disproportionate | `max(sqrt(heightMultiplier), 0.7)` |
| DataChannel message loss | Observer registered via mainHandler.post arrived too late | Register observer immediately after DataChannel creation |
| Game invisible behind UI | Opaque Compose background covering SurfaceView | Use `Color.Transparent` for game screen Compose layers |
| Stick figure Y-axis flip | Canvas Y-down vs SpriteKit Y-up offset mismatch | Invert Y offsets with `playerHeight - offset` |
| ConcurrentModificationException | Iterating LinkedHashMap while modifying | `.toList()` copy before all iterations |
| detectDragGestures for press | detectDragGestures doesn't fire on press without drag | Use `awaitEachGesture` pattern instead |

## Supported Languages (15)

English, 日本語, 简体中文, 繁體中文, 한국어, Francais, Deutsch, Espanol, Portugues, Italiano, Русский, ไทย, Tieng Viet, Indonesia, हिन्दी

## Links

- [Web版](https://blockpanic.riverapp.jp/)
- [iOS版](https://github.com/Yuuga2001/blockpanic-ios)
- [App Page](https://riverapp.jp/apps/blockpanic)

## License

All rights reserved. (C) riverapp.jp
