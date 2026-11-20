package com.rcb.wake.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import com.rcb.wake.module.EngineEvents
import com.rcb.wake.notification.WakeNotifications
import com.rcb.wake.receiver.TimerReceiver
import com.rcb.wake.storage.AppStorage

/**
 * Countdown timers. Completion is scheduled with AlarmManager on the monotonic clock
 * (ELAPSED_REALTIME_WAKEUP), so manual clock and time-zone changes do not affect timers and
 * completion never depends on JavaScript.
 */
class TimerController(context: Context) {
    private val appContext = context.applicationContext
    private val repository = TimerRepository(appContext)
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    private fun nowElapsed() = SystemClock.elapsedRealtime()
    private fun nowWall() = System.currentTimeMillis()

    fun getAll(): List<WakeTimer> = repository.getAll()

    fun create(label: String, durationMillis: Long, start: Boolean): WakeTimer = synchronized(LOCK) {
        val id = repository.insert(label.trim().take(40), TimerMath.validateDuration(durationMillis), nowWall())
        if (start) startLocked(id) else changed()
        requireNotNull(repository.get(id))
    }

    fun start(id: Long): WakeTimer = synchronized(LOCK) { startLocked(id) }

    private fun startLocked(id: Long): WakeTimer {
        val timer = requireNotNull(repository.get(id)) { "Timer not found" }
        if (timer.status == TimerStatus.RUNNING) return timer
        val remaining = if (timer.status == TimerStatus.FINISHED || timer.remainingMillis <= 0) timer.durationMillis else timer.remainingMillis
        return run(timer, remaining)
    }

    private fun run(timer: WakeTimer, remaining: Long): WakeTimer {
        val running = timer.copy(
            status = TimerStatus.RUNNING,
            remainingMillis = remaining,
            endElapsedMillis = nowElapsed() + remaining,
            endTimeMillis = nowWall() + remaining,
        )
        repository.update(running)
        rememberBoot()
        schedule(running)
        WakeNotifications.cancelTimerDone(appContext, timer.id)
        WakeNotifications.showTimerRunning(appContext, running)
        changed()
        return running
    }

    fun pause(id: Long): WakeTimer = synchronized(LOCK) {
        val timer = requireNotNull(repository.get(id)) { "Timer not found" }
        if (timer.status != TimerStatus.RUNNING) return timer
        val paused = timer.copy(
            status = TimerStatus.PAUSED,
            remainingMillis = TimerMath.remaining(timer, nowElapsed()),
            endElapsedMillis = null,
            endTimeMillis = null,
        )
        repository.update(paused)
        cancelSchedule(id)
        WakeNotifications.cancelTimer(appContext, id)
        changed()
        paused
    }

    fun reset(id: Long): WakeTimer? = synchronized(LOCK) {
        val timer = repository.get(id) ?: return null
        val idle = timer.copy(status = TimerStatus.IDLE, remainingMillis = timer.durationMillis, endElapsedMillis = null, endTimeMillis = null)
        repository.update(idle)
        cancelSchedule(id)
        WakeNotifications.cancelTimer(appContext, id)
        changed()
        idle
    }

    fun addTime(id: Long, millis: Long): WakeTimer = synchronized(LOCK) {
        require(millis in 1000..3_600_000) { "Invalid extra time" }
        val timer = requireNotNull(repository.get(id)) { "Timer not found" }
        when (timer.status) {
            TimerStatus.RUNNING -> run(timer, (TimerMath.remaining(timer, nowElapsed()) + millis).coerceAtMost(TimerMath.MAX_DURATION_MILLIS))
            TimerStatus.FINISHED -> run(timer, millis)
            else -> timer.copy(remainingMillis = (timer.remainingMillis + millis).coerceAtMost(TimerMath.MAX_DURATION_MILLIS))
                .also { repository.update(it); changed() }
        }
    }

    /** Used by the "+1 min" action on a finished timer's notification. */
    fun restartWithExtra(id: Long, millis: Long) {
        synchronized(LOCK) {
            val timer = repository.get(id) ?: return
            run(timer, millis)
        }
    }

    fun delete(id: Long) = synchronized(LOCK) {
        cancelSchedule(id)
        WakeNotifications.cancelTimer(appContext, id)
        repository.delete(id)
        changed()
    }

    /** Called by the AlarmManager receiver. A second delivery for the same completion is ignored. */
    fun onFired(id: Long) = synchronized(LOCK) {
        val timer = repository.get(id) ?: return
        if (timer.status != TimerStatus.RUNNING) return
        val left = TimerMath.remaining(timer, nowElapsed())
        if (left > EARLY_TOLERANCE_MILLIS) {
            schedule(timer) // Delivered early (inexact mode); re-arm for the true end.
            return
        }
        finish(timer)
    }

    private fun finish(timer: WakeTimer) {
        val done = timer.copy(status = TimerStatus.FINISHED, remainingMillis = 0, endElapsedMillis = null, endTimeMillis = null)
        repository.update(done)
        WakeNotifications.showTimerDone(appContext, done)
        changed()
    }

    /** Re-registers running timers. After a reboot, remaining time is recovered from wall-clock time. */
    fun reconcileAll() = synchronized(LOCK) {
        val rebooted = bootCount() != AppStorage.state(appContext).getInt(KEY_BOOT_COUNT, bootCount())
        for (timer in repository.getAll()) {
            if (timer.status != TimerStatus.RUNNING) continue
            val left = TimerMath.remainingAfterReconcile(timer, nowElapsed(), nowWall(), rebooted)
            if (left <= 0) {
                finish(timer)
            } else {
                val updated = timer.copy(endElapsedMillis = nowElapsed() + left, endTimeMillis = nowWall() + left)
                repository.update(updated)
                schedule(updated)
                WakeNotifications.showTimerRunning(appContext, updated)
            }
        }
        rememberBoot()
        changed()
    }

    private fun bootCount(): Int = Settings.Global.getInt(appContext.contentResolver, Settings.Global.BOOT_COUNT, 0)

    private fun rememberBoot() = AppStorage.state(appContext).edit().putInt(KEY_BOOT_COUNT, bootCount()).apply()

    private fun pendingIntent(id: Long, flags: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            appContext,
            TIMER_REQUEST_CODE_BASE + id.toInt(),
            Intent(appContext, TimerReceiver::class.java)
                .setAction(TimerReceiver.ACTION_TIMER_DONE)
                .setData(Uri.parse("rcbwake://timer/$id"))
                .putExtra(TimerReceiver.EXTRA_TIMER_ID, id),
            flags or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun schedule(timer: WakeTimer) {
        val end = timer.endElapsedMillis ?: return
        val pi = pendingIntent(timer.id, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        try {
            if (exact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, end, pi)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, end, pi)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, end, pi)
        }
    }

    private fun cancelSchedule(id: Long) {
        pendingIntent(id, PendingIntent.FLAG_NO_CREATE)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun changed() = EngineEvents.timersChanged()

    companion object {
        private val LOCK = Any()
        private const val KEY_BOOT_COUNT = "timers_boot_count"
        private const val EARLY_TOLERANCE_MILLIS = 1_500L
        /** Timer request codes live above the alarm id range (see AlarmIntents.requestCode). */
        const val TIMER_REQUEST_CODE_BASE = Int.MAX_VALUE / 2 + 1
    }
}
