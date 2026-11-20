package com.rcb.wake.sleep

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.rcb.wake.alarm.AlarmController
import com.rcb.wake.notification.WakeNotifications
import com.rcb.wake.storage.SettingsStore

/** Pure bedtime arithmetic, unit tested on the JVM. */
object SleepMath {
    /**
     * When to remind the user to go to bed so they get [sleepGoalMinutes] of sleep before the alarm
     * at [nextAlarmAt], [leadMinutes] ahead of bedtime. Returns null if that moment has passed.
     */
    fun reminderAt(nextAlarmAt: Long?, sleepGoalMinutes: Int, leadMinutes: Int, now: Long): Long? {
        if (nextAlarmAt == null || nextAlarmAt <= now) return null
        val remindAt = nextAlarmAt - (sleepGoalMinutes + leadMinutes) * 60_000L
        return remindAt.takeIf { it > now }
    }

    fun bedtime(nextAlarmAt: Long, sleepGoalMinutes: Int): Long = nextAlarmAt - sleepGoalMinutes * 60_000L
}

/**
 * Bedtime reminder derived from the next scheduled alarm. Recomputed whenever alarms or settings
 * change and after reboot; there is only ever one pending reminder.
 */
object SleepReminder {
    private const val ACTION = "com.rcb.wake.action.SLEEP_REMINDER"
    private const val EXTRA_ALARM_AT = "com.rcb.wake.extra.ALARM_AT"
    private const val REQUEST_CODE = Int.MAX_VALUE / 2 - 1

    private fun pendingIntent(context: Context, alarmAt: Long, flags: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, SleepReminderReceiver::class.java).setAction(ACTION).putExtra(EXTRA_ALARM_AT, alarmAt),
            flags or PendingIntent.FLAG_IMMUTABLE,
        )

    fun reschedule(context: Context) {
        val settings = SettingsStore.load(context)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        pendingIntent(context, 0, PendingIntent.FLAG_NO_CREATE)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
        if (!settings.sleepReminderEnabled) return
        val next = AlarmController(context).nextAlarm()?.nextTriggerAt
        val remindAt = SleepMath.reminderAt(next, settings.sleepGoalMinutes, settings.sleepReminderLeadMinutes, System.currentTimeMillis())
            ?: return
        val pi = pendingIntent(context, next!!, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        // A bedtime nudge does not need to be to-the-second; a Doze-friendly window is fine when exact is unavailable.
        if (exact) alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, remindAt, pi)
        else alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, remindAt, pi)
    }

    internal fun handle(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val settings = SettingsStore.load(context)
        val alarmAt = intent.getLongExtra(EXTRA_ALARM_AT, 0)
        // Ignore if the alarm this reminder was computed for has since moved.
        val current = AlarmController(context).nextAlarm()?.nextTriggerAt
        if (settings.sleepReminderEnabled && current == alarmAt) {
            WakeNotifications.showSleepReminder(context, alarmAt, settings.sleepGoalMinutes)
        }
        reschedule(context)
    }
}

class SleepReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = SleepReminder.handle(context, intent)
}
