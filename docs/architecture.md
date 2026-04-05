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
