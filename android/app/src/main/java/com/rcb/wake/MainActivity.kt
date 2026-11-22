package com.rcb.wake

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate
import com.rcb.wake.alarm.AlarmSessionStore
import com.rcb.wake.module.EngineEvents
import com.rcb.wake.notification.WakeNotifications

class MainActivity : ReactActivity() {
    override fun getMainComponentName(): String = "RCBWake"

    override fun createReactActivityDelegate(): ReactActivityDelegate =
        object : DefaultReactActivityDelegate(this, mainComponentName, fabricEnabled) {
            // Lets notifications open a specific tab (e.g. a finished timer opens the Timer tab).
            override fun getLaunchOptions(): Bundle? =
                intent?.getStringExtra(WakeNotifications.EXTRA_OPEN_TAB)?.let { tab ->
                    Bundle().apply { putString("initialTab", tab) }
                }
        }

    private val handler = Handler(Looper.getMainLooper())
    private val sessionListener = EngineEvents.Listener { type ->
        if (type == EngineEvents.SESSION_ENDED) handler.post { clearChallengeWindow() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // React Native Screens restores fragments itself; passing state here would crash on restore.
        super.onCreate(null)
        applyChallengeWindow(intent)
        EngineEvents.addListener(sessionListener)
    }

    override fun onResume() {
        super.onResume()
        // The alarm may have ended while the app was in the background.
        if (AlarmSessionStore.load(this) == null) clearChallengeWindow()
    }

    override fun onDestroy() {
        EngineEvents.removeListener(sessionListener)
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyChallengeWindow(intent)
    }

    /** While a challenge must be solved, show over the lock screen so the user can dismiss without unlocking. */
    private fun applyChallengeWindow(intent: Intent?) {
        val wantsChallenge = intent?.getBooleanExtra(WakeNotifications.EXTRA_OPEN_CHALLENGE, false) == true
        if (!wantsChallenge || AlarmSessionStore.load(this) == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
    }

    /** Once the alarm is over, the app must no longer appear over the lock screen or wake the screen. */
    private fun clearChallengeWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(false)
            setTurnScreenOn(false)
        }
    }
}
