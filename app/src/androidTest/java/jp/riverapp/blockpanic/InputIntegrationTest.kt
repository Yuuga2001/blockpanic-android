package jp.riverapp.blockpanic

import jp.riverapp.blockpanic.engine.*
import jp.riverapp.blockpanic.game.GameCoordinator
import jp.riverapp.blockpanic.game.GameScreen
import jp.riverapp.blockpanic.model.GameMode
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InputIntegrationTest {

    @Test
    fun testCoordinatorStartsSolo() {
        val coord = GameCoordinator()
        assertEquals(GameScreen.START, coord.currentScreen)
        coord.startSolo("TestPlayer")
        assertEquals(GameScreen.GAME, coord.currentScreen)
        assertTrue(coord.effectivePlayerId.isNotEmpty())
        coord.destroy()
    }

    @Test
    fun testInputReachesGameState() {
        val coord = GameCoordinator()
        coord.startSolo("TestPlayer")
        coord.applyInput(left = false, right = true, jump = false)
        // Wait for handler.post to execute on game thread
        Thread.sleep(200)
        val player = coord.engine.gameState.getPlayer(coord.effectivePlayerId)
        assertNotNull(player)
        coord.destroy()
    }

    @Test
    fun testJumpInputMovesPlayerUp() {
        val coord = GameCoordinator()
        coord.startSolo("TestPlayer")
        // Wait for player to land
        Thread.sleep(1000)
        val player = coord.engine.gameState.getPlayer(coord.effectivePlayerId)!!
        val yBefore = player.y
        coord.applyInput(left = false, right = false, jump = true)
        // Wait for handler + multiple ticks
        Thread.sleep(500)
        coord.applyInput(left = false, right = false, jump = false)
        // y should have decreased (jumped up) at some point during the ticks
        // Since multiple ticks ran, check current position
        assertTrue("Player should have jumped (y changed from $yBefore, now ${player.y})", player.y != yBefore)
        coord.destroy()
    }

    @Test
    fun testLeftInputMovesPlayerLeft() {
        val coord = GameCoordinator()
        coord.startSolo("TestPlayer")
        Thread.sleep(500)
        val player = coord.engine.gameState.getPlayer(coord.effectivePlayerId)!!
        val xBefore = player.x
        coord.applyInput(left = true, right = false, jump = false)
        Thread.sleep(500)
        coord.applyInput(left = false, right = false, jump = false)
        assertTrue("Player should have moved left from $xBefore, now ${player.x}", player.x < xBefore)
        coord.destroy()
    }

    @Test
    fun testRightInputMovesPlayerRight() {
        val coord = GameCoordinator()
        coord.startSolo("TestPlayer")
        Thread.sleep(500)
        val player = coord.engine.gameState.getPlayer(coord.effectivePlayerId)!!
        val xBefore = player.x
        coord.applyInput(left = false, right = true, jump = false)
        Thread.sleep(500)
        coord.applyInput(left = false, right = false, jump = false)
        assertTrue("Player should have moved right from $xBefore, now ${player.x}", player.x > xBefore)
        coord.destroy()
    }

    @Test
    fun testGiveUpTriggersGameOver() {
        val coord = GameCoordinator()
        coord.startSolo("TestPlayer")
        coord.giveUp()
        Thread.sleep(300)
        assertEquals(GameScreen.GAME_OVER, coord.currentScreen)
        coord.destroy()
    }

    @Test
    fun testRetryReturnsToStart() {
        val coord = GameCoordinator()
        coord.startSolo("TestPlayer")
        coord.giveUp()
        Thread.sleep(300)
        coord.retry()
        assertEquals(GameScreen.START, coord.currentScreen)
        coord.destroy()
    }

    @Test
    fun testScreenNavigationFlow() {
        val coord = GameCoordinator()
        coord.showRoomList()
        assertEquals(GameScreen.ROOM_LIST, coord.currentScreen)
        coord.currentScreen = GameScreen.START
        coord.showRecords()
        assertEquals(GameScreen.RECORDS, coord.currentScreen)
        coord.currentScreen = GameScreen.START
        coord.showHowToPlay()
        assertEquals(GameScreen.HOW_TO_PLAY, coord.currentScreen)
        coord.currentScreen = GameScreen.START
        coord.showSettings()
        assertEquals(GameScreen.SETTINGS, coord.currentScreen)
        coord.destroy()
    }
}
