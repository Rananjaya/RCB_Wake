package com.rcb.wake.audio

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Alarm vibration. Each cycle is started separately instead of as one repeating waveform, because
 * another app's notification vibration replaces a repeating vibration for good on older Android
 * versions; re-starting every cycle makes the alarm resume vibrating right after it.
 */
class AlarmVibrator(context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private val cycle = object : Runnable {
        override fun run() {
            if (!running) return
            vibrateOnce()
            handler.postDelayed(this, PATTERN.sum())
        }
    }

    private val vibrator: Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    fun start() {
        if (!vibrator.hasVibrator() || running) return
        running = true
        handler.post(cycle)
    }

    private fun vibrateOnce() {
        val effect = VibrationEffect.createWaveform(PATTERN, -1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(
                effect,
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
            )
        }
    }

    fun stop() {
        running = false
        handler.removeCallbacks(cycle)
        vibrator.cancel()
    }

    private companion object {
        val PATTERN = longArrayOf(0, 600, 400, 600, 1200)
    }
}
