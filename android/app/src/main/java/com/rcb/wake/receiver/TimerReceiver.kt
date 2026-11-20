package com.rcb.wake.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rcb.wake.timer.TimerController

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TIMER_DONE) return
        val id = intent.getLongExtra(EXTRA_TIMER_ID, -1)
        if (id > 0) TimerController(context).onFired(id)
    }

    companion object {
        const val ACTION_TIMER_DONE = "com.rcb.wake.action.TIMER_DONE"
        const val EXTRA_TIMER_ID = "com.rcb.wake.extra.TIMER_ID"
    }
}
