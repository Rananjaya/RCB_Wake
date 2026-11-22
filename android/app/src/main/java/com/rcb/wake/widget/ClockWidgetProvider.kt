package com.rcb.wake.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.SizeF
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.rcb.wake.MainActivity
import com.rcb.wake.R
import com.rcb.wake.alarm.AlarmController
import com.rcb.wake.notification.Localized
import com.rcb.wake.storage.AppStorage
import com.rcb.wake.storage.SettingsStore
import com.rcb.wake.weather.DeviceLocation
import com.rcb.wake.weather.WeatherCondition
import com.rcb.wake.weather.WeatherCodes
import com.rcb.wake.weather.WeatherRepository
import com.rcb.wake.weather.WeatherSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Resizable home-screen clock. Time and date are TextClocks, so the system keeps them current
 * without waking the app. The provider only re-renders for next-alarm, settings and weather changes.
 */
class ClockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        render(context, manager, ids)
        GlanceWidgetProvider.updateAll(context)
        com.rcb.wake.reminders.UpcomingReminders.reschedule(context)
        scheduleWeatherRefresh(context)
        refreshWeatherIfStale(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH_WEATHER) refreshWeatherIfStale(context) else super.onReceive(context, intent)
    }

    override fun onDisabled(context: Context) = scheduleWeatherRefresh(context)

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        render(context, manager, intArrayOf(id))
    }

    private fun refreshWeatherIfStale(context: Context) {
        if (!SettingsStore.load(context).weatherEnabled || !WeatherRepository.isStale(context)) return
        val pending = goAsync()
        Thread {
            try {
                if (WeatherRepository.refresh(context) != null) updateAll(context)
            } finally {
                pending.finish()
            }
        }.start()
    }

    /**
     * Layouts and the optional views each one contains. RemoteViews fails to apply if a setter
     * targets a view the layout does not have, so every setter is guarded by these flags.
     */
    enum class Layout(
        val resId: Int,
        val splitClock: Boolean,
        val date: Boolean,
        val weather: Boolean,
        val alarm: Boolean,
        val event: Boolean,
    ) {
        SMALL(R.layout.widget_small, splitClock = true, date = true, weather = false, alarm = false, event = false),
        BAR(R.layout.widget_bar, splitClock = false, date = false, weather = true, alarm = false, event = false),
        MEDIUM(R.layout.widget_medium, splitClock = false, date = true, weather = true, alarm = false, event = true),
        WIDE(R.layout.widget_wide, splitClock = false, date = true, weather = true, alarm = true, event = true),
        LARGE(R.layout.widget_large, splitClock = false, date = true, weather = true, alarm = true, event = true),
        ;

        val primaryText get() = if (splitClock) listOf(R.id.widget_hour, R.id.widget_minute) else listOf(R.id.widget_time)
        val secondaryText get() = buildList {
            if (splitClock) add(R.id.widget_dot)
            if (date) add(R.id.widget_date)
            if (weather) add(R.id.widget_weather_temp)
            if (alarm) add(R.id.widget_alarm)
            if (event) add(R.id.widget_event)
        }
    }

    companion object {
        const val ACTION_REFRESH_WEATHER = "com.rcb.wake.action.REFRESH_WEATHER"
        private const val SCHEDULED_KEY = "weather_refresh_scheduled_minutes"

        /**
         * Inexact, non-waking repeating alarm at the user's weather interval while weather is on and
         * a widget exists. It never wakes the device; a refresh due while asleep runs on next wake.
         */
        fun scheduleWeatherRefresh(context: Context) {
            val settings = SettingsStore.load(context)
            val hasWidget = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, ClockWidgetProvider::class.java)).isNotEmpty() ||
                GlanceWidgetProvider.ids(context).isNotEmpty()
            val minutes = if (settings.weatherEnabled && hasWidget) settings.weatherRefreshMinutes else 0
            val state = AppStorage.state(context)
            val intent = Intent(context, ClockWidgetProvider::class.java).setAction(ACTION_REFRESH_WEATHER)
            val existing = PendingIntent.getBroadcast(context, 901, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            // Re-arming would restart the countdown, so leave an alarm with the same interval alone.
            if (existing != null && state.getInt(SCHEDULED_KEY, 0) == minutes) return
            val alarms = context.getSystemService(AlarmManager::class.java)
            existing?.let { alarms.cancel(it); it.cancel() }
            state.edit().putInt(SCHEDULED_KEY, minutes).apply()
            if (minutes == 0) return
            val interval = minutes * 60_000L
            val pending = PendingIntent.getBroadcast(context, 901, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            alarms.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + interval, interval, pending)
        }

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ClockWidgetProvider::class.java))
            if (ids.isNotEmpty()) render(context, manager, ids)
            // Both widgets show the same next alarm, weather and settings.
            GlanceWidgetProvider.updateAll(context)
        }

        /** Picks a layout for widget dimensions in dp (used before Android 12). */
        fun layoutFor(widthDp: Int, heightDp: Int): Layout = when {
            widthDp < 150 -> Layout.SMALL
            heightDp < 100 -> Layout.BAR
            widthDp < 250 -> Layout.MEDIUM
            heightDp < 200 -> Layout.WIDE
            else -> Layout.LARGE
        }

        private fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val content = WidgetContent.load(context)
            CalendarEvents.scheduleRefresh(context, content.event)
            for (id in ids) {
                val options = manager.getAppWidgetOptions(id)
                val sizes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    @Suppress("DEPRECATION")
                    options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
                } else {
                    null
                }
                // Launchers that report their exact sizes (Pixel) get a size map and switch layouts
                // themselves. Others (Xiaomi HyperOS, many third-party launchers) leave the sizes out,
                // and the size map then falls back to the smallest entry, so pick from min/max instead.
                val views = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !sizes.isNullOrEmpty()) {
                    RemoteViews(SIZE_MAP.associate { (w, h, layout) -> SizeF(w, h) to build(context, layout, content, w, h) })
                } else {
                    // Portrait uses min width and max height; take the smaller pair so nothing clips.
                    val w = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
                    val h = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
                    build(context, layoutFor(w, h), content, w.toFloat(), h.toFloat())
                }
                manager.updateAppWidget(id, views)
            }
        }

        /**
         * Size-map entries (minimum dp size → layout). The launcher shows the largest entry that
         * fits, so several entries per layout let the clock grow as the widget is enlarged.
         */
        private val SIZE_MAP = listOf(
            Triple(100f, 100f, Layout.SMALL),
            Triple(130f, 150f, Layout.SMALL),
            Triple(250f, 50f, Layout.BAR),
            Triple(320f, 70f, Layout.BAR),
            Triple(170f, 110f, Layout.MEDIUM),
            Triple(210f, 150f, Layout.MEDIUM),
            Triple(250f, 110f, Layout.WIDE),
            Triple(300f, 140f, Layout.WIDE),
            Triple(360f, 170f, Layout.WIDE),
            Triple(250f, 200f, Layout.LARGE),
            Triple(300f, 250f, Layout.LARGE),
            Triple(350f, 300f, Layout.LARGE),
        )

        /**
         * Clock text size in dp that fits a widget of [w]×[h] dp next to the layout's other rows.
         * Computed rather than auto-sized so the clock wraps its text and the date stays right
         * under it. "10:58" is about 2.7 em wide; a line without font padding is about 1.2 em.
         */
        fun clockSizeDp(layout: Layout, w: Float, h: Float): Float {
            val (max, min, size) = when (layout) {
                Layout.SMALL -> Triple(48f, 16f, minOf((w - 16f) / 1.2f, (h - 16f - 40f) / 2f / 1.2f))
                Layout.BAR -> Triple(34f, 14f, minOf((w - 36f - 80f) / 2.7f, (h - 8f) / 1.25f))
                Layout.MEDIUM -> Triple(64f, 22f, minOf((w - 24f) / 2.7f, (h - 24f - 80f) / 1.2f))
                Layout.WIDE -> Triple(72f, 24f, minOf((w - 40f - 130f) / 2.7f, (h - 24f - 22f) / 1.2f))
                Layout.LARGE -> Triple(96f, 28f, minOf((w - 32f) / 2.7f, (h - 32f - 90f) / 1.2f))
            }
            return size.coerceIn(min, max)
        }

        private fun build(context: Context, layout: Layout, content: WidgetContent, w: Float, h: Float): RemoteViews {
            val views = RemoteViews(context.packageName, layout.resId)
            val clock = clockSizeDp(layout, w, h)
            if (layout.splitClock) {
                views.setTimeFormat(R.id.widget_hour, content.pattern("hh", "HH"))
                views.setTimeFormat(R.id.widget_minute, "mm" to "mm")
                views.setTextViewTextSize(R.id.widget_hour, TypedValue.COMPLEX_UNIT_DIP, clock)
                views.setTextViewTextSize(R.id.widget_minute, TypedValue.COMPLEX_UNIT_DIP, clock)
            } else {
                views.setTimeFormat(R.id.widget_time, content.pattern("h:mm", "HH:mm"))
                views.setTextViewTextSize(R.id.widget_time, TypedValue.COMPLEX_UNIT_DIP, clock)
            }
            if (layout.date) {
                views.setTimeFormat(R.id.widget_date, (if (layout == Layout.SMALL) "EEE d" else "MMM, EEEE d").let { it to it })
            }
            if (layout.alarm) {
                views.setViewVisibility(R.id.widget_alarm, if (content.alarmText != null) View.VISIBLE else View.GONE)
                content.alarmText?.let { views.setTextViewText(R.id.widget_alarm, it) }
            }
            if (layout.event) {
                views.setViewVisibility(R.id.widget_event, if (content.eventText != null) View.VISIBLE else View.GONE)
                content.eventText?.let { views.setTextViewText(R.id.widget_event, it) }
            }
            if (layout.weather) {
                val weather = content.weather
                views.setViewVisibility(R.id.widget_weather, if (weather != null) View.VISIBLE else View.GONE)
                if (weather != null) {
                    views.setImageViewResource(R.id.widget_weather_icon, weatherIcon(weather))
                    views.setContentDescription(R.id.widget_weather_icon, content.weatherDescription)
                    // Roomier layouts also name the place, e.g. "31°C · Kosgama".
                    val withPlace = layout == Layout.WIDE || layout == Layout.LARGE
                    views.setTextViewText(
                        R.id.widget_weather_temp,
                        content.placeName?.takeIf { withPlace }?.let { "${content.temperatureText} · $it" } ?: content.temperatureText,
                    )
                }
            }

            applyTextColor(context, views, layout, content)

            val open = PendingIntent.getActivity(
                context,
                900,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)
            return views
        }

        /** White by default (styles), Material You wallpaper tones on Android 12+, or the app accent. */
        private fun applyTextColor(context: Context, views: RemoteViews, layout: Layout, content: WidgetContent) {
            when {
                content.textColor == "material" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    // Resource ids resolve in the launcher, so colours follow wallpaper changes live.
                    layout.primaryText.forEach { views.setColorStateList(it, "setTextColor", android.R.color.system_accent1_100) }
                    layout.secondaryText.forEach { views.setColorStateList(it, "setTextColor", android.R.color.system_accent2_50) }
                }
                content.textColor == "accent" -> {
                    val color = Color.parseColor(content.accentHex)
                    layout.primaryText.forEach { views.setTextColor(it, color) }
                    layout.secondaryText.forEach { views.setTextColor(it, color) }
                }
                // White is set explicitly: launchers re-apply updates onto the existing views, so
                // colours from an earlier Material You or accent choice would otherwise remain.
                else -> {
                    layout.primaryText.forEach { views.setTextColor(it, WHITE) }
                    layout.secondaryText.forEach { views.setTextColor(it, WHITE_SECONDARY) }
                }
            }
        }

        const val WHITE = 0xFFFFFFFF.toInt()
        const val WHITE_SECONDARY = 0xE6FFFFFF.toInt()

        /** TextClock uses format12 or format24 according to the device's live 12/24-hour setting. */
        private fun RemoteViews.setTimeFormat(viewId: Int, patterns: Pair<String, String>) {
            setCharSequence(viewId, "setFormat12Hour", patterns.first)
            setCharSequence(viewId, "setFormat24Hour", patterns.second)
        }

        fun weatherIcon(w: WeatherSnapshot): Int = when (w.condition) {
            WeatherCondition.CLEAR -> if (w.isDay) R.drawable.ic_wx_clear_day else R.drawable.ic_wx_clear_night
            WeatherCondition.MOSTLY_CLEAR -> if (w.isDay) R.drawable.ic_wx_mostly_clear_day else R.drawable.ic_wx_mostly_clear_night
            WeatherCondition.PARTLY_CLOUDY -> if (w.isDay) R.drawable.ic_wx_partly_day else R.drawable.ic_wx_partly_night
            WeatherCondition.CLOUDY -> R.drawable.ic_wx_cloudy
            WeatherCondition.FOG -> R.drawable.ic_wx_fog
            WeatherCondition.DRIZZLE -> R.drawable.ic_wx_drizzle
            WeatherCondition.RAIN -> R.drawable.ic_wx_rain
            WeatherCondition.HEAVY_RAIN -> R.drawable.ic_wx_heavy_rain
            WeatherCondition.SHOWERS -> if (w.isDay) R.drawable.ic_wx_showers_day else R.drawable.ic_wx_showers_night
            WeatherCondition.SLEET -> R.drawable.ic_wx_sleet
            WeatherCondition.SNOW -> R.drawable.ic_wx_snow
            WeatherCondition.SNOW_SHOWERS -> if (w.isDay) R.drawable.ic_wx_snow_showers_day else R.drawable.ic_wx_snow_showers_night
            WeatherCondition.THUNDERSTORM -> if (w.isDay) R.drawable.ic_wx_thunder_day else R.drawable.ic_wx_thunder_night
        }
    }
}

/** Everything the widget shows besides the system-driven clock. */
data class WidgetContent(
    val is24Hour: Boolean,
    /** `system`, `12` or `24` from settings. */
    val timeFormat: String,
    val alarmText: String?,
    /** When the next alarm rings (epoch millis), or null. */
    val alarmAt: Long? = null,
    val weather: WeatherSnapshot?,
    val temperatureText: String,
    val weatherDescription: String,
    val event: CalendarEvent?,
    val eventText: String?,
    /** Village / city the weather is for; null when unknown. */
    val placeName: String? = null,
    /** `white`, `material` or `accent`. */
    val textColor: String = "white",
    /** Bright accent for text on the wallpaper. */
    val accentHex: String = "#A855F7",
) {
    /** 12/24-hour TextClock patterns: both when following the device, otherwise the forced one. */
    fun pattern(twelve: String, twentyFour: String): Pair<String, String> = when (timeFormat) {
        "12" -> twelve to twelve
        "24" -> twentyFour to twentyFour
        else -> twelve to twentyFour
    }

    companion object {
        fun load(context: Context): WidgetContent {
            val settings = SettingsStore.load(context)
            val c = Localized.context(context)
            val next = AlarmController(context).nextAlarm()
            val alarmText = next?.nextTriggerAt?.let {
                val pattern = if (settings.is24Hour) "HH:mm" else "h:mm a"
                c.getString(R.string.widget_alarm, SimpleDateFormat(pattern, Locale.getDefault()).format(Date(it)))
            }
            val weather = WeatherRepository.cached(context)
            val description = when (weather?.condition) {
                WeatherCondition.CLEAR -> R.string.wx_clear
                WeatherCondition.MOSTLY_CLEAR -> R.string.wx_mostly_clear
                WeatherCondition.PARTLY_CLOUDY -> R.string.wx_partly_cloudy
                WeatherCondition.CLOUDY -> R.string.wx_cloudy
                WeatherCondition.FOG -> R.string.wx_fog
                WeatherCondition.DRIZZLE -> R.string.wx_drizzle
                WeatherCondition.RAIN -> R.string.wx_rain
                WeatherCondition.HEAVY_RAIN -> R.string.wx_heavy_rain
                WeatherCondition.SHOWERS -> R.string.wx_showers
                WeatherCondition.SLEET -> R.string.wx_sleet
                WeatherCondition.SNOW -> R.string.wx_snow
                WeatherCondition.SNOW_SHOWERS -> R.string.wx_snow_showers
                WeatherCondition.THUNDERSTORM -> R.string.wx_thunder
                null -> null
            }?.let(c::getString).orEmpty()
            val event = CalendarEvents.next(context)
            val eventText = event?.let { formatEvent(c, it, settings.is24Hour) }
            return WidgetContent(
                is24Hour = settings.is24Hour,
                timeFormat = settings.timeFormat,
                alarmText = alarmText,
                alarmAt = next?.nextTriggerAt,
                weather = weather,
                temperatureText = weather?.let { WeatherCodes.formatTemperature(it.temperatureCelsius, settings.temperatureUnit) }.orEmpty(),
                weatherDescription = description,
                event = event,
                eventText = eventText,
                textColor = settings.widgetTextColor,
                placeName = weather?.takeIf { settings.widgetShowPlace }?.let {
                    if (it.city == WeatherRepository.LOCATION_KEY) DeviceLocation.placeName(context) else it.city
                },
                accentHex = SettingsStore.ACCENTS[settings.accentColor]?.darkPrimary
                    ?: if (settings.theme == SettingsStore.THEME_PINK) "#F49AC1" else "#A855F7",
            )
        }

        /** "11:00 AM · Title", "Tomorrow 9:00 AM · Title", "Now · Title" or "All day · Title". */
        fun formatEvent(c: Context, e: CalendarEvent, is24: Boolean, now: Long = System.currentTimeMillis()): String {
            val pattern = if (is24) "HH:mm" else "h:mm a"
            val time = SimpleDateFormat(pattern, Locale.getDefault()).format(Date(e.begin))
            val zone = java.time.ZoneId.systemDefault()
            val today = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            val day = java.time.Instant.ofEpochMilli(e.begin).atZone(zone).toLocalDate()
            val `when` = when {
                e.allDay -> c.getString(R.string.event_all_day)
                e.begin <= now -> c.getString(R.string.event_now)
                day == today -> time
                day == today.plusDays(1) -> c.getString(R.string.event_tomorrow, time)
                else -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(e.begin)) + " " + time
            }
            return "$`when` · ${e.title}"
        }
    }
}
