package com.rcb.wake.reminders

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rcb.wake.R
import com.rcb.wake.alarm.AlarmController
import com.rcb.wake.notification.Localized
import com.rcb.wake.notification.WakeNotifications
import com.rcb.wake.storage.AppStorage
import com.rcb.wake.storage.SettingsStore
import com.rcb.wake.widget.CalendarEvents

/**
 * Small heads-up notifications a few minutes before the next alarm (silent, so it never wakes
 * anyone early) and before the next timed calendar event (a normal notification). One pending
 * alarm is kept for the earliest upcoming reminder and re-planned whenever alarms, settings or the
 * calendar change.
 */
object UpcomingReminders {
    const val CHANNEL_ALARM = "upcoming_alarm"
    const val CHANNEL_EVENT = "event_reminder"
    private const val REQUEST = 904
    private const val KEY_ALARM_DONE = "reminder_alarm_done"
    private const val KEY_EVENT_DONE = "reminder_event_done"

    private data class Due(val at: Long, val kind: Kind, val key: String)
    private enum class Kind { ALARM, EVENT }

    private fun pending(context: Context) = PendingIntent.getBroadcast(
        context,
        REQUEST,
        Intent(context, Receiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Upcoming reminders not yet shown, earliest first. */
    private fun upcoming(context: Context, now: Long): List<Due> {
        val settings = SettingsStore.load(context)
        if (!settings.remindersEnabled) return emptyList()
        val lead = settings.reminderLeadMinutes * 60_000L
        val state = AppStorage.state(context)
        val out = mutableListOf<Due>()
        AlarmController(context).nextAlarm()?.let { alarm ->
            val at = alarm.nextTriggerAt ?: return@let
            val key = "${alarm.id}@$at"
            if (at > now && state.getString(KEY_ALARM_DONE, null) != key) out += Due(at - lead, Kind.ALARM, key)
        }
        CalendarEvents.next(context)?.takeIf { !it.allDay && it.begin > now }?.let { event ->
            val key = "${event.title}@${event.begin}"
            if (state.getString(KEY_EVENT_DONE, null) != key) out += Due(event.begin - lead, Kind.EVENT, key)
        }
        return out.sortedBy { it.at }
    }

    /** (Re)plans the next reminder. Cheap; call after any change to alarms, settings or calendars. */
    fun reschedule(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val pi = pending(context)
        alarms.cancel(pi)
        val next = upcoming(context, System.currentTimeMillis()).firstOrNull() ?: return
        val at = maxOf(next.at, System.currentTimeMillis() + 1_000)
        // A reminder may be up to a few minutes late in deep sleep; that is fine for a heads-up.
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    /** Shows every reminder that is due now, then plans the next one. */
    fun fire(context: Context) {
        val now = System.currentTimeMillis()
        ensureChannels(context)
        val c = Localized.context(context)
        val settings = SettingsStore.load(context)
        val state = AppStorage.state(context)
        val nm = context.getSystemService(NotificationManager::class.java)
        for (due in upcoming(context, now).filter { it.at <= now + 30_000 }) {
            when (due.kind) {
                Kind.ALARM -> {
                    val alarm = AlarmController(context).nextAlarm() ?: continue
                    val at = alarm.nextTriggerAt ?: continue
                    val minutes = ((at - now + 30_000) / 60_000).coerceAtLeast(1)
                    val n = Notification.Builder(context, CHANNEL_ALARM)
                        .setSmallIcon(R.drawable.ic_stat_alarm)
                        .setContentTitle(c.getString(R.string.reminder_alarm_title, minutes.toInt()))
                        .setContentText(
                            WakeNotifications.formatTime(context, at) + " · " +
                                alarm.label.ifBlank { c.getString(R.string.alarm_default_label) },
                        )
                        .setAutoCancel(true)
                        .setTimeoutAfter(at - now + 60_000)
                        .setContentIntent(WakeNotifications.openAppIntent(context, 905))
                        .build()
                    nm.notify("reminder_alarm", 0, n)
                    state.edit().putString(KEY_ALARM_DONE, due.key).apply()
                }
                Kind.EVENT -> {
                    val event = CalendarEvents.next(context) ?: continue
                    val minutes = ((event.begin - now + 30_000) / 60_000).coerceAtLeast(1)
                    val n = Notification.Builder(context, CHANNEL_EVENT)
                        .setSmallIcon(R.drawable.ic_stat_alarm)
                        .setContentTitle(c.getString(R.string.reminder_event_title, event.title, minutes.toInt()))
                        .setContentText(WakeNotifications.formatTime(context, event.begin))
                        .setAutoCancel(true)
                        .setTimeoutAfter(event.end - now)
                        .setContentIntent(WakeNotifications.openAppIntent(context, 906))
                        .build()
                    nm.notify("reminder_event", 0, n)
                    state.edit().putString(KEY_EVENT_DONE, due.key).apply()
                }
            }
        }
        if (!settings.remindersEnabled) return
        reschedule(context)
    }

    fun ensureChannels(context: Context) {
        val c = Localized.context(context)
        val nm = context.getSystemService(NotificationManager::class.java)
        // Silent: a heads-up before a wake-up alarm must never wake anyone early.
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALARM, c.getString(R.string.channel_upcoming_alarm), NotificationManager.IMPORTANCE_LOW),
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_EVENT, c.getString(R.string.channel_event_reminder), NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = fire(context)
    }
}
