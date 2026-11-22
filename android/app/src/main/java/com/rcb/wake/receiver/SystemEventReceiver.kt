package com.rcb.wake.receiver

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rcb.wake.alarm.AlarmController
import com.rcb.wake.timer.PomodoroController
import com.rcb.wake.sleep.SleepReminder
import com.rcb.wake.timer.TimerController
import com.rcb.wake.notification.WakeNotifications
import com.rcb.wake.widget.ClockWidgetProvider

/**
 * Re-registers alarms and timers whenever the OS may have dropped or invalidated them:
 * reboot (including Direct Boot), manual clock changes, time-zone changes, app updates and
 * exact-alarm permission changes.
 */
class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in HANDLED) return
        val pending = goAsync()
        Thread {
            try {
                AlarmController(context).reconcileAll(action.substringAfterLast('.'))
                TimerController(context).reconcileAll()
                PomodoroController(context).reconcile()
                // Resource ids change between app versions; re-render so the launcher drops
                // RemoteViews that reference stale drawables.
                ClockWidgetProvider.updateAll(context)
                ClockWidgetProvider.scheduleWeatherRefresh(context)
                WakeNotifications.updateNextAlarm(context, AlarmController(context).nextAlarm())
                SleepReminder.reschedule(context)
            } finally {
                pending.finish()
            }
        }.start()
    }

    private companion object {
        val HANDLED = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}
