package com.rcb.wake.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.rcb.wake.MainActivity
import com.rcb.wake.R
import com.rcb.wake.alarm.AlarmController
import com.rcb.wake.alarm.AlarmSessionStore
import com.rcb.wake.module.EngineEvents
import com.rcb.wake.notification.Localized
import com.rcb.wake.notification.WakeNotifications
import com.rcb.wake.service.RingingService
import com.rcb.wake.storage.SettingsStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Native full-screen ringing screen. Shown over the lock screen via the ringing notification's
 * full-screen intent. Works without React Native; the math challenge (when required) opens the
 * React Native app on top of the lock screen.
 */
class RingingActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val clockTick = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 1_000)
        }
    }
    private val sessionListener = EngineEvents.Listener { type ->
        if (type == EngineEvents.SESSION_ENDED) handler.post { finishAndRemoveTask() }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Localized.context(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        setContentView(R.layout.activity_ringing)
        applyTheme()
        findViewById<Button>(R.id.snooze).setOnClickListener {
            startService(RingingService.snoozeIntent(this))
        }
        findViewById<Button>(R.id.dismiss).setOnClickListener { onDismissPressed() }
        EngineEvents.addListener(sessionListener)
    }

    override fun onResume() {
        super.onResume()
        if (AlarmSessionStore.load(this) == null) {
            finishAndRemoveTask()
            return
        }
        bindSession()
        handler.post(clockTick)
    }

    override fun onPause() {
        handler.removeCallbacks(clockTick)
        super.onPause()
    }

    override fun onDestroy() {
        EngineEvents.removeListener(sessionListener)
        super.onDestroy()
    }

    @Deprecated("Back must not dismiss a ringing alarm")
    override fun onBackPressed() {
        // Intentionally ignored: the alarm is only stopped by snooze or dismiss.
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun bindSession() {
        val session = AlarmSessionStore.load(this) ?: return
        val controller = AlarmController(this)
        val labels = session.alarmIds.mapNotNull { controller.get(it)?.label?.takeIf(String::isNotBlank) }
        findViewById<TextView>(R.id.label).text =
            if (labels.isEmpty()) getString(R.string.alarm_default_label) else labels.joinToString(" · ")
        val challenge = findViewById<TextView>(R.id.challenge_info)
        val dismiss = findViewById<Button>(R.id.dismiss)
        if (session.challengeRequired) {
            val difficulty = when (session.challengeDifficulty) {
                "hard" -> getString(R.string.difficulty_hard)
                "medium" -> getString(R.string.difficulty_medium)
                else -> getString(R.string.difficulty_easy)
            }
            challenge.visibility = View.VISIBLE
            challenge.text = getString(R.string.ringing_challenge_info, session.challengeQuestionCount, difficulty)
            dismiss.text = getString(R.string.action_solve)
        } else {
            challenge.visibility = View.GONE
            dismiss.text = getString(R.string.action_dismiss)
        }
    }

    private fun updateClock() {
        val is24 = SettingsStore.load(this).is24Hour
        val now = Date()
        findViewById<TextView>(R.id.time).text = SimpleDateFormat(if (is24) "HH:mm" else "h:mm", Locale.getDefault()).format(now)
        val ampm = findViewById<TextView>(R.id.ampm)
        ampm.visibility = if (is24) View.GONE else View.VISIBLE
        ampm.text = SimpleDateFormat("a", Locale.getDefault()).format(now)
        findViewById<TextView>(R.id.date).text = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now)
    }

    private fun onDismissPressed() {
        val session = AlarmSessionStore.load(this) ?: return finishAndRemoveTask()
        if (!session.challengeRequired) {
            startService(RingingService.dismissIntent(this, RingingService.METHOD_NORMAL))
            return
        }
        // The challenge UI is React Native. MainActivity shows over the lock screen while a
        // challenge session is active, so the user does not need to unlock first.
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(WakeNotifications.EXTRA_OPEN_CHALLENGE, true),
        )
    }

    private fun applyTheme() {
        val settings = SettingsStore.load(this)
        val pink = settings.theme == SettingsStore.THEME_PINK
        val accent = SettingsStore.ACCENTS[settings.accentColor]
        val bg = Color.parseColor(accent?.let { if (pink) it.lightBg else it.darkBg } ?: if (pink) "#FFF4F2" else "#100D1C")
        val text = Color.parseColor(if (pink) "#3D2B3F" else "#F8F5FF")
        val muted = Color.parseColor(if (pink) "#8A6F86" else "#AAA1BB")
        val primary = Color.parseColor(accent?.let { if (pink) it.lightPrimary else it.darkPrimary } ?: if (pink) "#C2577F" else "#A855F7")
        val surface = Color.parseColor(accent?.let { if (pink) it.lightSurface else it.darkSurface } ?: if (pink) "#FBE3EA" else "#28213B")
        findViewById<View>(R.id.root).setBackgroundColor(bg)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        listOf(R.id.time).forEach { findViewById<TextView>(it).setTextColor(text) }
        listOf(R.id.ampm, R.id.date, R.id.challenge_info).forEach { findViewById<TextView>(it).setTextColor(muted) }
        findViewById<TextView>(R.id.label).setTextColor(text)
        findViewById<Button>(R.id.dismiss).apply {
            backgroundTintList = ColorStateList.valueOf(primary)
            setTextColor(if (pink) Color.WHITE else Color.parseColor("#100D1C"))
        }
        findViewById<Button>(R.id.snooze).apply {
            backgroundTintList = ColorStateList.valueOf(surface)
            setTextColor(text)
        }
    }
}
