# Lessons Learned

## WebRTC P2P 接続

### デリゲート管理
- **問題**: `PeerClient.disconnect()` でコールバック/デリゲートをnilにせずclose() → 内部コールバックが不正発火 → 以降の接続が全て失敗
- **教訓**: `pc.delegate = null`, `channel.delegate = null` を close() の**前に**実行。コールバック参照も全てnullにする
- **該当**: `PeerClient.disconnect()`, `PeerHost.handlePeerDisconnect()`, `PeerHost.destroy()`

### セッションID管理
- **問題**: ホスト切断後の再join時、古いPeerClientのICEコールバック（disconnected→failed→closed の3連）が遅延発火し、新しいセッションを破壊
- **教訓**: `sessionId` カウンターを全遷移ポイント（joinRoom/exitRoom/handleHostDisconnect/returnToStart）でインクリメント。全コールバックでsessionId照合
- **該当**: `GameCoordinator` の全PeerClientコールバック

### handleHostDisconnect ガード
- **問題**: mode == P2P_CLIENT だけでは不十分。hostDisconnected画面表示後にも遅延発火する
- **教訓**: `currentScreen == GAME || CONNECTING` もガード条件に含める
- **該当**: `GameCoordinator.handleHostDisconnect()`

### DataChannel Observer タイミング
- **問題**: DataChannel の observer を mainHandler.post 経由で登録すると、接続直後のメッセージを取りこぼす
- **教訓**: observer は DataChannel 作成直後に即座に登録する。mainHandler を経由しない
- **該当**: `PeerClient.connect()`, `PeerHost` DataChannel 生成箇所

## 物理演算

### 壁際ジャンプ
- **問題**: 壁ブロック下端の微小重なり（overlapY < 4px, overlapX >> overlapY）で vertical resolution が発動し、vy=0 にリセット → 1ブロック分しかジャンプできない
- **教訓**: pushY == +1（頭上ブロック）かつ overlapY < 4px かつ overlapX > overlapY*2 の場合、水平解決に切り替え
- **該当**: `Physics.kt` resolvePlayerBlockCollisions

### Crush判定の偽陽性
- **問題**: 着地ブロックに横から触れると死亡 / 頭に触れる前に死亡
- **教訓**: Web版と1:1一致のシンプルなロジック（overlapX>2 && overlapY>2, 着地ブロックのみ）が最も安定。独自の先行crush判定は偽陽性を生む
- **該当**: `Physics.kt`

### Head Scaling (sqrt スケーリング)
- **問題**: BIG/MINI エフェクトで頭のサイズが体と不釣り合い（線形スケーリングだと大きすぎる/小さすぎる）
- **教訓**: `headScale = max(sqrt(heightMultiplier), 0.7)` で平方根による比例スケーリング。最小値0.7で極端な縮小を防止
- **該当**: `GameSurfaceView.kt` 棒人間描画

## スレッド安全性

### Input Race Condition
- **問題**: メインスレッド（UI）からゲームスレッドの player.input を直接変更 → tick 中の読み取りと競合
- **教訓**: 全ての入力は `handler?.post {}` でゲームスレッドに投入する。直接書き込み禁止
- **該当**: `LocalGameEngine.applyInput()`, `applyRemoteInput()`

### ConcurrentModificationException
- **問題**: `players` LinkedHashMap の反復中に addPlayer/removePlayer が呼ばれる
- **教訓**: 全ての反復箇所で `.toList()` コピーを作成してから処理する
- **該当**: `GameState.tick()`, `getAlivePlayers()`, `getCPUPlayers()`, `getUserCount()`, `getCPUCount()`, `getWorldState()`

## クロスプラットフォーム

### PlayerState デコード
- **問題**: iOS/Android独自フィールド（inputVx, activeEffect, effectEndTime）が Web版JSONに存在せず、デコード失敗 → state受信不能 → host disconnected
- **教訓**: Gson のデフォルト値機能を使い、存在しないフィールドはデフォルト値でデコード。Kotlin の `= 0.0` / `= null` で対応
- **該当**: `Types.kt` PlayerState

### Stick Figure Y-Axis
- **問題**: iOS (SpriteKit Y-up, 原点左下) と Android (Canvas Y-down, 原点左上) で棒人間の関節座標が上下反転
- **教訓**: Y座標オフセットを `playerHeight - offset` パターンで変換する
- **該当**: `GameSurfaceView.kt` 棒人間描画

## UI/UX (Android 固有)

### detectDragGestures の制約
- **問題**: Compose の `detectDragGestures` はプレス（押下のみ）で発火しない。ジャンプボタンのタップに使えない
- **教訓**: `awaitEachGesture` + `awaitFirstDown()` パターンを使用して即座にプレスを検出
- **該当**: ゲームコントロールUI

### Compose 背景の不透明性
- **問題**: Jetpack Compose のデフォルト背景が不透明で、背後の SurfaceView（ゲーム画面）が見えなくなる
- **教訓**: ゲーム画面オーバーレイの Compose レイヤーは `Color.Transparent` を明示的に指定
- **該当**: ゲーム画面 Composable

### ジャンプ入力
- **問題**: jump=true/false を即座に送るとtrue が届く前にfalseで上書き
- **教訓**: `jumpPressed` フラグで1回だけ発火。jump=false送信は50ms遅延
- **該当**: ゲームコントロールUI

### ルーム自動破棄
- **問題**: アプリがバックグラウンドに入るだけでルームが破棄される
- **教訓**: ハートビート（30秒間隔）+ DynamoDB TTL（2分）で不在ルームを自動削除。アプリライフサイクルイベントでの即時削除は避ける
- **該当**: `GameCoordinator`, `PeerHost`, signaling Lambda

## 2026-04-15: ブロックすり抜けバグ

- **問題**: ブロックに潰される直前にジャンプするとブロックをすり抜けてその上に乗れる
- **原因**: 衝突解決の `pushY == -1`（着地判定）がプレイヤーの速度方向を考慮せず、上昇中でも「上から着地」として処理していた
- **修正**: `pushY == -1 && player.vy >= 0` に変更（上昇中は頭衝突として処理）。潰し判定も `onGround` + 残存オーバーラップ方式に改善
- **教訓**: 衝突解決では位置だけでなく速度方向も考慮する。3プラットフォーム共通のPhysicsレベルの問題
- **該当**: `Physics.swift`, `Physics.kt`, `Physics.ts`
