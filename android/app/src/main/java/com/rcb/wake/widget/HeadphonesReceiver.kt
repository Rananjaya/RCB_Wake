package com.rcb.wake.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rcb.wake.storage.SettingsStore

/**
 * Re-renders the My Day widget when Bluetooth earphones connect, disconnect or report battery.
 * Android announces the Bluetooth link a few seconds before the earphones become an audio output,
 * so the widget is refreshed now and again shortly after.
 */
class HeadphonesReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!SettingsStore.load(context).widgetShowHeadphones) return
        GlanceWidgetProvider.updateAll(context)
        val pending = goAsync()
        Thread {
            try {
                for (delay in RETRY_DELAYS_MILLIS) {
                    Thread.sleep(delay)
                    GlanceWidgetProvider.updateAll(context)
                }
            } finally {
                pending.finish()
            }
        }.start()
    }

    private companion object {
        // Together well under the 10 s a receiver may run with goAsync().
        val RETRY_DELAYS_MILLIS = longArrayOf(2_000, 3_000, 3_000)
    }
}
