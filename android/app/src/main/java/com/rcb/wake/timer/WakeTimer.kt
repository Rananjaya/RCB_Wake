package com.rcb.wake.timer

enum class TimerStatus { IDLE, RUNNING, PAUSED, FINISHED }

data class WakeTimer(
    val id: Long,
    val label: String,
    val durationMillis: Long,
    /** Remaining time for IDLE/PAUSED timers. For RUNNING timers it is derived from [endElapsedMillis]. */
    val remainingMillis: Long,
    /** SystemClock.elapsedRealtime() at completion, for RUNNING timers. */
    val endElapsedMillis: Long?,
    /** Wall-clock completion time, used only to recover running timers after a reboot. */
    val endTimeMillis: Long?,
    val status: TimerStatus,
    val createdAt: Long,
)

/** Pure timer arithmetic, unit tested on the JVM. */
object TimerMath {
    const val MAX_DURATION_MILLIS = 100L * 60 * 60 * 1000 - 1000 // 99:59:59

    fun remaining(timer: WakeTimer, nowElapsed: Long): Long = when (timer.status) {
        TimerStatus.RUNNING -> ((timer.endElapsedMillis ?: nowElapsed) - nowElapsed).coerceAtLeast(0)
        TimerStatus.FINISHED -> 0
        else -> timer.remainingMillis
    }

    /**
     * Remaining time of a RUNNING timer during reconciliation. After a reboot the monotonic clock
     * restarted, so the wall-clock end time is the only reference that survived.
     */
    fun remainingAfterReconcile(timer: WakeTimer, nowElapsed: Long, nowWall: Long, rebooted: Boolean): Long =
        if (rebooted) ((timer.endTimeMillis ?: nowWall) - nowWall).coerceAtLeast(0)
        else remaining(timer, nowElapsed)

    fun validateDuration(millis: Long): Long {
        require(millis in 1000..MAX_DURATION_MILLIS) { "Timer duration must be between 1 second and 99:59:59" }
        return millis
    }
}
