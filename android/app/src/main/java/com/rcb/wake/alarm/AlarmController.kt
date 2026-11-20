package com.rcb.wake.alarm

import android.content.Context
import android.util.Log
import com.rcb.wake.module.EngineEvents
import com.rcb.wake.notification.WakeNotifications
import com.rcb.wake.storage.AlarmRepository
import java.time.ZoneId

/**
 * Application-level alarm operations. Every mutation follows the same order:
 * persist configuration -> schedule with the OS -> persist what the OS accepted -> notify listeners.
 *
 * All methods are synchronized on a process-wide lock so concurrent receivers, the ringing service
 * and the React Native module cannot interleave and double-schedule.
 */
class AlarmController(context: Context) {
    private val appContext = context.applicationContext
    private val repository = AlarmRepository(appContext)
    private val scheduler = AlarmScheduler(appContext)

    private fun now() = System.currentTimeMillis()
    private fun zone() = ZoneId.systemDefault()

    fun getAll(): List<Alarm> = repository.getAll()

    fun get(id: Long): Alarm? = repository.get(id)

    fun save(input: AlarmInput): Alarm = synchronized(LOCK) {
        val valid = AlarmValidator.validate(input)
        val id = repository.upsert(valid, now())
        val alarm = reschedule(id)
        EngineEvents.alarmsChanged()
        alarm
    }

    fun setEnabled(id: Long, enabled: Boolean): Alarm = synchronized(LOCK) {
        requireNotNull(repository.get(id)) { "Alarm not found" }
        repository.setEnabled(id, enabled, now())
        val alarm = reschedule(id)
        EngineEvents.alarmsChanged()
        alarm
    }

    fun delete(id: Long) = synchronized(LOCK) {
        scheduler.cancel(id)
        repository.delete(id)
        WakeNotifications.cancelSnoozed(appContext, id)
        EngineEvents.alarmsChanged()
    }

    fun cancelAll() = synchronized(LOCK) {
        repository.getAll().forEach {
            scheduler.cancel(it.id)
            repository.setEnabled(it.id, false, now())
            repository.setScheduleResult(it.id, null, ScheduleState.NONE)
            WakeNotifications.cancelSnoozed(appContext, it.id)
        }
        EngineEvents.alarmsChanged()
    }

    /** Recomputes and (re)registers a single alarm. Idempotent. */
    private fun reschedule(id: Long): Alarm {
        val alarm = requireNotNull(repository.get(id)) { "Alarm not found" }
        val triggerAt = AlarmTimeCalculator.effectiveTriggerMillis(alarm, now(), zone())
        if (triggerAt == null) {
            scheduler.cancel(id)
            repository.setScheduleResult(id, null, ScheduleState.NONE)
        } else {
            val state = scheduler.schedule(id, triggerAt)
            repository.setScheduleResult(id, if (state == ScheduleState.FAILED) null else triggerAt, state)
        }
        return requireNotNull(repository.get(id))
    }

    /**
     * Brings the OS registrations in line with the database. Called on app start, boot,
     * time/zone change, package update and exact-alarm permission changes.
     *
     * Alarms whose scheduled time passed while they could not ring (device off, clock jumped)
     * ring now if they are at most [LATE_RING_GRACE_MILLIS] late; otherwise they are reported as
     * missed and one-time alarms are switched off.
     */
    fun reconcileAll(reason: String) = synchronized(LOCK) {
        val now = now()
        val ringing = AlarmSessionStore.load(appContext)?.alarmIds.orEmpty()
        var changed = false
        for (alarm in repository.getAll()) {
            if (alarm.id in ringing) continue
            val previous = alarm.nextTriggerAt
            val wasArmed = alarm.enabled || alarm.snoozedUntil != null
            if (wasArmed && previous != null && previous <= now) {
                if (now - previous <= LATE_RING_GRACE_MILLIS) {
                    val state = scheduler.schedule(alarm.id, now + 1_000)
                    repository.setScheduleResult(alarm.id, now + 1_000, state)
                    changed = true
                    continue
                }
                WakeNotifications.showMissed(appContext, alarm, previous)
                repository.setSnoozedUntil(alarm.id, null)
                if (!alarm.isRepeating) repository.setEnabled(alarm.id, false, now)
                changed = true
            }
            val before = repository.get(alarm.id)
            val after = reschedule(alarm.id)
            if (before?.nextTriggerAt != after.nextTriggerAt || before?.scheduleState != after.scheduleState) changed = true
        }
        Log.i(TAG, "Reconciled alarms ($reason)")
        if (changed) EngineEvents.alarmsChanged()
    }

    /**
     * Called by the trigger receiver. Returns the alarm if it should start ringing, or null if the
     * broadcast is stale (alarm deleted, disabled or rescheduled since this intent was created).
     *
     * Before returning, the alarm is advanced so a lost ringing session can never lose the alarm:
     * repeating alarms are already registered for their next occurrence, one-time alarms are off.
     */
    fun onFired(id: Long, intentTriggerAt: Long): Alarm? = synchronized(LOCK) {
        val alarm = repository.get(id) ?: return null
        val expected = alarm.nextTriggerAt
        if (expected == null || kotlin.math.abs(expected - intentTriggerAt) > STALE_TOLERANCE_MILLIS) {
            Log.i(TAG, "Ignoring stale trigger for alarm $id")
            return null
        }
        if (!alarm.enabled && alarm.snoozedUntil == null) return null
        repository.setSnoozedUntil(id, null)
        WakeNotifications.cancelSnoozed(appContext, id)
        if (alarm.isRepeating) {
            reschedule(id)
        } else {
            repository.setEnabled(id, false, now())
            scheduler.cancel(id)
            repository.setScheduleResult(id, null, ScheduleState.RINGING)
        }
        EngineEvents.alarmsChanged()
        alarm
    }

    /** Snoozes the given alarms. Returns the snooze end time of the first alarm. */
    fun snooze(ids: Collection<Long>): Long? = synchronized(LOCK) {
        var first: Long? = null
        for (id in ids) {
            val alarm = repository.get(id) ?: continue
            val until = AlarmTimeCalculator.snoozeUntilMillis(now(), alarm.snoozeMinutes)
            if (!alarm.enabled) repository.setEnabled(id, true, now())
            repository.setSnoozedUntil(id, until)
            val updated = reschedule(id)
            if (updated.scheduleState == ScheduleState.EXACT || updated.scheduleState == ScheduleState.INEXACT) {
                WakeNotifications.showSnoozed(appContext, updated, until)
            }
            if (first == null) first = until
        }
        EngineEvents.alarmsChanged()
        first
    }

    /** Ends ringing/snooze for the given alarms and arms their next regular occurrence. */
    fun dismiss(ids: Collection<Long>) = synchronized(LOCK) {
        for (id in ids) {
            val alarm = repository.get(id) ?: continue
            repository.setSnoozedUntil(id, null)
            WakeNotifications.cancelSnoozed(appContext, id)
            if (!alarm.isRepeating) repository.setEnabled(id, false, now())
            reschedule(id)
        }
        EngineEvents.alarmsChanged()
    }

    fun nextAlarm(): Alarm? =
        repository.getAll()
            .filter { it.nextTriggerAt != null && it.scheduleState != ScheduleState.FAILED }
            .minByOrNull { it.nextTriggerAt!! }

    fun isRegistered(id: Long) = scheduler.isRegistered(id)

    fun canScheduleExact() = scheduler.canScheduleExact()

    companion object {
        private const val TAG = "RcbWakeAlarms"
        private val LOCK = Any()
        const val LATE_RING_GRACE_MILLIS = 10 * 60_000L
        const val STALE_TOLERANCE_MILLIS = 5_000L
    }
}
