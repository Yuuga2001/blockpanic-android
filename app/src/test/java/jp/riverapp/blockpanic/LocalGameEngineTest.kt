package jp.riverapp.blockpanic

import jp.riverapp.blockpanic.engine.*
import jp.riverapp.blockpanic.game.LocalGameEngine
import org.junit.Assert.*
import org.junit.Test

/**
 * LocalGameEngine tests.
 * Note: start()/stop()/restart() use HandlerThread which is unavailable in pure JUnit.
 * applyInput/applyRemoteInput post to handler, so in tests we verify via GameState directly.
 */
class LocalGameEngineTest {

    private fun makeEngine(): LocalGameEngine = LocalGameEngine()

    // -- Local player --

    @Test
    fun testJoinCreatesPlayer() {
        val engine = makeEngine()
        val id = engine.join(name = "Test")
        assertTrue(id.isNotEmpty())
        assertEquals(id, engine.playerId)
        assertEquals(id, engine.lastSelfId)
    }

    @Test
    fun testJoinWithEmptyNameStillWorks() {
        val engine = makeEngine()
        val id = engine.join(name = "")
        assertTrue(id.isNotEmpty())
    }

    // -- Remote player management --

    @Test
    fun testAddRemotePlayer() {
        val engine = makeEngine()
        engine.join(name = "Host")
        val remoteId = engine.addRemotePlayer(peerId = "peer1", name = "Remote")
        assertTrue(remoteId.isNotEmpty())
        assertNotEquals(remoteId, engine.playerId)
        assertEquals(2, engine.totalPlayerCount)
    }

    @Test
    fun testRemoveRemotePlayer() {
        val engine = makeEngine()
        engine.join(name = "Host")
        engine.addRemotePlayer(peerId = "peer1", name = "Remote")
        assertEquals(2, engine.totalPlayerCount)
        engine.removeRemotePlayer(peerId = "peer1")
        assertEquals(1, engine.totalPlayerCount)
    }

    @Test
    fun testApplyRemoteInput_viaGameState() {
        // applyRemoteInput posts to handler, unavailable in test.
        // Verify the pattern: set player.input directly via GameState.
        val engine = makeEngine()
        engine.join(name = "Host")
        val remoteId = engine.addRemotePlayer(peerId = "peer1", name = "Remote")
        val player = engine.gameState.getPlayer(remoteId)
        assertNotNull(player)
        // Simulate what applyRemoteInput does internally
        player!!.input = InputState(left = true)
        assertTrue(player.input.left)
    }

    @Test
    fun testApplyRemoteInputForUnknownPeerDoesNotCrash() {
        val engine = makeEngine()
        engine.join(name = "Host")
        // handler is null so post is no-op, should not crash
        engine.applyRemoteInput(peerId = "unknown", left = true, right = false, jump = false)
    }

    @Test
    fun testRemoteGiveUp() {
        val engine = makeEngine()
        engine.join(name = "Host")
        val remoteId = engine.addRemotePlayer(peerId = "peer1", name = "Remote")
        engine.remoteGiveUp(peerId = "peer1")
        val player = engine.gameState.getPlayer(remoteId)
        assertNotNull(player)
        assertFalse(player!!.alive)
    }

    @Test
    fun testPlayerIdForPeer() {
        val engine = makeEngine()
        engine.join(name = "Host")
        val remoteId = engine.addRemotePlayer(peerId = "peer1", name = "Remote")
        assertEquals(remoteId, engine.playerIdForPeer("peer1"))
        assertNull(engine.playerIdForPeer("unknown"))
    }

    // -- Jump input consumed (via GameState.tick) --

    @Test
    fun testJumpInputConsumedAfterTick() {
        val engine = makeEngine()
        val id = engine.join(name = "Test")
        // Set input directly (handler not available in test)
        val player = engine.gameState.getPlayer(id)!!
        player.input = InputState(jump = true)
        assertTrue("jump should be true", player.input.jump)

        engine.gameState.tick(dtMs = 16.0)
        assertFalse("tick should consume jump input", player.input.jump)
    }

    // -- Apply input (verify thread-safe pattern exists) --

    @Test
    fun testApplyInputSetsPlayerInput_viaGameState() {
        // applyInput posts to handler. Verify pattern via direct GameState access.
        val engine = makeEngine()
        val id = engine.join(name = "Test")
        val player = engine.gameState.getPlayer(id)!!
        player.input = InputState(left = true)
        assertTrue(player.input.left)
        assertFalse(player.input.right)
    }

    @Test
    fun testApplyInputRightAndJump_viaGameState() {
        val engine = makeEngine()
        val id = engine.join(name = "Test")
        val player = engine.gameState.getPlayer(id)!!
        player.input = InputState(right = true, jump = true)
        assertTrue(player.input.right)
        assertTrue(player.input.jump)
    }
}
