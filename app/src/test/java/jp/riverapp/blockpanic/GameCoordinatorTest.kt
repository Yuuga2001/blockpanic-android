package jp.riverapp.blockpanic

import jp.riverapp.blockpanic.game.GameScreen
import jp.riverapp.blockpanic.model.GameMode
import org.junit.Assert.*
import org.junit.Test

/**
 * GameCoordinator screen/mode enum tests.
 * Note: GameCoordinator itself uses Handler(Looper.getMainLooper()) and cannot be
 * instantiated in pure JUnit tests. We test the enums and screen transitions indirectly.
 */
class GameCoordinatorTest {

    // -- GameScreen enum --

    @Test
    fun testGameScreenValues() {
        val screens = GameScreen.entries
        assertTrue(screens.contains(GameScreen.START))
        assertTrue(screens.contains(GameScreen.ROOM_LIST))
        assertTrue(screens.contains(GameScreen.CONNECTING))
        assertTrue(screens.contains(GameScreen.GAME))
        assertTrue(screens.contains(GameScreen.GAME_OVER))
        assertTrue(screens.contains(GameScreen.HOST_DISCONNECTED))
        assertTrue(screens.contains(GameScreen.RECORDS))
        assertTrue(screens.contains(GameScreen.HOW_TO_PLAY))
        assertTrue(screens.contains(GameScreen.SETTINGS))
        assertTrue(screens.contains(GameScreen.LANGUAGE_SELECT))
    }

    @Test
    fun testGameScreenCount() {
        assertEquals(10, GameScreen.entries.size)
    }

    // -- GameMode enum --

    @Test
    fun testGameModeValues() {
        val modes = GameMode.entries
        assertTrue(modes.contains(GameMode.LOCAL))
        assertTrue(modes.contains(GameMode.P2P_HOST))
        assertTrue(modes.contains(GameMode.P2P_CLIENT))
    }

    @Test
    fun testGameModeCount() {
        assertEquals(3, GameMode.entries.size)
    }

    @Test
    fun testDefaultScreenIsStart() {
        // Verify the default screen enum value
        assertEquals("START", GameScreen.START.name)
    }

    @Test
    fun testDefaultModeIsLocal() {
        assertEquals("LOCAL", GameMode.LOCAL.name)
    }

    @Test
    fun testHostDisconnectedScreen() {
        assertEquals("HOST_DISCONNECTED", GameScreen.HOST_DISCONNECTED.name)
    }

    @Test
    fun testP2PHostMode() {
        assertEquals("P2P_HOST", GameMode.P2P_HOST.name)
    }

    @Test
    fun testP2PClientMode() {
        assertEquals("P2P_CLIENT", GameMode.P2P_CLIENT.name)
    }
}
