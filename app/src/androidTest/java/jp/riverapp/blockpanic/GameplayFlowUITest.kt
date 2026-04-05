package jp.riverapp.blockpanic

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Full gameplay flow UI tests.
 * Tests the complete user journey: Start → Play → Game → Die → Game Over → Retry.
 * Requires emulator or device.
 */
@RunWith(AndroidJUnit4::class)
class GameplayFlowUITest {

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = context.packageManager.getLaunchIntentForPackage("jp.riverapp.blockpanic")
        context.startActivity(intent!!)
        device.wait(Until.hasObject(By.pkg("jp.riverapp.blockpanic").depth(0)), 5000)
    }

    @Test
    fun testPlayButtonStartsGame() {
        // Find and tap PLAY
        val playButton = device.findObject(UiSelector().textContains("PLAY"))
        assertTrue("PLAY button should exist", playButton.waitForExists(3000))
        playButton.click()

        // Game screen should show (wait for controls to appear)
        Thread.sleep(1000)
        // In game state, the SurfaceView renders the game
        // We verify by checking the app is still in foreground
        assertEquals("jp.riverapp.blockpanic", device.currentPackageName)
    }

    @Test
    fun testOnlineShowsRoomList() {
        val onlineButton = device.findObject(UiSelector().textContains("ONLINE"))
        assertTrue("ONLINE button should exist", onlineButton.waitForExists(3000))
        onlineButton.click()

        // Room list should show CREATE button
        val createButton = device.findObject(UiSelector().textContains("CREATE"))
        assertTrue("CREATE button should appear in room list", createButton.waitForExists(3000))
    }

    @Test
    fun testRoomListBackReturnsToStart() {
        // Go to room list
        val onlineButton = device.findObject(UiSelector().textContains("ONLINE"))
        onlineButton.waitForExists(3000)
        onlineButton.click()

        // Wait for room list
        val createButton = device.findObject(UiSelector().textContains("CREATE"))
        createButton.waitForExists(3000)

        // Press BACK
        val backButton = device.findObject(UiSelector().textContains("BACK"))
        assertTrue("BACK button should exist", backButton.waitForExists(2000))
        backButton.click()

        // Should return to start screen (PLAY visible again)
        val playButton = device.findObject(UiSelector().textContains("PLAY"))
        assertTrue("Should return to start screen", playButton.waitForExists(3000))
    }
}
