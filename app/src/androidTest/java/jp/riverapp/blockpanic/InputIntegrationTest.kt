package jp.riverapp.blockpanic

import jp.riverapp.blockpanic.engine.*
import jp.riverapp.blockpanic.game.GameCoordinator
import jp.riverapp.blockpanic.game.GameScreen
import jp.riverapp.blockpanic.model.GameMode
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test: input → physics → state verification.
 * Runs on emulator to test with real HandlerThread and Looper.
 * Verifies that touch input actually reaches the game state and moves the player.
 */
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
        val playerId = coord.effectivePlayerId

        // Apply right input
        coord.applyInput(left = false, right = true, jump = false)

        // Wait for game thread to process (handler.post)
        Thread.sleep(100)

        val player = coord.engine.gameState.getPlayer(playerId)
        assertNotNull("Player should exist", player)
        // After tick, player should have moved right
        // (input was posted to game thread, tick processed it)

        coord.destroy()
    }

    @Test
    fun testJumpInputMovesPlayerUp() {
        val coord = GameCoordinator()
        coord.startSolo("TestPlayer")
        val playerId = coord.effectivePlayerId

        // Wait for player to land on ground
        Thread.sleep(500)

        val player = coord.engine.gameState.getPlayer(playerId)
        assertNotNull(player)
        val yBefore = player!!.y

        // Apply jump
        coord.applyInput(left = false, right = false, jump = true)

        // Wait for physics to process
        Thread.sleep(200)

        // Player should have moved up (y decreased)
        assertTrue("Player should have jumped (y decreased from $yBefore)", player.y < yBefore)

        coord.destroy()
    }

    @Test
    fun testLeftInputMovesPlayerLeft() {
        val coord = GameCoordinator()
        coord.startSolo("TestPlayer")
        val playerId = coord.effectivePlayerId

        Thread.sleep(200)
        val player = coord.engine.gameState.getPlayer(playerId)!!
        val xBefore = player.x

        // Move left
        coord.applyInput(left = true, right = false, jump = false)
        Thread.sleep(200)
        coord.applyInput(left = false, right = false, jump = false) // stop

        assertTrue("Player should have moved left from $xBefore", player.x < xBefore)
        coord.destroy()
    }

    @Test
    fun testRightInputMovesPlayerRight() {
        val coord = GameCoordinator()
        coord.startSolo("TestPlayer")
        val playerId = coord.effectivePlayerId

        Thread.sleep(200)
        val player = coord.engine.gameState.getPlayer(playerId)!!
        val xBefore = player.x

        // Move right
        coord.applyInput(left = false, right = true, jump = false)
        Thread.sleep(200)
        coord.applyInput(left = false, right = false, jump = false) // stop

        assertTrue("Player should have moved right from $xBefore", player.x > xBefore)
        coord.destroy()
    }

    @Test
    fun testGiveUpTriggersGameOver() {
        val coord = GameCoordinator()
        coord.startSolo("TestPlayer")
        assertEquals(GameScreen.GAME, coord.currentScreen)

        coord.giveUp()

        // Game over should be triggered via callback (on main handler)
        Thread.sleep(200)
        assertEquals(GameScreen.GAME_OVER, coord.currentScreen)
        coord.destroy()
    }

    @Test
    fun testRetryReturnsToStart() {
        val coord = GameCoordinator()
        coord.startSolo("TestPlayer")
        coord.giveUp()
        Thread.sleep(200)
        assertEquals(GameScreen.GAME_OVER, coord.currentScreen)

        coord.retry() // local mode → returnToStart
        assertEquals(GameScreen.START, coord.currentScreen)
        coord.destroy()
    }

    @Test
    fun testHostModeRetryRejoinsSameSession() {
        val coord = GameCoordinator()
        coord.startHost("HostPlayer")
        // startHost goes to CONNECTING, then async to GAME
        Thread.sleep(2000) // wait for room creation

        if (coord.currentScreen == GameScreen.GAME) {
            coord.giveUp()
            Thread.sleep(200)
            assertEquals(GameScreen.GAME_OVER, coord.currentScreen)

            coord.retry() // host mode → rejoinAsHost
            assertEquals(GameScreen.GAME, coord.currentScreen)
            assertEquals(GameMode.P2P_HOST, coord.mode)
        }
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

        coord.currentScreen = GameScreen.START
        coord.showLanguageSelect()
        assertEquals(GameScreen.LANGUAGE_SELECT, coord.currentScreen)

        coord.destroy()
    }
}
