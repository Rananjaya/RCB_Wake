package com.rcb.wake.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import com.rcb.wake.module.EngineEvents
import com.rcb.wake.notification.WakeNotifications
import com.rcb.wake.receiver.TimerReceiver
import com.rcb.wake.storage.AppStorage
import org.json.JSONObject

enum class PomodoroPhase { FOCUS, SHORT_BREAK, LONG_BREAK }

enum class PomodoroStatus { IDLE, RUNNING, PAUSED }

data class PomodoroConfig(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val roundsBeforeLongBreak: Int = 4,
    val autoStartNext: Boolean = true,
) {
    fun validate(): PomodoroConfig {
        require(focusMinutes in 1..180) { "focus 1-180" }
        require(shortBreakMinutes in 1..60) { "short break 1-60" }
        require(longBreakMinutes in 1..120) { "long break 1-120" }
        require(roundsBeforeLongBreak in 1..12) { "rounds 1-12" }
        return this
    }

    fun minutesFor(phase: PomodoroPhase) = when (phase) {
        PomodoroPhase.FOCUS -> focusMinutes
        PomodoroPhase.SHORT_BREAK -> shortBreakMinutes
        PomodoroPhase.LONG_BREAK -> longBreakMinutes
    }
}

data class PomodoroState(
    val config: PomodoroConfig = PomodoroConfig(),
    val status: PomodoroStatus = PomodoroStatus.IDLE,
    val phase: PomodoroPhase = PomodoroPhase.FOCUS,
    /** Focus sessions completed in the current cycle (resets after a long break). */
    val completedFocus: Int = 0,
    /** Focus sessions completed since the user last reset. */
    val totalFocus: Int = 0,
    val remainingMillis: Long = config.focusMinutes * 60_000L,
    val endElapsedMillis: Long? = null,
    val endTimeMillis: Long? = null,
)

/** Pure phase transitions, unit tested on the JVM. */
object PomodoroMath {
    /** The phase after [state.phase] finishes, and the updated focus counters. */
    fun advance(state: PomodoroState): PomodoroState {
        val c = state.config
        return if (state.phase == PomodoroPhase.FOCUS) {
            val done = state.completedFocus + 1
            val next = if (done >= c.roundsBeforeLongBreak) PomodoroPhase.LONG_BREAK else PomodoroPhase.SHORT_BREAK
            state.copy(phase = next, completedFocus = done, totalFocus = state.totalFocus + 1, remainingMillis = c.minutesFor(next) * 60_000L)
        } else {
            val done = if (state.phase == PomodoroPhase.LONG_BREAK) 0 else state.completedFocus
            state.copy(phase = PomodoroPhase.FOCUS, completedFocus = done, remainingMillis = c.focusMinutes * 60_000L)
        }
    }

    fun remaining(state: PomodoroState, nowElapsed: Long): Long =
        if (state.status == PomodoroStatus.RUNNING) ((state.endElapsedMillis ?: nowElapsed) - nowElapsed).coerceAtLeast(0)
        else state.remainingMillis
}

/**
 * Pomodoro focus/break cycle. Like countdown timers it runs on the monotonic clock with
 * AlarmManager, so phase changes happen with the app closed, and after a reboot the
 * remaining time is recovered from wall-clock time.
 */
class PomodoroController(context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    private fun nowElapsed() = SystemClock.elapsedRealtime()
    private fun nowWall() = System.currentTimeMillis()

    fun get(): PomodoroState = load()

    fun saveConfig(config: PomodoroConfig): PomodoroState = synchronized(LOCK) {
        val s = load()
        val updated = if (s.status == PomodoroStatus.IDLE) {
            s.copy(config = config.validate(), remainingMillis = config.minutesFor(s.phase) * 60_000L)
        } else {
            s.copy(config = config.validate())
        }
        store(updated)
        updated
    }

    fun start(): PomodoroState = synchronized(LOCK) { run(load()) }

    private fun run(s: PomodoroState): PomodoroState {
        val remaining = if (s.remainingMillis > 0) s.remainingMillis else s.config.minutesFor(s.phase) * 60_000L
        val running = s.copy(
            status = PomodoroStatus.RUNNING,
            remainingMillis = remaining,
            endElapsedMillis = nowElapsed() + remaining,
            endTimeMillis = nowWall() + remaining,
        )
        store(running)
        schedule(running)
        WakeNotifications.showPomodoroRunning(appContext, running)
        return running
    }

    fun pause(): PomodoroState = synchronized(LOCK) {
        val s = load()
        if (s.status != PomodoroStatus.RUNNING) return s
        val paused = s.copy(
            status = PomodoroStatus.PAUSED,
            remainingMillis = PomodoroMath.remaining(s, nowElapsed()),
            endElapsedMillis = null,
            endTimeMillis = null,
        )
        cancelSchedule()
        WakeNotifications.cancelPomodoro(appContext)
        store(paused)
        paused
    }

    /** Ends the current phase now and moves to the next one (running if it was running). */
    fun skip(): PomodoroState = synchronized(LOCK) {
        val s = load()
        cancelSchedule()
        val next = PomodoroMath.advance(s).copy(status = PomodoroStatus.IDLE, endElapsedMillis = null, endTimeMillis = null)
        if (s.status == PomodoroStatus.RUNNING) run(next) else next.also { store(it); WakeNotifications.cancelPomodoro(appContext) }
    }

    fun reset(): PomodoroState = synchronized(LOCK) {
        cancelSchedule()
        WakeNotifications.cancelPomodoro(appContext)
        val config = load().config
        val fresh = PomodoroState(config = config, remainingMillis = config.focusMinutes * 60_000L)
        store(fresh)
        fresh
    }

    /** AlarmManager delivery at the end of a phase. Late or duplicate deliveries are ignored. */
    fun onPhaseEnd() {
        synchronized(LOCK) {
            val s = load()
            if (s.status != PomodoroStatus.RUNNING) return
            if (PomodoroMath.remaining(s, nowElapsed()) > EARLY_TOLERANCE_MILLIS) {
                schedule(s)
                return
            }
            val next = PomodoroMath.advance(s).copy(endElapsedMillis = null, endTimeMillis = null)
            WakeNotifications.showPomodoroPhaseDone(appContext, finished = s.phase, next = next.phase)
            if (s.config.autoStartNext) {
                run(next)
            } else {
                store(next.copy(status = PomodoroStatus.IDLE))
                WakeNotifications.cancelPomodoro(appContext)
            }
        }
    }

    fun reconcile() {
        synchronized(LOCK) {
            val s = load()
            if (s.status != PomodoroStatus.RUNNING) return
            val rebooted = bootCount() != AppStorage.state(appContext).getInt(KEY_BOOT, bootCount())
            val left = if (rebooted) ((s.endTimeMillis ?: nowWall()) - nowWall()).coerceAtLeast(0) else PomodoroMath.remaining(s, nowElapsed())
            if (left <= 0) {
                store(s.copy(endElapsedMillis = nowElapsed()))
                onPhaseEnd()
            } else {
                run(s.copy(remainingMillis = left))
            }
        }
    }

    private fun bootCount() = Settings.Global.getInt(appContext.contentResolver, Settings.Global.BOOT_COUNT, 0)

    private fun pendingIntent(flags: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE,
            Intent(appContext, TimerReceiver::class.java).setAction(TimerReceiver.ACTION_POMODORO_PHASE_END),
            flags or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun schedule(s: PomodoroState) {
        val end = s.endElapsedMillis ?: return
        val pi = pendingIntent(PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        try {
            if (exact) alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, end, pi)
            else alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, end, pi)
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, end, pi)
        }
    }

    private fun cancelSchedule() {
        pendingIntent(PendingIntent.FLAG_NO_CREATE)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun load(): PomodoroState {
        val raw = AppStorage.state(appContext).getString(KEY, null) ?: return PomodoroState()
        return runCatching {
            val o = JSONObject(raw)
            val c = o.getJSONObject("config")
            PomodoroState(
                config = PomodoroConfig(
                    focusMinutes = c.getInt("focus"),
                    shortBreakMinutes = c.getInt("short"),
                    longBreakMinutes = c.getInt("long"),
                    roundsBeforeLongBreak = c.getInt("rounds"),
                    autoStartNext = c.getBoolean("auto"),
                ),
                status = PomodoroStatus.valueOf(o.getString("status")),
                phase = PomodoroPhase.valueOf(o.getString("phase")),
                completedFocus = o.getInt("completed"),
                totalFocus = o.getInt("total"),
                remainingMillis = o.getLong("remaining"),
                endElapsedMillis = if (o.isNull("endElapsed")) null else o.getLong("endElapsed"),
                endTimeMillis = if (o.isNull("endTime")) null else o.getLong("endTime"),
            )
        }.getOrDefault(PomodoroState())
    }

    private fun store(s: PomodoroState) {
        val c = JSONObject()
            .put("focus", s.config.focusMinutes)
            .put("short", s.config.shortBreakMinutes)
            .put("long", s.config.longBreakMinutes)
            .put("rounds", s.config.roundsBeforeLongBreak)
            .put("auto", s.config.autoStartNext)
        val o = JSONObject()
            .put("config", c)
            .put("status", s.status.name)
            .put("phase", s.phase.name)
            .put("completed", s.completedFocus)
            .put("total", s.totalFocus)
            .put("remaining", s.remainingMillis)
            .put("endElapsed", s.endElapsedMillis ?: JSONObject.NULL)
            .put("endTime", s.endTimeMillis ?: JSONObject.NULL)
        AppStorage.state(appContext).edit().putString(KEY, o.toString()).putInt(KEY_BOOT, bootCount()).commit()
        EngineEvents.emit(EngineEvents.POMODORO_CHANGED)
    }

    companion object {
        private val LOCK = Any()
        private const val KEY = "pomodoro"
        private const val KEY_BOOT = "pomodoro_boot_count"
        private const val EARLY_TOLERANCE_MILLIS = 1_500L
        /** Fixed request code just below the timer range. */
        const val REQUEST_CODE = Int.MAX_VALUE / 2
    }
}
