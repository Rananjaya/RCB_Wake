package com.rcb.wake.storage

import android.content.Context
import android.content.SharedPreferences

/** Device-protected storage helpers, readable during Direct Boot. */
object AppStorage {
    private const val SETTINGS_PREFS = "rcb_wake_settings"
    private const val STATE_PREFS = "rcb_wake_state"

    fun deviceContext(context: Context): Context =
        context.applicationContext.createDeviceProtectedStorageContext()

    /** User preferences (theme, language, defaults). */
    fun settings(context: Context): SharedPreferences =
        deviceContext(context).getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)

    /** Small runtime state: active ringing session, stopwatch, challenge progress. */
    fun state(context: Context): SharedPreferences =
        deviceContext(context).getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
}
