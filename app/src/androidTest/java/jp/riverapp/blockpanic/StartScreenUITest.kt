package jp.riverapp.blockpanic

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import jp.riverapp.blockpanic.i18n.L

@RunWith(AndroidJUnit4::class)
class StartScreenUITest {

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = context.packageManager.getLaunchIntentForPackage("jp.riverapp.blockpanic")
        assertNotNull(intent)
        context.startActivity(intent)
        device.wait(Until.hasObject(By.pkg("jp.riverapp.blockpanic").depth(0)), 5000)
        Thread.sleep(2000) // wait for Compose to render
    }

    @Test
    fun testAppLaunches() {
        assertEquals("jp.riverapp.blockpanic", device.currentPackageName)
    }

    @Test
    fun testPlayButtonExists() {
        val playText = L("play")
        val found = device.wait(Until.hasObject(By.textContains(playText)), 5000)
        assertTrue("'$playText' button should exist on start screen", found)
    }

    @Test
    fun testOnlineButtonExists() {
        val onlineText = L("online")
        val found = device.wait(Until.hasObject(By.textContains(onlineText)), 5000)
        assertTrue("'$onlineText' button should exist on start screen", found)
    }
}
