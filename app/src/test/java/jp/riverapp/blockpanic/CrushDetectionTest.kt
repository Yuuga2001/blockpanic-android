package jp.riverapp.blockpanic

import jp.riverapp.blockpanic.engine.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Block crush detection regression tests.
 * Bug history:
 * - Side contact with landed block caused death
 * - Death triggered before head contact
 * - Death without block overlap
 */
class CrushDetectionTest {

    private fun makePlayer(configure: (ServerPlayer) -> Unit = {}): ServerPlayer {
        val p = createPlayer(
            id = "test", name = "Test", type = PlayerType.USER,
            spawnX = 100.0, spawnY = C.gameHeight - C.playerHeight
        )
        p.onGround = true
        configure(p)
        return p
    }

    // -- Normal crush detection --

    @Test
    fun testCrushedBetweenBlockAndFloor() {
        val p = makePlayer { it.x = 100.0; it.y = C.gameHeight - C.playerHeight }
        val block = BlockState(
            id = "b", x = 95.0, y = C.gameHeight - C.playerHeight - 10, width = 30.0, height = 30.0,
            falling = false, pieceType = 0
        )
        val crushed = resolvePlayerBlockCollisions(p, listOf(block))
        assertTrue("Should be crushed between block and floor", crushed)
    }

    @Test
    fun testCrushedBetweenTwoBlocks() {
        val p = makePlayer { it.x = 100.0; it.y = 400.0 }
        val blocks = listOf(
            BlockState(id = "top", x = 95.0, y = 380.0, width = 30.0, height = 30.0, falling = false, pieceType = 0),
            BlockState(id = "bot", x = 95.0, y = 430.0, width = 30.0, height = 30.0, falling = false, pieceType = 0),
        )
        val crushed = resolvePlayerBlockCollisions(p, blocks)
        assertTrue("Should be crushed between two blocks", crushed)
    }

    // -- Side contact does not kill --

    @Test
    fun testSideContactWithLandedBlockDoesNotKill() {
        val p = makePlayer { it.x = 87.0; it.y = C.gameHeight - C.playerHeight; it.vx = C.moveSpeed }
        val block = BlockState(
            id = "wall", x = 90.0, y = C.gameHeight - 30, width = 30.0, height = 30.0,
            falling = false, pieceType = 0
        )
        val crushed = resolvePlayerBlockCollisions(p, listOf(block))
        assertFalse("Side contact should not kill", crushed)
    }

    @Test
    fun testDeepSideContactDoesNotKill() {
        val p = makePlayer { it.x = 80.0; it.y = C.gameHeight - C.playerHeight }
        val block = BlockState(
            id = "wall", x = 90.0, y = C.gameHeight - 30, width = 30.0, height = 30.0,
            falling = false, pieceType = 0
        )
        val crushed = resolvePlayerBlockCollisions(p, listOf(block))
        assertFalse("Deep side contact should not kill", crushed)
    }

    @Test
    fun testSideContactAfterStopDoesNotKill() {
        val p = makePlayer { it.x = 85.0; it.y = C.gameHeight - C.playerHeight; it.vx = 0.0 }
        val block = BlockState(
            id = "wall", x = 90.0, y = C.gameHeight - 30, width = 30.0, height = 30.0,
            falling = false, pieceType = 0
        )
        val crushed = resolvePlayerBlockCollisions(p, listOf(block))
        assertFalse("Stationary side contact should not kill", crushed)
    }

    // -- Falling blocks --

    @Test
    fun testFallingBlockDoesNotCrush() {
        val p = makePlayer { it.x = 100.0; it.y = C.gameHeight - C.playerHeight }
        val block = BlockState(
            id = "fall", x = 95.0, y = C.gameHeight - C.playerHeight - 5, width = 30.0, height = 30.0,
            falling = true, pieceType = 0
        )
        val crushed = resolvePlayerBlockCollisions(p, listOf(block))
        assertFalse("Falling block should not crush", crushed)
    }

    @Test
    fun testBlockLandingBesideDoesNotKill() {
        val p = makePlayer { it.x = 60.0; it.y = C.gameHeight - C.playerHeight }
        val block = BlockState(
            id = "beside", x = 90.0, y = C.gameHeight - 30, width = 30.0, height = 30.0,
            falling = false, pieceType = 0
        )
        val crushed = resolvePlayerBlockCollisions(p, listOf(block))
        assertFalse("Block landing beside should not kill", crushed)
    }

    // -- Pushed above screen --

    @Test
    fun testPushedAboveScreenIsDead() {
        val p = makePlayer { it.y = -C.playerHeight - 1 }
        val crushed = resolvePlayerBlockCollisions(p, emptyList())
        assertTrue("Should die when pushed above screen", crushed)
    }

    // -- Airborne player should not be crushed --

    @Test
    fun testAirbornePlayerNotCrushed() {
        val p = makePlayer {
            it.x = 100.0
            it.y = 100.0
            it.onGround = false
        }
        val block = BlockState(
            id = "air", x = 95.0, y = 110.0, width = 30.0, height = 30.0,
            falling = false, pieceType = 0
        )
        val crushed = resolvePlayerBlockCollisions(p, listOf(block))
        assertFalse("Airborne player should not be crushed", crushed)
    }

    // -- Jump phase-through prevention --

    @Test
    fun testJumpingUpwardIntoBlockDoesNotPhaseThrough() {
        val p = makePlayer {
            it.x = 100.0
            it.y = 100.0
            it.vy = -10.0
            it.onGround = false
        }
        // Block covers head area (block.y < player.y + pH/2)
        val block = BlockState(
            id = "head", x = 95.0, y = 90.0, width = 30.0, height = 30.0,
            falling = false, pieceType = 0
        )
        resolvePlayerBlockCollisions(p, listOf(block))
        // Player should NOT be placed on top of block (phase-through prevented)
        assertTrue("Player should not phase through block while jumping up", p.y >= 90.0)
    }

    // -- Wall-jump preserves landing on feet-level block --

    @Test
    fun testWallJumpLandsOnFeetLevelBlock() {
        val p = makePlayer {
            it.x = 100.0
            it.y = C.gameHeight - C.playerHeight - 20  // 20px above floor
            it.vy = -10.0
            it.onGround = false
        }
        // Block at feet level (block.y >= player.y + pH/2)
        val block = BlockState(
            id = "foot", x = 95.0, y = C.gameHeight - 30, width = 30.0, height = 30.0,
            falling = false, pieceType = 0
        )
        resolvePlayerBlockCollisions(p, listOf(block))
        // Player should land on top of the block (wall-jump scenario)
        assertTrue("Should land on feet-level block during wall jump",
            p.y <= C.gameHeight - 30 - C.playerHeight + 1)
    }
}
