package jp.riverapp.blockpanic

import jp.riverapp.blockpanic.engine.*
import org.junit.Assert.*
import org.junit.Test

class PlayerTest {

    @Test
    fun testCreateUserPlayer() {
        val p = createPlayer(id = "p1", name = "Alice", type = PlayerType.USER, spawnX = 100.0, spawnY = 200.0)
        assertEquals("p1", p.id)
        assertEquals("Alice", p.name)
        assertEquals(PlayerType.USER, p.type)
        assertEquals(PlayerColor.BLUE, p.color)
        assertTrue(p.alive)
        assertEquals(1.0, p.heightMultiplier, 0.001)
        assertEquals(1.0, p.jumpMultiplier, 0.001)
        assertNull(p.activeEffect)
    }

    @Test
    fun testCreateCPUPlayer() {
        val p = createPlayer(id = "cpu1", name = "Bot", type = PlayerType.CPU, spawnX = 0.0, spawnY = 0.0)
        assertEquals(PlayerColor.BLACK, p.color)
        assertEquals(PlayerType.CPU, p.type)
    }

    @Test
    fun testInitialInputState() {
        val p = createPlayer(id = "p1", name = "T", type = PlayerType.USER, spawnX = 0.0, spawnY = 0.0)
        assertFalse(p.input.left)
        assertFalse(p.input.right)
        assertFalse(p.input.jump)
    }

    @Test
    fun testToPlayerState() {
        val p = createPlayer(id = "p1", name = "Alice", type = PlayerType.USER, spawnX = 100.0, spawnY = 200.0)
        p.score = 42
        p.heightMultiplier = 2.0
        val state = toPlayerState(p)
        assertEquals("p1", state.id)
        assertEquals(42, state.score)
        assertEquals(2.0, state.heightMultiplier, 0.001)
        assertEquals(100.0, state.x, 0.001)
    }
}
