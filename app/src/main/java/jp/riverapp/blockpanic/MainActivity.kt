package jp.riverapp.blockpanic

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import jp.riverapp.blockpanic.game.GameCoordinator
import jp.riverapp.blockpanic.i18n.LocalizationManager
import jp.riverapp.blockpanic.model.GameRecordStore
import jp.riverapp.blockpanic.network.WebRTCConfig
import jp.riverapp.blockpanic.ui.GameScreen

class MainActivity : ComponentActivity() {
    private lateinit var coordinator: GameCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize subsystems
        LocalizationManager.init(this)
        GameRecordStore.init(this)
        WebRTCConfig.initialize(this)

        // Create game coordinator
        coordinator = GameCoordinator()

        // Fullscreen immersive
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            GameScreen(coordinator)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coordinator.destroy()
    }
}
