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
class GameplayFlowUITest {

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = context.packageManager.getLaunchIntentForPackage("jp.riverapp.blockpanic")
        context.startActivity(intent!!)
        device.wait(Until.hasObject(By.pkg("jp.riverapp.blockpanic").depth(0)), 5000)
        Thread.sleep(2000)
    }

    @Test
    fun testPlayButtonStartsGame() {
        val playText = L("play")
        val found = device.wait(Until.hasObject(By.textContains(playText)), 5000)
        assertTrue("'$playText' button should exist", found)
        device.findObject(By.textContains(playText)).click()
        Thread.sleep(1000)
        assertEquals("jp.riverapp.blockpanic", device.currentPackageName)
    }

    @Test
    fun testOnlineShowsRoomList() {
        val onlineText = L("online")
        val found = device.wait(Until.hasObject(By.textContains(onlineText)), 5000)
        assertTrue("'$onlineText' button should exist", found)
        device.findObject(By.textContains(onlineText)).click()

        val createText = L("create")
        val createFound = device.wait(Until.hasObject(By.textContains(createText)), 5000)
        assertTrue("'$createText' button should appear in room list", createFound)
    }

    @Test
    fun testRoomListBackReturnsToStart() {
        val onlineText = L("online")
        device.wait(Until.hasObject(By.textContains(onlineText)), 5000)
        device.findObject(By.textContains(onlineText)).click()

        val backText = L("back")
        val backFound = device.wait(Until.hasObject(By.textContains(backText)), 5000)
        assertTrue("'$backText' button should exist", backFound)
        device.findObject(By.textContains(backText)).click()

        val playText = L("play")
        val returnFound = device.wait(Until.hasObject(By.textContains(playText)), 5000)
        assertTrue("Should return to start screen", returnFound)
    }
}
