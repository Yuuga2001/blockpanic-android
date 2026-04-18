package jp.riverapp.blockpanic.util

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object Haptics {
    // Death vibration waveform: [delay, vibrate, pause, vibrate] (ms)
    private val DEATH_WAVEFORM = longArrayOf(0, 80, 60, 120)

    @Volatile
    private var vibrator: Vibrator? = null

    fun init(context: Context) {
        // Use applicationContext to avoid activity leaks
        val appCtx = context.applicationContext
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (appCtx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appCtx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /** Death / game over vibration (two short pulses) */
    fun death() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        // minSdk = 26, so VibrationEffect is always available
        val effect = VibrationEffect.createWaveform(DEATH_WAVEFORM, -1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+: VibrationAttributes (USAGE_GAME is not defined, USAGE_MEDIA is closest)
            val attrs = VibrationAttributes.Builder()
                .setUsage(VibrationAttributes.USAGE_MEDIA)
                .build()
            v.vibrate(effect, attrs)
        } else {
            // API 26-32: AudioAttributes with USAGE_GAME to respect DND / user settings
            val audioAttrs = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_GAME)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            v.vibrate(effect, audioAttrs)
        }
    }
}
