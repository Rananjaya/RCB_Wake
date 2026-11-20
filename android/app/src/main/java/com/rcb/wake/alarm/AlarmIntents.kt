package com.rcb.wake.alarm

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.rcb.wake.MainActivity
import com.rcb.wake.receiver.AlarmTriggerReceiver

/**
 * PendingIntent identities.
 *
 * Every alarm owns exactly one trigger PendingIntent, identified by action + data URI
 * (`rcbwake://alarm/<id>`) + request code. Snoozes reuse the same identity, so rescheduling
 * always replaces the previous registration and can never produce a duplicate.
 */
object AlarmIntents {
    const val ACTION_FIRE = "com.rcb.wake.action.ALARM_FIRE"
    const val EXTRA_ALARM_ID = "com.rcb.wake.extra.ALARM_ID"
    const val EXTRA_TRIGGER_AT = "com.rcb.wake.extra.TRIGGER_AT"

    fun alarmUri(alarmId: Long): Uri = Uri.parse("rcbwake://alarm/$alarmId")

    /** Request codes are the alarm id; timers use a separate offset range (see TimerIntents). */
    fun requestCode(alarmId: Long): Int {
        require(alarmId in 1..Int.MAX_VALUE / 2) { "Alarm id out of request-code range" }
        return alarmId.toInt()
    }

    fun fireIntent(context: Context, alarmId: Long, triggerAt: Long): Intent =
        Intent(context, AlarmTriggerReceiver::class.java)
            .setAction(ACTION_FIRE)
            .setData(alarmUri(alarmId))
            .putExtra(EXTRA_ALARM_ID, alarmId)
            .putExtra(EXTRA_TRIGGER_AT, triggerAt)

    fun firePendingIntent(context: Context, alarmId: Long, triggerAt: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode(alarmId),
            fireIntent(context, alarmId, triggerAt),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /** Returns the registered PendingIntent, or null if none exists for this alarm. */
    fun existingFirePendingIntent(context: Context, alarmId: Long): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            requestCode(alarmId),
            Intent(context, AlarmTriggerReceiver::class.java).setAction(ACTION_FIRE).setData(alarmUri(alarmId)),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )

    /** Opened when the user taps the system "next alarm" indicator. */
    fun showAppPendingIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
