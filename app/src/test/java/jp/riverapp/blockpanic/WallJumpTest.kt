package jp.riverapp.blockpanic

import jp.riverapp.blockpanic.engine.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Wall-jump graze detection tests.
 * Bug: jumping while pressed against a wall caused vy=0 reset from
 * small overlap at wall block bottom edge, limiting jump to 1 block.
 */
class WallJumpTest {

    private fun makePlayer(configure: (ServerPlayer) -> Unit = {}): ServerPlayer {
        val p = createPlayer(
            id = "test", name = "Test", type = PlayerType.USER,
            spawnX = 70.0, spawnY = C.gameHeight - C.playerHeight
        )
        p.onGround = true
        configure(p)
        return p
    }

    @Test
    fun testJumpVelocityPreservedNextToWall() {
        val p = makePlayer()
        val wallBlocks = listOf(
            BlockState(id = "w1", x = 90.0, y = 570.0, width = 30.0, height = 30.0, falling = false, pieceType = 0),
            BlockState(id = "w2", x = 90.0, y = 540.0, width = 30.0, height = 30.0, falling = false, pieceType = 0),
            BlockState(id = "w3", x = 90.0, y = 510.0, width = 30.0, height = 30.0, falling = false, pieceType = 0),
        )

        applyInput(p, InputState(right = true, jump = true))
        updatePlayerPhysics(p)

        val vyAfterJump = p.vy
        assertTrue("Jump velocity should be set", vyAfterJump < -5)

        resolvePlayerBlockCollisions(p, wallBlocks)
        assertTrue("Jump velocity should not be reset by wall collision", p.vy < -5)
    }

    @Test
    fun testSmallVerticalOverlapResolvesHorizontally() {
        val p = makePlayer { it.x = 76.0; it.y = 538.0; it.vy = -10.0; it.onGround = false }
        val block = BlockState(id = "w", x = 90.0, y = 510.0, width = 30.0, height = 30.0, falling = false, pieceType = 0)

        resolvePlayerBlockCollisions(p, listOf(block))
        assertTrue("Small overlap should preserve jump velocity", p.vy < 0)
    }

    @Test
    fun testFullJumpHeightAlongWall() {
        val p = makePlayer()
        val wallBlocks = (0 until 10).map { i ->
            BlockState(
                id = "w$i", x = 90.0, y = (600 - 30 * (i + 1)).toDouble(),
                width = 30.0, height = 30.0, falling = false, pieceType = 0
            )
        }

        applyInput(p, InputState(right = true, jump = true))

        var minY = p.y
        for (frame in 0 until 30) {
            updatePlayerPhysics(p)
            resolvePlayerBlockCollisions(p, wallBlocks)
            minY = minOf(minY, p.y)
            applyInput(p, InputState(right = true))
        }

        val jumpHeight = (C.gameHeight - C.playerHeight) - minY
        assertTrue(
            "Jump height along wall should be at least 60px (actual: ${jumpHeight}px)",
            jumpHeight >= 60
        )
    }

    @Test
    fun testCeilingBlockStopsJump() {
        val p = makePlayer { it.x = 445.0; it.y = 470.0; it.vy = -10.0; it.onGround = false }
        val block = BlockState(id = "ceil", x = 440.0, y = 450.0, width = 30.0, height = 30.0, falling = false, pieceType = 0)

        resolvePlayerBlockCollisions(p, listOf(block))
        assertTrue("Player should be pushed below ceiling block", p.y >= 480)
    }
}
