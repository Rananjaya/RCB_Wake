package com.rcb.wake.alarm

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.util.Log

/** Thin wrapper over AlarmManager that reports what the OS actually accepted. */
class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun schedule(alarmId: Long, triggerAt: Long): ScheduleState {
        val operation = AlarmIntents.firePendingIntent(context, alarmId, triggerAt)
        return try {
            if (canScheduleExact()) {
                val info = AlarmManager.AlarmClockInfo(triggerAt, AlarmIntents.showAppPendingIntent(context))
                alarmManager.setAlarmClock(info, operation)
                ScheduleState.EXACT
            } else {
                // Without exact-alarm access the best the OS allows is a Doze-tolerant inexact alarm.
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
                ScheduleState.INEXACT
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "AlarmManager rejected alarm $alarmId", e)
            ScheduleState.FAILED
        }
    }

    fun cancel(alarmId: Long) {
        AlarmIntents.existingFirePendingIntent(context, alarmId)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    fun isRegistered(alarmId: Long): Boolean = AlarmIntents.existingFirePendingIntent(context, alarmId) != null

    private companion object {
        const val TAG = "RcbWakeScheduler"
    }
}
