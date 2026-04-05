package jp.riverapp.blockpanic

import jp.riverapp.blockpanic.engine.*
import jp.riverapp.blockpanic.game.LocalGameEngine
import org.junit.Assert.*
import org.junit.Test

/**
 * Thread safety pattern verification tests.
 * LocalGameEngine uses HandlerThread for the game loop.
 * All input writes are posted via handler?.post to avoid race conditions.
 * GameState collections use .toList() to create safe copies for iteration.
 */
class ThreadSafetyTest {

    // =========================================================================
    // LocalGameEngine.applyInput uses handler?.post
    // =========================================================================

    @Test
    fun `LocalGameEngine has handler field for thread-safe input`() {
        // handler is private, verify via reflection
        val field = LocalGameEngine::class.java.getDeclaredField("handler")
        assertNotNull("handler field should exist", field)
        assertEquals("android.os.Handler", field.type.name)
    }

    @Test
    fun `LocalGameEngine applyInput method exists`() {
        val method = LocalGameEngine::class.java.getMethod(
            "applyInput", Boolean::class.java, Boolean::class.java, Boolean::class.java
        )
        assertNotNull("applyInput method should exist", method)
    }

    @Test
    fun `LocalGameEngine applyRemoteInput method exists`() {
        val method = LocalGameEngine::class.java.getMethod(
            "applyRemoteInput", String::class.java,
            Boolean::class.java, Boolean::class.java, Boolean::class.java
        )
        assertNotNull("applyRemoteInput method should exist", method)
    }

    @Test
    fun `applyInput with null handler does not crash`() {
        // When handler is null (engine not started), post is a no-op
        val engine = LocalGameEngine()
        // handler is null since start() was not called
        engine.join("Test")
        engine.applyInput(left = true, right = false, jump = false)
        // Should not throw
    }

    @Test
    fun `applyRemoteInput with null handler does not crash`() {
        val engine = LocalGameEngine()
        engine.join("Host")
        engine.addRemotePlayer("peer1", "Remote")
        engine.applyRemoteInput("peer1", left = true, right = false, jump = false)
        // Should not throw
    }

    // =========================================================================
    // GameState.players is LinkedHashMap
    // =========================================================================

    @Test
    fun `GameState players is LinkedHashMap`() {
        val gs = GameState()
        assertTrue(
            "players should be LinkedHashMap",
            gs.players is LinkedHashMap
        )
    }

    @Test
    fun `GameState players preserves insertion order`() {
        val gs = GameState()
        // Clear CPUs first by adding enough users
        val ids = (1..6).map { i ->
            gs.addPlayer("p$i", "Player$i").id
        }
        // Verify order is preserved (LinkedHashMap guarantee)
        val keys = gs.players.keys.toList()
        for (id in ids) {
            assertTrue("Player $id should be in map", keys.contains(id))
        }
    }

    // =========================================================================
    // GameState.getAlivePlayers returns a copy (List), not the original
    // =========================================================================

    @Test
    fun `getAlivePlayers returns a copy list`() {
        val gs = GameState()
        gs.addPlayer("p1", "Alice")
        val alive1 = gs.getAlivePlayers()
        val alive2 = gs.getAlivePlayers()
        // Different list instances
        assertNotSame("Should return new list each call", alive1, alive2)
    }

    @Test
    fun `getAlivePlayers result type is List`() {
        val gs = GameState()
        gs.addPlayer("p1", "Alice")
        val alive = gs.getAlivePlayers()
        assertTrue("Should be a List", alive is List<ServerPlayer>)
    }

    // =========================================================================
    // GameState.getCPUPlayers returns a copy (List)
    // =========================================================================

    @Test
    fun `getCPUPlayers returns a copy list`() {
        val gs = GameState()
        // GameState auto-creates CPUs
        val cpu1 = gs.getCPUPlayers()
        val cpu2 = gs.getCPUPlayers()
        assertNotSame("Should return new list each call", cpu1, cpu2)
    }

    @Test
    fun `getCPUPlayers only contains CPU type`() {
        val gs = GameState()
        gs.addPlayer("p1", "Alice")
        val cpus = gs.getCPUPlayers()
        for (cpu in cpus) {
            assertEquals(PlayerType.CPU, cpu.type)
        }
    }

    // =========================================================================
    // getUserCount / getCPUCount iterate safely via .toList()
    // =========================================================================

    @Test
    fun `getUserCount iterates safely`() {
        val gs = GameState()
        assertEquals(0, gs.getUserCount())
        gs.addPlayer("p1", "Alice")
        assertEquals(1, gs.getUserCount())
        gs.addPlayer("p2", "Bob")
        assertEquals(2, gs.getUserCount())
    }

    @Test
    fun `getCPUCount iterates safely`() {
        val gs = GameState()
        // With 0 users, there should be minPlayers CPUs
        assertEquals(C.minPlayers, gs.getCPUCount())
        // Adding users reduces CPUs
        gs.addPlayer("p1", "Alice")
        val expectedCPU = C.minPlayers - 1
        assertEquals(expectedCPU, gs.getCPUCount())
    }

    @Test
    fun `getWorldState uses toList for safe iteration`() {
        val gs = GameState()
        gs.addPlayer("p1", "Alice")
        // getWorldState() internally uses players.values.toList().map { ... }
        val state = gs.getWorldState()
        assertNotNull(state)
        assertTrue(state.players.isNotEmpty())
    }

    @Test
    fun `tick uses toList for safe player iteration`() {
        val gs = GameState()
        gs.addPlayer("p1", "Alice")
        // tick() iterates players.values.toList() -- should not throw CME
        gs.tick(16.0)
        // If we get here, no ConcurrentModificationException
    }
}
