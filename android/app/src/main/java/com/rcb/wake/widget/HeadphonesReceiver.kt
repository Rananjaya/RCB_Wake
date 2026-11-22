package com.rcb.wake.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rcb.wake.storage.SettingsStore

/** Re-renders the My Day widget when Bluetooth earphones connect, disconnect or report battery. */
class HeadphonesReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (SettingsStore.load(context).widgetShowHeadphones) GlanceWidgetProvider.updateAll(context)
    }
}
