package com.rcb.wake.audio

import android.content.ContentResolver
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import com.rcb.wake.R

/**
 * Sound URIs stored with alarms:
 *  - `builtin:<key>`  one of the bundled tones in res/raw
 *  - `system:default` the device's default alarm sound
 *  - `content://…`    a user-selected file obtained through the Storage Access Framework
 */
object AlarmSounds {
    data class BuiltIn(val key: String, val name: String, val resId: Int)

    val BUILT_IN = listOf(
        BuiltIn("dawn", "Dawn", R.raw.tone_dawn),
        BuiltIn("chime", "Chime", R.raw.tone_chime),
        BuiltIn("pulse", "Pulse", R.raw.tone_pulse),
        BuiltIn("classic", "Classic", R.raw.tone_classic),
    )

    const val DEFAULT_URI = "builtin:dawn"
    const val DEFAULT_NAME = "Dawn"
    const val SYSTEM_DEFAULT_URI = "system:default"

    fun isSupportedUri(uri: String) =
        uri == SYSTEM_DEFAULT_URI || uri.startsWith("content://") ||
            (uri.startsWith("builtin:") && BUILT_IN.any { "builtin:${it.key}" == uri })

    fun builtInUri(context: Context, resId: Int): Uri =
        Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(context.packageName)
            .appendPath(resId.toString())
            .build()

    /** Playable URI for [soundUri], or null if it cannot be resolved. */
    fun resolve(context: Context, soundUri: String): Uri? = when {
        soundUri.startsWith("builtin:") ->
            BUILT_IN.firstOrNull { "builtin:${it.key}" == soundUri }?.let { builtInUri(context, it.resId) }
        soundUri == SYSTEM_DEFAULT_URI ->
            RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
                ?: Settings.System.DEFAULT_ALARM_ALERT_URI
        soundUri.startsWith("content://") -> Uri.parse(soundUri)
        else -> null
    }

    /** Ordered fallbacks: the requested sound, then the bundled default, then the system alarm. */
    fun candidates(context: Context, soundUri: String): List<Uri> =
        listOfNotNull(
            resolve(context, soundUri),
            resolve(context, DEFAULT_URI),
            resolve(context, SYSTEM_DEFAULT_URI),
        ).distinct()
}
