package com.rcb.wake

import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate
import com.rcb.wake.alarm.AlarmSessionStore
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

    override fun onCreate(savedInstanceState: Bundle?) {
        // React Native Screens restores fragments itself; passing state here would crash on restore.
        super.onCreate(null)
        applyChallengeWindow(intent)
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
}
