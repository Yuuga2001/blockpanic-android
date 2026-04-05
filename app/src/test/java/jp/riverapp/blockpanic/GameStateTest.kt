package jp.riverapp.blockpanic

import jp.riverapp.blockpanic.engine.*
import org.junit.Assert.*
import org.junit.Test

class GameStateTest {

    // -- Player management --

    @Test
    fun testAddPlayer() {
        val gs = GameState()
        val p = gs.addPlayer(id = "p1", name = "Alice")
        assertEquals(PlayerType.USER, p.type)
        assertTrue(p.alive)
        assertNotNull(gs.getPlayer("p1"))
    }

    @Test
    fun testRemovePlayer() {
        val gs = GameState()
        gs.addPlayer(id = "p1", name = "Alice")
        gs.removePlayer("p1")
        assertNull(gs.getPlayer("p1"))
    }

    @Test
    fun testCPURebalance() {
        val gs = GameState()
        val cpuBefore = gs.getCPUCount()
        assertTrue(cpuBefore >= 1)

        gs.addPlayer(id = "p1", name = "Alice")
        assertEquals(1, gs.getUserCount())
    }

    // -- Tick / Physics --

    @Test
    fun testTickAppliesGravity() {
        val gs = GameState()
        val p = gs.addPlayer(id = "p1", name = "Alice")
        val y0 = p.y
        for (i in 0 until 10) { gs.tick(dtMs = 16.0) }
        assertTrue(p.y > y0)
    }

    @Test
    fun testPlayerCanJump() {
        val gs = GameState()
        val p = gs.addPlayer(id = "p1", name = "Alice")
        p.y = C.gameHeight - C.playerHeight
        p.vy = 0.0
        p.onGround = true
        p.input = InputState(jump = true)
        gs.tick(dtMs = 16.0)
        assertTrue(p.vy < 0)
    }

    @Test
    fun testPlayerCanMoveRight() {
        val gs = GameState()
        val p = gs.addPlayer(id = "p1", name = "Alice")
        p.x = 450.0
        p.y = 0.0
        val x0 = p.x
        p.input = InputState(right = true)
        gs.tick(dtMs = 16.0)
        assertTrue(p.x > x0)
    }

    // -- Effects --

    @Test
    fun testEffectExpiry() {
        val gs = GameState()
        val p = gs.addPlayer(id = "p1", name = "Alice")
        p.heightMultiplier = 0.5
        p.activeEffect = MysteryEffect.SHRINK
        p.effectEndTime = System.currentTimeMillis().toDouble() - 1000 // already expired
        gs.tick(dtMs = 16.0)
        assertEquals(1.0, p.heightMultiplier, 0.001)
        assertNull(p.activeEffect)
        assertEquals(0.0, p.effectEndTime, 0.001)
    }

    // -- World state --

    @Test
    fun testGetWorldState() {
        val gs = GameState()
        gs.addPlayer(id = "p1", name = "Alice")
        val state = gs.getWorldState()
        assertFalse(state.players.isEmpty())
        assertTrue(state.players.any { it.id == "p1" })
    }

    @Test
    fun testWorldStateIncludesHeightMultiplier() {
        val gs = GameState()
        val p = gs.addPlayer(id = "p1", name = "Alice")
        p.heightMultiplier = 2.0
        val state = gs.getWorldState()
        val alice = state.players.find { it.id == "p1" }
        assertEquals(2.0, alice?.heightMultiplier ?: 0.0, 0.001)
    }
}
