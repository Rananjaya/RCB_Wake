package com.rcb.wake.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import com.rcb.wake.MainActivity
import com.rcb.wake.storage.AppStorage

/**
 * Invisible trampoline for My Day widget taps. A single tap switches the page; a second tap within
 * [DOUBLE_TAP_MILLIS] undoes that switch and opens the app. Widgets only receive clicks, so the
 * double tap is recognised from the time between two clicks.
 */
class WidgetTapActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val hasPages = intent.getBooleanExtra(EXTRA_HAS_PAGES, false)
        val state = AppStorage.state(this)
        val key = "glance_last_tap_$id"
        val now = SystemClock.elapsedRealtime()
        val isDouble = now - state.getLong(key, 0L) < DOUBLE_TAP_MILLIS
        when {
            !hasPages || id == AppWidgetManager.INVALID_APPWIDGET_ID -> openApp()
            isDouble -> {
                state.edit().remove(key).apply()
                GlanceWidgetProvider.nextPage(this, id, step = -1) // undo the first tap's page change
                openApp()
            }
            else -> {
                state.edit().putLong(key, now).apply()
                GlanceWidgetProvider.nextPage(this, id)
            }
        }
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    private fun openApp() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    companion object {
        const val EXTRA_HAS_PAGES = "com.rcb.wake.extra.HAS_PAGES"
        const val DOUBLE_TAP_MILLIS = 450L
    }
}
