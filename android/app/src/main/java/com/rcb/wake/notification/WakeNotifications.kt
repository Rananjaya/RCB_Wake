package com.rcb.wake.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.SystemClock
import com.rcb.wake.MainActivity
import com.rcb.wake.R
import com.rcb.wake.activity.RingingActivity
import com.rcb.wake.alarm.Alarm
import com.rcb.wake.alarm.AlarmSession
import com.rcb.wake.audio.AlarmSounds
import com.rcb.wake.receiver.NotificationActionReceiver
import com.rcb.wake.service.RingingService
import com.rcb.wake.storage.SettingsStore
import com.rcb.wake.timer.PomodoroPhase
import com.rcb.wake.timer.PomodoroState
import com.rcb.wake.timer.WakeTimer
import java.util.Date

object WakeNotifications {
    const val CHANNEL_RINGING = "alarm_ringing"
    const val CHANNEL_STATUS = "alarm_status"
    const val CHANNEL_TIMER_RUNNING = "timer_running"
    const val CHANNEL_TIMER_DONE = "timer_done"
    const val CHANNEL_POMODORO = "pomodoro"

    const val RINGING_NOTIFICATION_ID = 1
    private const val TAG_SNOOZE = "snooze"
    private const val TAG_MISSED = "missed"
    private const val TAG_TIMER = "timer"
    private const val TAG_TIMER_DONE = "timer_done"
    private const val TAG_POMODORO = "pomodoro"
    private const val TAG_POMODORO_DONE = "pomodoro_done"

    const val EXTRA_OPEN_CHALLENGE = "com.rcb.wake.extra.OPEN_CHALLENGE"
    const val EXTRA_OPEN_TAB = "com.rcb.wake.extra.OPEN_TAB"

    private fun manager(context: Context) = context.getSystemService(NotificationManager::class.java)

    fun ensureChannels(context: Context) {
        val c = Localized.context(context)
        val nm = manager(context)
        // The service plays alarm audio itself, so the ringing channel is silent.
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_RINGING, c.getString(R.string.channel_ringing), NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_STATUS, c.getString(R.string.channel_status), NotificationManager.IMPORTANCE_DEFAULT).apply {
                setSound(null, null)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_TIMER_RUNNING, c.getString(R.string.channel_timer_running), NotificationManager.IMPORTANCE_LOW),
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_TIMER_DONE, c.getString(R.string.channel_timer_done), NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(
                    AlarmSounds.builtInUri(context, R.raw.tone_classic),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )
        // A single gentle chime at each phase change; not insistent like finished timers.
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_POMODORO, c.getString(R.string.channel_pomodoro), NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(
                    AlarmSounds.builtInUri(context, R.raw.tone_chime),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            },
        )
    }

    fun canPostNotifications(context: Context) = manager(context).areNotificationsEnabled()

    fun canUseFullScreenIntent(context: Context) =
        Build.VERSION.SDK_INT < 34 || manager(context).canUseFullScreenIntent()

    private fun immutable() = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun formatTime(context: Context, millis: Long): String {
        val is24 = SettingsStore.load(context).is24Hour
        val pattern = if (is24) "HH:mm" else "h:mm a"
        return java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault()).format(Date(millis))
    }

    fun openAppIntent(context: Context, requestCode: Int, tab: String? = null, challenge: Boolean = false): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_OPEN_CHALLENGE, challenge)
        tab?.let { intent.putExtra(EXTRA_OPEN_TAB, it) }
        return PendingIntent.getActivity(context, requestCode, intent, immutable())
    }

    fun buildRinging(context: Context, session: AlarmSession, alarms: List<Alarm>): Notification {
        val c = Localized.context(context)
        val first = alarms.firstOrNull()
        val title = first?.label?.takeIf { it.isNotBlank() } ?: c.getString(R.string.alarm_default_label)
        val time = first?.let { String.format(java.util.Locale.ROOT, "%02d:%02d", it.hour, it.minute) }.orEmpty()
        val fullScreen = PendingIntent.getActivity(
            context,
            100,
            Intent(context, RingingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            immutable(),
        )
        val snooze = PendingIntent.getService(context, 101, RingingService.snoozeIntent(context), immutable())
        val builder = Notification.Builder(context, CHANNEL_RINGING)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(title)
            .setContentText(if (session.challengeRequired) c.getString(R.string.ringing_challenge_hint) else time)
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(fullScreen)
            .setFullScreenIntent(fullScreen, true)
            .addAction(Notification.Action.Builder(null, c.getString(R.string.action_snooze), snooze).build())
        if (session.challengeRequired) {
            builder.addAction(
                Notification.Action.Builder(null, c.getString(R.string.action_solve), openAppIntent(context, 102, challenge = true)).build(),
            )
        } else {
            val dismiss = PendingIntent.getService(context, 103, RingingService.dismissIntent(context, RingingService.METHOD_NORMAL), immutable())
            builder.addAction(Notification.Action.Builder(null, c.getString(R.string.action_dismiss), dismiss).build())
        }
        return builder.build()
    }

    fun showSnoozed(context: Context, alarm: Alarm, until: Long) {
        val c = Localized.context(context)
        val cancel = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            NotificationActionReceiver.cancelSnoozeIntent(context, alarm.id),
            immutable(),
        )
        val n = Notification.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(c.getString(R.string.snoozed_until, formatTime(context, until)))
            .setContentText(alarm.label.ifBlank { c.getString(R.string.alarm_default_label) })
            .setOngoing(true)
            .setContentIntent(openAppIntent(context, 200 + alarm.id.toInt()))
            .addAction(Notification.Action.Builder(null, c.getString(R.string.action_cancel_snooze), cancel).build())
            .build()
        manager(context).notify(TAG_SNOOZE, alarm.id.toInt(), n)
    }

    fun cancelSnoozed(context: Context, alarmId: Long) = manager(context).cancel(TAG_SNOOZE, alarmId.toInt())

    fun showMissed(context: Context, alarm: Alarm, scheduledAt: Long) {
        val c = Localized.context(context)
        val n = Notification.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(c.getString(R.string.missed_alarm))
            .setContentText(
                listOf(formatTime(context, scheduledAt), alarm.label).filter { it.isNotBlank() }.joinToString(" · "),
            )
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context, 300 + alarm.id.toInt()))
            .build()
        manager(context).notify(TAG_MISSED, alarm.id.toInt(), n)
    }

    fun showTimerRunning(context: Context, timer: WakeTimer) {
        val c = Localized.context(context)
        val remaining = (timer.endElapsedMillis ?: return) - SystemClock.elapsedRealtime()
        val n = Notification.Builder(context, CHANNEL_TIMER_RUNNING)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(timer.label.ifBlank { c.getString(R.string.timer_default_label) })
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(System.currentTimeMillis() + remaining)
            .setShowWhen(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent(context, 400 + timer.id.toInt(), tab = "timer"))
            .build()
        manager(context).notify(TAG_TIMER, timer.id.toInt(), n)
    }

    fun cancelTimer(context: Context, timerId: Long) {
        manager(context).cancel(TAG_TIMER, timerId.toInt())
        manager(context).cancel(TAG_TIMER_DONE, timerId.toInt())
    }

    fun showTimerDone(context: Context, timer: WakeTimer) {
        val c = Localized.context(context)
        manager(context).cancel(TAG_TIMER, timer.id.toInt())
        val stop = PendingIntent.getBroadcast(
            context, 500 + timer.id.toInt(), NotificationActionReceiver.stopTimerIntent(context, timer.id), immutable(),
        )
        val addMinute = PendingIntent.getBroadcast(
            context, 600 + timer.id.toInt(), NotificationActionReceiver.addMinuteIntent(context, timer.id), immutable(),
        )
        val n = Notification.Builder(context, CHANNEL_TIMER_DONE)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(c.getString(R.string.timer_done))
            .setContentText(timer.label.ifBlank { c.getString(R.string.timer_default_label) })
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(openAppIntent(context, 700 + timer.id.toInt(), tab = "timer"))
            .setDeleteIntent(stop)
            .setTimeoutAfter(TIMER_ALERT_TIMEOUT_MILLIS)
            .addAction(Notification.Action.Builder(null, c.getString(R.string.action_stop), stop).build())
            .addAction(Notification.Action.Builder(null, c.getString(R.string.action_add_minute), addMinute).build())
            .build()
        // Insistent: the alarm-usage channel sound repeats until the user stops it.
        n.flags = n.flags or Notification.FLAG_INSISTENT
        manager(context).notify(TAG_TIMER_DONE, timer.id.toInt(), n)
    }

    fun cancelTimerDone(context: Context, timerId: Long) = manager(context).cancel(TAG_TIMER_DONE, timerId.toInt())

    private const val TIMER_ALERT_TIMEOUT_MILLIS = 10 * 60_000L

    private fun phaseName(c: Context, phase: PomodoroPhase) = c.getString(
        when (phase) {
            PomodoroPhase.FOCUS -> R.string.pomodoro_focus
            PomodoroPhase.SHORT_BREAK -> R.string.pomodoro_short_break
            PomodoroPhase.LONG_BREAK -> R.string.pomodoro_long_break
        },
    )

    fun showPomodoroRunning(context: Context, state: PomodoroState) {
        val c = Localized.context(context)
        val remaining = (state.endElapsedMillis ?: return) - SystemClock.elapsedRealtime()
        val n = Notification.Builder(context, CHANNEL_TIMER_RUNNING)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(phaseName(c, state.phase))
            .setContentText(c.getString(R.string.pomodoro_round, state.completedFocus + 1, state.config.roundsBeforeLongBreak))
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(System.currentTimeMillis() + remaining)
            .setShowWhen(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent(context, 800, tab = "timer"))
            .build()
        manager(context).notify(TAG_POMODORO, 0, n)
    }

    fun cancelPomodoro(context: Context) = manager(context).cancel(TAG_POMODORO, 0)

    fun showPomodoroPhaseDone(context: Context, finished: PomodoroPhase, next: PomodoroPhase) {
        val c = Localized.context(context)
        val n = Notification.Builder(context, CHANNEL_POMODORO)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(c.getString(R.string.pomodoro_phase_done, phaseName(c, finished)))
            .setContentText(c.getString(R.string.pomodoro_next, phaseName(c, next)))
            .setAutoCancel(true)
            .setTimeoutAfter(5 * 60_000L)
            .setContentIntent(openAppIntent(context, 801, tab = "timer"))
            .build()
        manager(context).notify(TAG_POMODORO_DONE, 0, n)
    }
}
