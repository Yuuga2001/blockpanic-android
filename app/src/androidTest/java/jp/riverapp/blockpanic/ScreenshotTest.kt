package jp.riverapp.blockpanic

import android.os.Environment
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
import java.io.File

@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

    private lateinit var device: UiDevice
    private lateinit var screenshotDir: File

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        screenshotDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "BlockPanic")
        screenshotDir.mkdirs()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = context.packageManager.getLaunchIntentForPackage("jp.riverapp.blockpanic")
        context.startActivity(intent!!)
        device.wait(Until.hasObject(By.pkg("jp.riverapp.blockpanic").depth(0)), 5000)
        Thread.sleep(2000)
    }

    @Test
    fun captureStartScreen() {
        takeScreenshot("01_start_screen")
    }

    @Test
    fun captureGameScreen() {
        val playText = L("play")
        val found = device.wait(Until.hasObject(By.textContains(playText)), 5000)
        if (found) {
            device.findObject(By.textContains(playText)).click()
            Thread.sleep(2000)
        }
        takeScreenshot("02_game_screen")
    }

    @Test
    fun captureRoomListScreen() {
        val onlineText = L("online")
        val found = device.wait(Until.hasObject(By.textContains(onlineText)), 5000)
        if (found) {
            device.findObject(By.textContains(onlineText)).click()
            Thread.sleep(2000)
        }
        takeScreenshot("03_room_list_screen")
    }

    private fun takeScreenshot(name: String) {
        val file = File(screenshotDir, "${name}.png")
        device.takeScreenshot(file)
        assertTrue("Screenshot should be saved: ${file.absolutePath}", file.exists())
        assertTrue("Screenshot should not be empty", file.length() > 0)
    }
}
