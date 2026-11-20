package com.rcb.wake.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rcb.wake.alarm.AlarmController
import com.rcb.wake.alarm.AlarmIntents
import com.rcb.wake.service.RingingService

/** Receives AlarmManager triggers. Validates the trigger, advances the alarm, then starts ringing. */
class AlarmTriggerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmIntents.ACTION_FIRE) return
        val id = intent.getLongExtra(AlarmIntents.EXTRA_ALARM_ID, -1)
        val triggerAt = intent.getLongExtra(AlarmIntents.EXTRA_TRIGGER_AT, -1)
        if (id <= 0) return
        val alarm = AlarmController(context).onFired(id, triggerAt) ?: return
        RingingService.ring(context, alarm.id)
    }
}
