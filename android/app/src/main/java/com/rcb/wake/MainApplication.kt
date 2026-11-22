package com.rcb.wake

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.UserManager
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeApplicationEntryPoint.loadReactNative
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import com.rcb.wake.alarm.AlarmController
import com.rcb.wake.module.EngineEvents
import com.rcb.wake.module.RcbWakePackage
import com.rcb.wake.notification.WakeNotifications
import com.rcb.wake.sleep.SleepReminder
import com.rcb.wake.storage.SettingsStore
import com.rcb.wake.widget.ClockWidgetProvider
import com.rcb.wake.widget.GlanceWidgetProvider

class MainApplication : Application(), ReactApplication {
    override val reactHost: ReactHost by lazy {
        getDefaultReactHost(
            context = applicationContext,
            packageList = PackageList(this).packages.apply { add(RcbWakePackage()) },
        )
    }

    private var reactLoaded = false

    override fun onCreate() {
        super.onCreate()
        // Keep the home-screen widget's next-alarm and format in sync with engine changes.
        EngineEvents.addListener { type ->
            if (type == EngineEvents.ALARMS_CHANGED || type == EngineEvents.SETTINGS_CHANGED) {
                ClockWidgetProvider.updateAll(this)
                SleepReminder.reschedule(this)
                WakeNotifications.updateNextAlarm(this, AlarmController(this).nextAlarm())
            }
        }
        // Earphones plugged in or out (wired ones send no manifest broadcast) refresh the glance widget.
        getSystemService(AudioManager::class.java).registerAudioDeviceCallback(
            object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>) = refreshGlance()
                override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>) = refreshGlance()
            },
            Handler(Looper.getMainLooper()),
        )
        // The alarm engine is Direct Boot aware, so this process can start before the user unlocks.
        // React Native is only needed for the UI, which cannot be opened until unlock.
        if (getSystemService(UserManager::class.java).isUserUnlocked) {
            ensureReactNative()
        } else {
            registerReceiver(
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        ensureReactNative()
                        unregisterReceiver(this)
                    }
                },
                IntentFilter(Intent.ACTION_USER_UNLOCKED),
            )
        }
    }

    private fun refreshGlance() {
        if (SettingsStore.load(this).widgetShowHeadphones) GlanceWidgetProvider.updateAll(this)
    }

    private fun ensureReactNative() {
        if (reactLoaded) return
        reactLoaded = true
        loadReactNative(this)
    }
}
