package com.rcb.wake.module

import java.util.concurrent.CopyOnWriteArraySet

/**
 * In-process change notifications from the native engine. The React Native module subscribes
 * while JavaScript is running; the engine never depends on anyone listening.
 */
object EngineEvents {
    const val ALARMS_CHANGED = "alarmsChanged"
    const val TIMERS_CHANGED = "timersChanged"
    const val SESSION_STARTED = "sessionStarted"
    const val SESSION_ENDED = "sessionEnded"
    const val SETTINGS_CHANGED = "settingsChanged"

    fun interface Listener {
        fun onEvent(type: String)
    }

    private val listeners = CopyOnWriteArraySet<Listener>()

    fun addListener(listener: Listener) = listeners.add(listener)

    fun removeListener(listener: Listener) = listeners.remove(listener)

    fun emit(type: String) = listeners.forEach { runCatching { it.onEvent(type) } }

    fun alarmsChanged() = emit(ALARMS_CHANGED)

    fun timersChanged() = emit(TIMERS_CHANGED)
}
