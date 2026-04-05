package jp.riverapp.blockpanic

import jp.riverapp.blockpanic.engine.*
import jp.riverapp.blockpanic.game.LocalGameEngine
import org.junit.Assert.*
import org.junit.Test

/**
 * LocalGameEngine tests.
 * Note: start()/stop()/restart() use HandlerThread which is unavailable in pure JUnit.
 * Tests that need the game loop use GameState.tick() directly instead.
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
    fun testApplyRemoteInput() {
        val engine = makeEngine()
        engine.join(name = "Host")
        val remoteId = engine.addRemotePlayer(peerId = "peer1", name = "Remote")
        engine.applyRemoteInput(peerId = "peer1", left = true, right = false, jump = false)
        val player = engine.gameState.getPlayer(remoteId)
        assertNotNull(player)
        assertTrue(player!!.input.left)
    }

    @Test
    fun testApplyRemoteInputForUnknownPeerDoesNotCrash() {
        val engine = makeEngine()
        engine.join(name = "Host")
        // Should not crash
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
        engine.applyInput(left = false, right = false, jump = true)

        val player = engine.gameState.getPlayer(id)!!
        assertTrue("applyInput should set jump=true", player.input.jump)

        // Use GameState.tick directly (no HandlerThread needed)
        engine.gameState.tick(dtMs = 16.0)
        assertFalse("tick should consume jump input", player.input.jump)
    }

    // -- Apply input --

    @Test
    fun testApplyInputSetsPlayerInput() {
        val engine = makeEngine()
        val id = engine.join(name = "Test")
        engine.applyInput(left = true, right = false, jump = false)
        val player = engine.gameState.getPlayer(id)!!
        assertTrue(player.input.left)
        assertFalse(player.input.right)
        assertFalse(player.input.jump)
    }

    @Test
    fun testApplyInputRightAndJump() {
        val engine = makeEngine()
        val id = engine.join(name = "Test")
        engine.applyInput(left = false, right = true, jump = true)
        val player = engine.gameState.getPlayer(id)!!
        assertFalse(player.input.left)
        assertTrue(player.input.right)
        assertTrue(player.input.jump)
    }
}
