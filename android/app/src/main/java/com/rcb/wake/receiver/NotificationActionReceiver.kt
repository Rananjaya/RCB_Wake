package com.rcb.wake.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.rcb.wake.alarm.AlarmController
import com.rcb.wake.timer.TimerController

/** Handles notification buttons that are not part of an active ringing session. */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1)
        if (id <= 0) return
        when (intent.action) {
            ACTION_CANCEL_SNOOZE -> AlarmController(context).dismiss(listOf(id))
            ACTION_STOP_TIMER -> TimerController(context).reset(id)
            ACTION_ADD_MINUTE -> TimerController(context).restartWithExtra(id, 60_000L)
        }
    }

    companion object {
        const val ACTION_CANCEL_SNOOZE = "com.rcb.wake.action.CANCEL_SNOOZE"
        const val ACTION_STOP_TIMER = "com.rcb.wake.action.STOP_TIMER"
        const val ACTION_ADD_MINUTE = "com.rcb.wake.action.TIMER_ADD_MINUTE"
        private const val EXTRA_ID = "com.rcb.wake.extra.ID"

        private fun intent(context: Context, action: String, id: Long, kind: String) =
            Intent(context, NotificationActionReceiver::class.java)
                .setAction(action)
                .setData(Uri.parse("rcbwake://$kind/$id"))
                .putExtra(EXTRA_ID, id)

        fun cancelSnoozeIntent(context: Context, alarmId: Long) = intent(context, ACTION_CANCEL_SNOOZE, alarmId, "alarm")
        fun stopTimerIntent(context: Context, timerId: Long) = intent(context, ACTION_STOP_TIMER, timerId, "timer")
        fun addMinuteIntent(context: Context, timerId: Long) = intent(context, ACTION_ADD_MINUTE, timerId, "timer")
    }
}
