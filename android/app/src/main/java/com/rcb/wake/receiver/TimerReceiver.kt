package com.rcb.wake.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rcb.wake.timer.PomodoroController
import com.rcb.wake.timer.TimerController

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TIMER_DONE -> {
                val id = intent.getLongExtra(EXTRA_TIMER_ID, -1)
                if (id > 0) TimerController(context).onFired(id)
            }
            ACTION_POMODORO_PHASE_END -> PomodoroController(context).onPhaseEnd()
        }
    }

    companion object {
        const val ACTION_TIMER_DONE = "com.rcb.wake.action.TIMER_DONE"
        const val ACTION_POMODORO_PHASE_END = "com.rcb.wake.action.POMODORO_PHASE_END"
        const val EXTRA_TIMER_ID = "com.rcb.wake.extra.TIMER_ID"
    }
}
