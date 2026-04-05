package jp.riverapp.blockpanic

import jp.riverapp.blockpanic.engine.*
import org.junit.Assert.*
import org.junit.Test

class PhysicsTest {

    private fun makePlayer(configure: (ServerPlayer) -> Unit = {}): ServerPlayer {
        val p = createPlayer(id = "test", name = "Test", type = PlayerType.USER, spawnX = 450.0, spawnY = 500.0)
        configure(p)
        return p
    }

    // -- applyInput --

    @Test
    fun testMoveLeft() {
        val p = makePlayer()
        applyInput(p, InputState(left = true))
        assertEquals(-C.moveSpeed, p.vx, 0.001)
    }

    @Test
    fun testMoveRight() {
        val p = makePlayer()
        applyInput(p, InputState(right = true))
        assertEquals(C.moveSpeed, p.vx, 0.001)
    }

    @Test
    fun testStopWhenNoInput() {
        val p = makePlayer { it.vx = 10.0 }
        applyInput(p, InputState())
        assertEquals(0.0, p.vx, 0.001)
    }

    @Test
    fun testJumpOnGround() {
        val p = makePlayer { it.onGround = true }
        applyInput(p, InputState(jump = true))
        assertEquals(C.jumpPower, p.vy, 0.001)
        assertFalse(p.onGround)
    }

    @Test
    fun testNoJumpInAir() {
        val p = makePlayer { it.onGround = false; it.vy = -5.0 }
        applyInput(p, InputState(jump = true))
        assertEquals(-5.0, p.vy, 0.001)
    }

    // -- updatePlayerPhysics --

    @Test
    fun testGravityApplied() {
        val p = makePlayer { it.vy = 0.0; it.y = 100.0 }
        updatePlayerPhysics(p)
        assertEquals(C.gravity, p.vy, 0.001)
    }

    @Test
    fun testMaxFallSpeedCap() {
        val p = makePlayer { it.vy = C.maxFallSpeed + 10; it.y = 100.0 }
        updatePlayerPhysics(p)
        assertEquals(C.maxFallSpeed, p.vy, 0.001)
    }

    @Test
    fun testFloorLanding() {
        val p = makePlayer { it.y = C.gameHeight; it.vy = 5.0 }
        updatePlayerPhysics(p)
        assertEquals(C.gameHeight - C.playerHeight, p.y, 0.001)
        assertTrue(p.onGround)
        assertEquals(0.0, p.vy, 0.001)
    }

    @Test
    fun testCeilingClamp() {
        val p = makePlayer { it.y = -10.0; it.vy = -20.0 }
        updatePlayerPhysics(p)
        assertEquals(0.0, p.y, 0.001)
        assertEquals(0.0, p.vy, 0.001)
    }

    @Test
    fun testLeftWallClamp() {
        val p = makePlayer { it.x = -5.0; it.vx = -10.0 }
        updatePlayerPhysics(p)
        assertEquals(0.0, p.x, 0.001)
        assertEquals(0.0, p.vx, 0.001)
    }

    @Test
    fun testRightWallClamp() {
        val p = makePlayer { it.x = C.gameWidth; it.vx = 10.0 }
        updatePlayerPhysics(p)
        assertEquals(C.gameWidth - C.playerWidth, p.x, 0.001)
        assertEquals(0.0, p.vx, 0.001)
    }

    @Test
    fun testSuperjumpReducesGravity() {
        val normal = makePlayer { it.vy = -10.0; it.y = 300.0 }
        updatePlayerPhysics(normal)

        val boosted = makePlayer { it.vy = -10.0; it.y = 300.0; it.jumpMultiplier = 2.0 }
        updatePlayerPhysics(boosted)

        // Boosted decelerates slower (vy stays more negative)
        assertTrue(boosted.vy < normal.vy)
    }

    @Test
    fun testHeightMultiplierFloor() {
        val p = makePlayer { it.heightMultiplier = 2.0; it.y = C.gameHeight; it.vy = 5.0 }
        updatePlayerPhysics(p)
        assertEquals(C.gameHeight - C.playerHeight * 2, p.y, 0.001)
        assertTrue(p.onGround)
    }

    // -- resolvePlayerBlockCollisions --

    @Test
    fun testNoBlocksNoCrush() {
        val p = makePlayer()
        assertFalse(resolvePlayerBlockCollisions(p, emptyList()))
    }

    @Test
    fun testFallingBlocksIgnoredForCrush() {
        val p = makePlayer { it.x = 100.0; it.y = 100.0 }
        val blocks = listOf(
            BlockState(id = "b1", x = 95.0, y = 90.0, width = 30.0, height = 30.0, falling = true, pieceType = 0),
            BlockState(id = "b2", x = 95.0, y = 130.0, width = 30.0, height = 30.0, falling = true, pieceType = 0),
        )
        assertFalse(resolvePlayerBlockCollisions(p, blocks))
    }
}
