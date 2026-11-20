package com.rcb.wake.notification

import android.content.Context
import android.content.res.Configuration
import com.rcb.wake.storage.SettingsStore
import java.util.Locale

/** Wraps a context so native strings follow the in-app language setting rather than the system. */
object Localized {
    fun context(base: Context): Context {
        val language = SettingsStore.load(base).language
        val config = Configuration(base.resources.configuration)
        config.setLocale(Locale.forLanguageTag(language))
        return base.createConfigurationContext(config)
    }
}
