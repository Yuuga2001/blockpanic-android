package jp.riverapp.blockpanic

import android.graphics.Bitmap
import android.os.Environment
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
import java.io.File
import java.io.FileOutputStream

/**
 * Screenshot capture tests for visual regression testing.
 * Captures reference screenshots of each screen state.
 * Screenshots are saved to /sdcard/Pictures/BlockPanic/ for manual review.
 *
 * To run: ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=jp.riverapp.blockpanic.ScreenshotTest
 * To pull: adb pull /sdcard/Pictures/BlockPanic/ ./screenshots/
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

    private lateinit var device: UiDevice
    private lateinit var screenshotDir: File

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Create screenshot directory
        screenshotDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "BlockPanic")
        screenshotDir.mkdirs()

        // Launch app
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = context.packageManager.getLaunchIntentForPackage("jp.riverapp.blockpanic")
        context.startActivity(intent!!)
        device.wait(Until.hasObject(By.pkg("jp.riverapp.blockpanic").depth(0)), 5000)
    }

    @Test
    fun captureStartScreen() {
        Thread.sleep(2000) // Wait for rendering
        takeScreenshot("01_start_screen")
    }

    @Test
    fun captureGameScreen() {
        val playButton = device.findObject(UiSelector().textContains("PLAY"))
        playButton.waitForExists(3000)
        playButton.click()
        Thread.sleep(2000) // Wait for game to start and render
        takeScreenshot("02_game_screen")
    }

    @Test
    fun captureRoomListScreen() {
        val onlineButton = device.findObject(UiSelector().textContains("ONLINE"))
        onlineButton.waitForExists(3000)
        onlineButton.click()
        Thread.sleep(2000)
        takeScreenshot("03_room_list_screen")
    }

    private fun takeScreenshot(name: String) {
        val file = File(screenshotDir, "${name}.png")
        device.takeScreenshot(file)
        assertTrue("Screenshot should be saved: ${file.absolutePath}", file.exists())
        assertTrue("Screenshot should not be empty", file.length() > 0)
    }
}
