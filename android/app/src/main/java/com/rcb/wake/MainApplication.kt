package com.rcb.wake

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.UserManager
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeApplicationEntryPoint.loadReactNative
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import com.rcb.wake.module.RcbWakePackage

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

    private fun ensureReactNative() {
        if (reactLoaded) return
        reactLoaded = true
        loadReactNative(this)
    }
}
