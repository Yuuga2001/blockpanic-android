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
 * Start screen UI tests using UiAutomator.
 * Tests button existence, screen transitions, and basic interaction.
 * Requires emulator or device.
 */
@RunWith(AndroidJUnit4::class)
class StartScreenUITest {

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        // Launch app
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = context.packageManager.getLaunchIntentForPackage("jp.riverapp.blockpanic")
        assertNotNull("Launch intent should exist", intent)
        context.startActivity(intent)
        // Wait for app to load
        device.wait(Until.hasObject(By.pkg("jp.riverapp.blockpanic").depth(0)), 5000)
    }

    @Test
    fun testAppLaunches() {
        // App should launch without crashing
        val pkg = device.currentPackageName
        assertEquals("jp.riverapp.blockpanic", pkg)
    }

    @Test
    fun testPlayButtonExists() {
        val playButton = device.findObject(UiSelector().textContains("PLAY").className("android.widget.TextView"))
        assertTrue("PLAY button should exist on start screen", playButton.waitForExists(3000))
    }

    @Test
    fun testOnlineButtonExists() {
        val onlineButton = device.findObject(UiSelector().textContains("ONLINE").className("android.widget.TextView"))
        assertTrue("ONLINE button should exist on start screen", onlineButton.waitForExists(3000))
    }
}
