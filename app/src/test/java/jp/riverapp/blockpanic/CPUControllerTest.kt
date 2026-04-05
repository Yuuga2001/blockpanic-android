package jp.riverapp.blockpanic

import jp.riverapp.blockpanic.engine.*
import org.junit.Assert.*
import org.junit.Test

class CPUControllerTest {

    @Test
    fun testMinimumPlayersEnforced() {
        val gs = GameState()
        val alivePlayers = gs.getAlivePlayers()
        assertTrue(
            "Should have at least ${C.minPlayers} players",
            alivePlayers.size >= C.minPlayers
        )
    }

    @Test
    fun testCPUCountReducesWhenUserJoins() {
        val gs = GameState()
        val cpuBefore = gs.getCPUCount()
        gs.addPlayer(id = "user1", name = "User")
        val cpuAfter = gs.getCPUCount()
        assertTrue("CPU count should decrease when user joins", cpuAfter < cpuBefore)
    }

    @Test
    fun testCPURespawnsAfterDeath() {
        val gs = GameState()
        val cpuPlayers = gs.getCPUPlayers()
        assertTrue("Should have CPUs", cpuPlayers.isNotEmpty())

        val cpu = cpuPlayers.first()
        cpu.alive = false
        cpu.diedAt = System.currentTimeMillis().toDouble() - 2000 // died 2 seconds ago

        gs.tick(dtMs = 16.0)
        assertTrue("CPU should respawn after delay", cpu.alive)
    }

    @Test
    fun testUserCountTracking() {
        val gs = GameState()
        assertEquals(0, gs.getUserCount())
        gs.addPlayer(id = "u1", name = "User1")
        assertEquals(1, gs.getUserCount())
        gs.addPlayer(id = "u2", name = "User2")
        assertEquals(2, gs.getUserCount())
        gs.removePlayer("u1")
        assertEquals(1, gs.getUserCount())
    }
}
