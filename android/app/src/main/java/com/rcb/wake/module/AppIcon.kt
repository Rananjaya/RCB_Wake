package com.rcb.wake.module

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Selectable launcher icons. Each variant is an <activity-alias> of MainActivity with its own
 * icon; switching enables the chosen alias and disables the rest. Some launchers take a moment to
 * refresh and may move the icon on the home screen.
 */
object AppIcon {
    val VARIANTS = listOf("default", "blue", "teal", "green", "orange", "rose", "midnight", "cream")

    private fun component(context: Context, variant: String) =
        ComponentName(context, "com.rcb.wake.Launcher" + variant.replaceFirstChar { it.uppercase() })

    fun current(context: Context): String {
        val pm = context.packageManager
        return VARIANTS.firstOrNull { v ->
            when (pm.getComponentEnabledSetting(component(context, v))) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> v == "default"
                else -> false
            }
        } ?: "default"
    }

    fun set(context: Context, variant: String) {
        require(variant in VARIANTS) { "Unknown icon" }
        if (variant == current(context)) return
        val pm = context.packageManager
        // Enable the new entry first so the app always has a launcher icon.
        pm.setComponentEnabledSetting(component(context, variant), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        VARIANTS.filter { it != variant }.forEach {
            pm.setComponentEnabledSetting(component(context, it), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
    }
}
