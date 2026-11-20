package com.rcb.wake.alarm

import android.content.Context
import com.rcb.wake.storage.AppStorage
import org.json.JSONArray
import org.json.JSONObject

/**
 * The currently ringing session. Alarms that fire while another is ringing join the same session,
 * so there is only ever one audio stream and one ringing screen.
 *
 * Persisted so the session survives process death; the ringing service restores it on restart.
 */
data class AlarmSession(
    val alarmIds: List<Long>,
    val startedAt: Long,
    val challengeRequired: Boolean,
    val challengeDifficulty: String,
    val challengeQuestionCount: Int,
) {
    fun withAlarm(alarm: Alarm): AlarmSession {
        if (alarm.id in alarmIds) return this
        val required = challengeRequired || alarm.challengeEnabled
        val difficulty = when {
            !alarm.challengeEnabled -> challengeDifficulty
            !challengeRequired -> alarm.challengeDifficulty
            else -> harder(challengeDifficulty, alarm.challengeDifficulty)
        }
        val count = if (alarm.challengeEnabled) maxOf(challengeQuestionCount, alarm.challengeQuestionCount) else challengeQuestionCount
        return copy(alarmIds = alarmIds + alarm.id, challengeRequired = required, challengeDifficulty = difficulty, challengeQuestionCount = count)
    }

    companion object {
        fun start(alarm: Alarm, now: Long) = AlarmSession(
            alarmIds = listOf(alarm.id),
            startedAt = now,
            challengeRequired = alarm.challengeEnabled,
            challengeDifficulty = alarm.challengeDifficulty,
            challengeQuestionCount = if (alarm.challengeEnabled) alarm.challengeQuestionCount else 0,
        )

        private val order = listOf(ChallengeDifficulty.EASY, ChallengeDifficulty.MEDIUM, ChallengeDifficulty.HARD)
        fun harder(a: String, b: String) = if (order.indexOf(a) >= order.indexOf(b)) a else b
    }
}

object AlarmSessionStore {
    private const val KEY = "active_session"
    /** Written by the JS challenge screen via setValue("challenge_progress"); cleared with the session. */
    const val CHALLENGE_PROGRESS_KEY = "js.challenge_progress"

    fun load(context: Context): AlarmSession? {
        val raw = AppStorage.state(context).getString(KEY, null) ?: return null
        return runCatching {
            val o = JSONObject(raw)
            val ids = o.getJSONArray("alarmIds")
            AlarmSession(
                alarmIds = List(ids.length()) { ids.getLong(it) },
                startedAt = o.getLong("startedAt"),
                challengeRequired = o.getBoolean("challengeRequired"),
                challengeDifficulty = o.getString("challengeDifficulty"),
                challengeQuestionCount = o.getInt("challengeQuestionCount"),
            )
        }.getOrNull()
    }

    fun save(context: Context, session: AlarmSession) {
        val o = JSONObject()
            .put("alarmIds", JSONArray(session.alarmIds))
            .put("startedAt", session.startedAt)
            .put("challengeRequired", session.challengeRequired)
            .put("challengeDifficulty", session.challengeDifficulty)
            .put("challengeQuestionCount", session.challengeQuestionCount)
        AppStorage.state(context).edit().putString(KEY, o.toString()).commit()
    }

    fun clear(context: Context) {
        AppStorage.state(context).edit().remove(KEY).remove(CHALLENGE_PROGRESS_KEY).commit()
    }
}
