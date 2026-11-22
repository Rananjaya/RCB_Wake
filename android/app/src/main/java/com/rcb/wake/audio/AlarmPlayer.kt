package com.rcb.wake.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Looping alarm playback on the alarm audio stream.
 *
 * - Falls back through [AlarmSounds.candidates] if a sound cannot be opened (e.g. a deleted
 *   custom file, or a content URI that is unreadable before the device is unlocked).
 * - Requests audio focus but keeps ringing through other apps' sounds (notification sounds briefly
 *   take focus and would otherwise silence the alarm). It pauses only during a phone call and
 *   resumes when the call ends, even if the focus-gain callback never arrives.
 * - Optionally ramps volume up over [RAMP_MILLIS].
 *
 * Must be used from the main thread. [stop] is idempotent and always releases everything.
 */
class AlarmPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())

    private var player: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private var rampStartedAt = 0L
    private var gradual = false
    private var pausedForFocus = false

    val isPlaying: Boolean get() = player != null

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> if (inCall()) pauseForCall()
            AudioManager.AUDIOFOCUS_GAIN -> resumeAfterCall()
        }
    }

    private fun inCall() = audioManager.mode in setOf(
        AudioManager.MODE_IN_CALL,
        AudioManager.MODE_IN_COMMUNICATION,
        AudioManager.MODE_RINGTONE,
    )

    private fun pauseForCall() {
        val p = player ?: return
        if (p.isPlaying) p.pause()
        pausedForFocus = true
        handler.removeCallbacks(callWatch)
        handler.postDelayed(callWatch, CALL_POLL_MILLIS)
    }

    private fun resumeAfterCall() {
        if (!pausedForFocus) return
        pausedForFocus = false
        handler.removeCallbacks(callWatch)
        player?.let { if (!it.isPlaying) it.start() }
        requestFocus()
    }

    /** Some devices never send AUDIOFOCUS_GAIN after a call; resume when the call has ended. */
    private val callWatch = object : Runnable {
        override fun run() {
            if (!pausedForFocus) return
            if (inCall()) handler.postDelayed(this, CALL_POLL_MILLIS) else resumeAfterCall()
        }
    }

    private val rampTick = object : Runnable {
        override fun run() {
            val p = player ?: return
            val fraction = ((System.currentTimeMillis() - rampStartedAt).toFloat() / RAMP_MILLIS).coerceIn(0f, 1f)
            // Quadratic curve: perceived loudness grows roughly evenly.
            val volume = (MIN_VOLUME + (1f - MIN_VOLUME) * fraction * fraction)
            p.setVolume(volume, volume)
            if (fraction < 1f) handler.postDelayed(this, RAMP_STEP_MILLIS)
        }
    }

    /** Starts looping playback. Returns false if no sound source could be opened. */
    fun start(soundUri: String, gradualVolume: Boolean): Boolean {
        stop()
        gradual = gradualVolume
        requestFocus()
        for (uri in AlarmSounds.candidates(appContext, soundUri)) {
            val p = open(uri) ?: continue
            player = p
            if (gradual) {
                rampStartedAt = System.currentTimeMillis()
                p.setVolume(MIN_VOLUME, MIN_VOLUME)
                handler.post(rampTick)
            }
            p.start()
            return true
        }
        Log.w(TAG, "No playable alarm sound; relying on vibration")
        abandonFocus()
        return false
    }

    private fun open(uri: Uri): MediaPlayer? {
        val p = MediaPlayer()
        return try {
            p.setAudioAttributes(attributes)
            p.setDataSource(appContext, uri)
            p.isLooping = true
            p.setOnErrorListener { _, what, extra ->
                Log.w(TAG, "Playback error $what/$extra, recovering with fallback sound")
                handler.post { recoverWithDefault() }
                true
            }
            p.prepare()
            p
        } catch (e: Exception) {
            Log.w(TAG, "Cannot open alarm sound", e)
            p.release()
            null
        }
    }

    private fun recoverWithDefault() {
        if (player == null) return
        releasePlayer()
        val p = open(AlarmSounds.resolve(appContext, AlarmSounds.DEFAULT_URI) ?: return) ?: return
        player = p
        p.start()
    }

    private fun requestFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(focusListener, handler)
            .build()
        focusRequest = request
        audioManager.requestAudioFocus(request)
    }

    private fun abandonFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    private fun releasePlayer() {
        handler.removeCallbacks(rampTick)
        player?.let {
            runCatching { it.stop() }
            it.release()
        }
        player = null
    }

    fun stop() {
        handler.removeCallbacks(callWatch)
        releasePlayer()
        abandonFocus()
        pausedForFocus = false
    }

    private companion object {
        const val TAG = "RcbWakeAudio"
        const val RAMP_MILLIS = 30_000L
        const val RAMP_STEP_MILLIS = 500L
        const val MIN_VOLUME = 0.05f
        const val CALL_POLL_MILLIS = 2_000L
    }
}
