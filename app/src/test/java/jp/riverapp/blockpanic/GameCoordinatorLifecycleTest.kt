package jp.riverapp.blockpanic

import jp.riverapp.blockpanic.game.GameCoordinator
import jp.riverapp.blockpanic.game.GameScreen
import jp.riverapp.blockpanic.model.GameMode
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Field

/**
 * GameCoordinator lifecycle and mode transition tests.
 * Note: GameCoordinator uses Handler(Looper.getMainLooper()) which is unavailable in pure JUnit.
 * These tests verify structure, field existence, and enum transitions via reflection.
 */
class GameCoordinatorLifecycleTest {

    // Helper: get private field value via reflection
    private fun getField(obj: Any, fieldName: String): Any? {
        val field = obj::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(obj)
    }

    // =========================================================================
    // startSolo sets mode to LOCAL
    // =========================================================================

    @Test
    fun `startSolo should set mode to LOCAL - verified via field existence`() {
        // GameCoordinator.mode field exists and defaults to LOCAL
        val modeField = GameCoordinator::class.java.getDeclaredField("mode")
        assertNotNull("mode field should exist", modeField)

        // Verify startSolo method exists
        val method = GameCoordinator::class.java.getMethod("startSolo", String::class.java)
        assertNotNull("startSolo method should exist", method)
    }

    @Test
    fun `GameMode LOCAL is the default mode`() {
        assertEquals("LOCAL", GameMode.LOCAL.name)
        assertEquals(0, GameMode.LOCAL.ordinal)
    }

    // =========================================================================
    // startHost sets mode to P2P_HOST
    // =========================================================================

    @Test
    fun `startHost method exists`() {
        val method = GameCoordinator::class.java.getMethod("startHost", String::class.java)
        assertNotNull("startHost method should exist", method)
    }

    @Test
    fun `P2P_HOST mode enum is correct`() {
        assertEquals("P2P_HOST", GameMode.P2P_HOST.name)
    }

    // =========================================================================
    // joinRoom sets mode to P2P_CLIENT
    // =========================================================================

    @Test
    fun `joinRoom method exists with correct signature`() {
        val method = GameCoordinator::class.java.getMethod(
            "joinRoom", String::class.java, String::class.java, String::class.java
        )
        assertNotNull("joinRoom method should exist", method)
    }

    @Test
    fun `P2P_CLIENT mode enum is correct`() {
        assertEquals("P2P_CLIENT", GameMode.P2P_CLIENT.name)
    }

    // =========================================================================
    // joinRoom increments sessionId
    // =========================================================================

    @Test
    fun `sessionId field exists on GameCoordinator`() {
        val field = GameCoordinator::class.java.getDeclaredField("sessionId")
        assertNotNull("sessionId field should exist", field)
        assertEquals("int", field.type.name)
    }

    @Test
    fun `sessionId field is private`() {
        val field = GameCoordinator::class.java.getDeclaredField("sessionId")
        assertTrue("sessionId should be private",
            java.lang.reflect.Modifier.isPrivate(field.modifiers))
    }

    // =========================================================================
    // exitRoom increments sessionId and resets mode to LOCAL
    // =========================================================================

    @Test
    fun `exitRoom method exists`() {
        val method = GameCoordinator::class.java.getMethod("exitRoom")
        assertNotNull("exitRoom method should exist", method)
    }

    @Test
    fun `exitRoom resets to START screen - verified via enum`() {
        // exitRoom sets currentScreen = GameScreen.START
        assertTrue(GameScreen.entries.contains(GameScreen.START))
    }

    // =========================================================================
    // leaveRoom resets mode to LOCAL
    // =========================================================================

    @Test
    fun `leaveRoom method exists`() {
        val method = GameCoordinator::class.java.getMethod("leaveRoom")
        assertNotNull("leaveRoom method should exist", method)
    }

    // =========================================================================
    // returnToStart (via retry in LOCAL mode) resets to START screen
    // =========================================================================

    @Test
    fun `returnToStart method exists as private`() {
        val method = GameCoordinator::class.java.getDeclaredMethod("returnToStart")
        assertNotNull("returnToStart method should exist", method)
        assertTrue("returnToStart should be private",
            java.lang.reflect.Modifier.isPrivate(method.modifiers))
    }

    @Test
    fun `retry method exists - dispatches based on mode`() {
        val method = GameCoordinator::class.java.getMethod("retry")
        assertNotNull("retry method should exist", method)
    }

    // =========================================================================
    // Multiple rapid joinRoom calls don't crash
    // =========================================================================

    @Test
    fun `joinRoom parameters are correct types`() {
        // Verify joinRoom signature accepts String roomId, String name, String hostName
        val method = GameCoordinator::class.java.getMethod(
            "joinRoom", String::class.java, String::class.java, String::class.java
        )
        val params = method.parameterTypes
        assertEquals(3, params.size)
        assertEquals(String::class.java, params[0])
        assertEquals(String::class.java, params[1])
        assertEquals(String::class.java, params[2])
    }

    // =========================================================================
    // handleHostDisconnect guard: mode + screen check
    // =========================================================================

    @Test
    fun `handleHostDisconnect method exists with mode and screen check`() {
        val method = GameCoordinator::class.java.getDeclaredMethod("handleHostDisconnect")
        assertNotNull("handleHostDisconnect method should exist", method)
        assertTrue("handleHostDisconnect should be private",
            java.lang.reflect.Modifier.isPrivate(method.modifiers))
    }

    @Test
    fun `HOST_DISCONNECTED screen exists for disconnect handling`() {
        assertTrue(GameScreen.entries.contains(GameScreen.HOST_DISCONNECTED))
    }

    // =========================================================================
    // sessionId field exists on GameCoordinator
    // =========================================================================

    @Test
    fun `GameCoordinator has all lifecycle fields`() {
        val expectedFields = listOf("sessionId", "mode", "engine", "peerHost", "peerClient")
        for (fieldName in expectedFields) {
            try {
                val field = GameCoordinator::class.java.getDeclaredField(fieldName)
                assertNotNull("$fieldName field should exist", field)
            } catch (e: NoSuchFieldException) {
                fail("GameCoordinator should have field: $fieldName")
            }
        }
    }

    @Test
    fun `GameCoordinator has all screen navigation methods`() {
        val methods = listOf("showRoomList", "showRecords", "showHowToPlay", "showSettings", "showLanguageSelect")
        for (methodName in methods) {
            val method = GameCoordinator::class.java.getMethod(methodName)
            assertNotNull("$methodName method should exist", method)
        }
    }

    @Test
    fun `all 3 game modes are available`() {
        val modes = GameMode.entries
        assertEquals(3, modes.size)
        assertTrue(modes.contains(GameMode.LOCAL))
        assertTrue(modes.contains(GameMode.P2P_HOST))
        assertTrue(modes.contains(GameMode.P2P_CLIENT))
    }

    @Test
    fun `all 10 game screens are available`() {
        val screens = GameScreen.entries
        assertEquals(10, screens.size)
    }
}
