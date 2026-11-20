package com.rcb.wake.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import com.rcb.wake.alarm.Alarm
import com.rcb.wake.alarm.AlarmController
import com.rcb.wake.alarm.AlarmSession
import com.rcb.wake.alarm.AlarmSessionStore
import com.rcb.wake.audio.AlarmPlayer
import com.rcb.wake.audio.AlarmVibrator
import com.rcb.wake.module.EngineEvents
import com.rcb.wake.notification.WakeNotifications

/**
 * Foreground service that owns the single active ringing session: audio, vibration,
 * the ringing notification (with full-screen intent) and the wake lock.
 *
 * Runs without React Native. The session is persisted, so if the process is killed while ringing
 * the system restarts this sticky service and ringing resumes.
 */
class RingingService : Service() {
    private lateinit var controller: AlarmController
    private lateinit var player: AlarmPlayer
    private lateinit var vibrator: AlarmVibrator
    private val handler = Handler(Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null
    private var session: AlarmSession? = null

    private val autoSnooze = Runnable {
        Log.i(TAG, "Ringing timed out; snoozing")
        snooze()
    }

    override fun onCreate() {
        super.onCreate()
        controller = AlarmController(this)
        player = AlarmPlayer(this)
        vibrator = AlarmVibrator(this)
        WakeNotifications.ensureChannels(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RING -> ring(intent.getLongExtra(EXTRA_ALARM_ID, -1))
            ACTION_SNOOZE -> snooze()
            ACTION_DISMISS -> dismiss(intent.getStringExtra(EXTRA_METHOD) ?: METHOD_NORMAL)
            else -> restore()
        }
        return START_STICKY
    }

    /**
     * Calls to startForegroundService() must be answered with startForeground() even when there is
     * nothing to ring (stale trigger), otherwise the system crashes the app.
     */
    private fun satisfyForegroundContractAndStop() {
        startForegroundCompat(WakeNotifications.buildRinging(this, AlarmSession(emptyList(), 0, false, "easy", 0), emptyList()))
        stopSelfCleanly()
    }

    private fun alarmsOf(s: AlarmSession): List<Alarm> = s.alarmIds.mapNotNull { controller.get(it) }

    private fun ring(alarmId: Long) {
        val alarm = controller.get(alarmId)
        val current = session ?: AlarmSessionStore.load(this)
        if (alarm == null) {
            if (current == null) satisfyForegroundContractAndStop()
            return
        }
        if (current != null && alarm.id in current.alarmIds && session != null) {
            Log.i(TAG, "Alarm already ringing; ignoring duplicate trigger")
            return
        }
        val next = current?.withAlarm(alarm) ?: AlarmSession.start(alarm, System.currentTimeMillis())
        val isNewSession = session == null
        session = next
        AlarmSessionStore.save(this, next)
        startForegroundCompat(WakeNotifications.buildRinging(this, next, alarmsOf(next)))
        if (isNewSession) startOutput(alarm)
        EngineEvents.emit(EngineEvents.SESSION_STARTED)
    }

    /** Resumes a persisted session after process death, or stops if there is nothing to ring. */
    private fun restore() {
        val saved = AlarmSessionStore.load(this)
        val first = saved?.let { alarmsOf(it).firstOrNull() }
        if (saved == null || first == null) {
            AlarmSessionStore.clear(this)
            satisfyForegroundContractAndStop()
            return
        }
        Log.i(TAG, "Restoring ringing session after restart")
        session = saved
        startForegroundCompat(WakeNotifications.buildRinging(this, saved, alarmsOf(saved)))
        startOutput(first)
    }

    private fun startOutput(alarm: Alarm) {
        acquireWakeLock()
        player.start(alarm.soundUri, alarm.gradualVolumeEnabled)
        if (alarm.vibrationEnabled || !player.isPlaying) vibrator.start()
        handler.removeCallbacks(autoSnooze)
        handler.postDelayed(autoSnooze, AUTO_SNOOZE_MILLIS)
    }

    private fun snooze() {
        val s = session ?: AlarmSessionStore.load(this) ?: return stopSelfCleanly()
        controller.snooze(s.alarmIds)
        endSession()
    }

    private fun dismiss(method: String) {
        val s = session ?: AlarmSessionStore.load(this) ?: return stopSelfCleanly()
        if (s.challengeRequired && method == METHOD_NORMAL) {
            Log.i(TAG, "Dismiss refused: challenge required")
            return
        }
        controller.dismiss(s.alarmIds)
        endSession()
    }

    private fun endSession() {
        handler.removeCallbacks(autoSnooze)
        player.stop()
        vibrator.stop()
        session = null
        AlarmSessionStore.clear(this)
        EngineEvents.emit(EngineEvents.SESSION_ENDED)
        stopSelfCleanly()
    }

    private fun stopSelfCleanly() {
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundCompat(notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                WakeNotifications.RINGING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(WakeNotifications.RINGING_NOTIFICATION_ID, notification)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RcbWake:ringing")
            .apply { acquire(AUTO_SNOOZE_MILLIS + 60_000L) }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoSnooze)
        player.stop()
        vibrator.stop()
        releaseWakeLock()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "RcbWakeRinging"
        const val ACTION_RING = "com.rcb.wake.action.RING"
        const val ACTION_SNOOZE = "com.rcb.wake.action.SNOOZE"
        const val ACTION_DISMISS = "com.rcb.wake.action.DISMISS"
        const val EXTRA_ALARM_ID = "com.rcb.wake.extra.ALARM_ID"
        const val EXTRA_METHOD = "com.rcb.wake.extra.METHOD"

        /** Plain dismissal; refused when the session requires a challenge. */
        const val METHOD_NORMAL = "normal"
        const val METHOD_CHALLENGE = "challenge"
        const val METHOD_EMERGENCY = "emergency"

        /** Ringing stops and the alarm snoozes automatically after this long. */
        const val AUTO_SNOOZE_MILLIS = 10 * 60_000L

        fun ring(context: Context, alarmId: Long) {
            val intent = Intent(context, RingingService::class.java).setAction(ACTION_RING).putExtra(EXTRA_ALARM_ID, alarmId)
            context.startForegroundService(intent)
        }

        fun snoozeIntent(context: Context): Intent = Intent(context, RingingService::class.java).setAction(ACTION_SNOOZE)

        fun dismissIntent(context: Context, method: String): Intent =
            Intent(context, RingingService::class.java).setAction(ACTION_DISMISS).putExtra(EXTRA_METHOD, method)
    }
}
