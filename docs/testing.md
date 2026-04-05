# Testing Guide

## Overview

252 unit tests across 23 test files.

## Running Tests

```bash
# Unit tests (JUnit, no Android framework required)
./gradlew testDebugUnitTest

# Run specific test class
./gradlew testDebugUnitTest --tests "jp.riverapp.blockpanic.HeadScalingTest"

# Run specific test method
./gradlew testDebugUnitTest --tests "jp.riverapp.blockpanic.PhysicsTest.wall graze*"

# Instrumented tests (emulator or device required)
./gradlew connectedAndroidTest
```

## Test Files

### Physics & Collision
| File | Tests | Coverage |
|------|-------|----------|
| `CollisionTest.kt` | AABB overlap, push direction, player AABB |
| `PhysicsTest.kt` | Input, gravity, floor/ceiling/wall, superjump |
| `WallJumpTest.kt` | Wall-graze vy preservation, full jump height |
| `CrushDetectionTest.kt` | Crush from above, side contact safe, falling blocks |

### Game Logic
| File | Tests | Coverage |
|------|-------|----------|
| `GameStateTest.kt` | Player add/remove, tick, effects, world state |
| `CPUControllerTest.kt` | Min players, rebalance, respawn |
| `TetrisTest.kt` | Piece rotations, dimensions, AI placement |
| `EffectSystemTest.kt` | Effects, toPlayerState, inputVx |
| `CoinMysterySystemTest.kt` | Spawn, state types, collection |

### Player & Engine
| File | Tests | Coverage |
|------|-------|----------|
| `PlayerTest.kt` | Create user/CPU, initial state, toPlayerState |
| `LocalGameEngineTest.kt` | Join, remote players, jump consume, input |
| `HeadScalingTest.kt` | sqrt scaling, min clamp 0.7, headRadius |
| `ThreadSafetyTest.kt` | handler.post pattern, LinkedHashMap, toList() copies |

### Multiplayer & Network
| File | Tests | Coverage |
|------|-------|----------|
| `GameCoordinatorTest.kt` | GameScreen/GameMode enums, screen count |
| `GameCoordinatorLifecycleTest.kt` | Mode transitions, sessionId, lifecycle methods |
| `SignalingClientTest.kt` | RoomInfo/SignalInfo decode, error types |
| `NetworkProtocolTest.kt` | StateMessage serialization, PeerError, DataChannel config |
| `PlayerStateDecodingTest.kt` | Cross-platform JSON decode, round-trip |

### Features
| File | Tests | Coverage |
|------|-------|----------|
| `LocalizationTest.kt` | 15-language keys, fallback, auto detection |
| `GameRecordTest.kt` | Save/load, Codable, mode strings |
| `GameRecordPersistenceTest.kt` | formattedDate, array serialization, sorting |
| `InputAndRenderingTest.kt` | Input physics, pose logic, Y-axis, effects |
| `LessonsLearnedTest.kt` | Regression tests for all known bugs |

## Testing Strategy

### Unit Tests (JUnit)
- **Framework**: JUnit 4 + Kotlin
- **No Android dependency**: Pure JVM tests, no emulator needed
- **Coverage**: Engine logic, physics, serialization, data models
- **Limitation**: Cannot test Handler/Looper, SharedPreferences, Canvas

### Reflection-based Tests
- GameCoordinator uses `Handler(Looper.getMainLooper())` -- cannot instantiate in JUnit
- Tests verify field/method existence via reflection
- Mode transitions verified via enum values and method signatures

### Instrumented Tests
- Requires Android emulator or physical device
- `./gradlew connectedAndroidTest`
- Tests: SharedPreferences persistence, Compose UI interactions

### Screenshot Tests
- Not yet implemented
- Planned: Paparazzi for Compose snapshot tests
- Would cover: stick figure rendering, effect popups, HUD layout

## Writing Tests

### Physics Tests
```kotlin
private fun makePlayer(configure: (ServerPlayer) -> Unit = {}): ServerPlayer {
    val p = createPlayer("test", "Test", PlayerType.USER, 450.0, C.gameHeight - C.playerHeight)
    p.onGround = true
    configure(p)
    return p
}

@Test
fun `example physics test`() {
    val p = makePlayer()
    applyInput(p, InputState(jump = true))
    assertEquals(C.jumpPower, p.vy, 0.001)
}
```

### Serialization Tests
```kotlin
@Test
fun `cross-platform decode test`() {
    val json = """{"id":"p","name":"P","x":0,"y":0,"vx":0,"vy":0,
         "type":"user","color":"blue","alive":true,"onGround":true,
         "score":0,"heightMultiplier":1,"jumpMultiplier":1}"""
    val state = Gson().fromJson(json, PlayerState::class.java)
    assertEquals(0.0, state.inputVx, 0.001) // default when missing
}
```

### Reflection Tests (for classes requiring Android context)
```kotlin
@Test
fun `verify method exists via reflection`() {
    val method = GameCoordinator::class.java.getDeclaredMethod("handleHostDisconnect")
    assertNotNull(method)
    assertTrue(java.lang.reflect.Modifier.isPrivate(method.modifiers))
}
```
