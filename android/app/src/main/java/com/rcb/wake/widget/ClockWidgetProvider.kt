package com.rcb.wake.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.rcb.wake.MainActivity
import com.rcb.wake.R
import com.rcb.wake.alarm.AlarmController
import com.rcb.wake.notification.Localized
import com.rcb.wake.storage.SettingsStore
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
        refreshWeatherIfStale(context)
    }

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
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ClockWidgetProvider::class.java))
            if (ids.isNotEmpty()) render(context, manager, ids)
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
                val views = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    RemoteViews(
                        mapOf(
                            SizeF(100f, 100f) to build(context, Layout.SMALL, content),
                            SizeF(250f, 50f) to build(context, Layout.BAR, content),
                            SizeF(170f, 110f) to build(context, Layout.MEDIUM, content),
                            SizeF(250f, 110f) to build(context, Layout.WIDE, content),
                            SizeF(250f, 200f) to build(context, Layout.LARGE, content),
                        ),
                    )
                } else {
                    val options = manager.getAppWidgetOptions(id)
                    val w = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
                    val h = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 110)
                    build(context, layoutFor(w, h), content)
                }
                manager.updateAppWidget(id, views)
            }
        }

        private fun build(context: Context, layout: Layout, content: WidgetContent): RemoteViews {
            val views = RemoteViews(context.packageName, layout.resId)
            if (layout.splitClock) {
                views.setTimeFormat(R.id.widget_hour, if (content.is24Hour) "HH" else "hh")
                views.setTimeFormat(R.id.widget_minute, "mm")
            } else {
                views.setTimeFormat(R.id.widget_time, if (content.is24Hour) "HH:mm" else "h:mm")
            }
            if (layout.date) {
                views.setTimeFormat(R.id.widget_date, if (layout == Layout.SMALL) "EEE d" else "MMM, EEEE d")
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
                    views.setTextViewText(R.id.widget_weather_temp, content.temperatureText)
                }
            }

            val open = PendingIntent.getActivity(
                context,
                900,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)
            return views
        }

        private fun RemoteViews.setTimeFormat(viewId: Int, pattern: String) {
            setCharSequence(viewId, "setFormat12Hour", pattern)
            setCharSequence(viewId, "setFormat24Hour", pattern)
        }

        fun weatherIcon(w: WeatherSnapshot): Int = when (w.condition) {
            WeatherCondition.CLEAR -> if (w.isDay) R.drawable.ic_wx_clear_day else R.drawable.ic_wx_clear_night
            WeatherCondition.PARTLY_CLOUDY -> if (w.isDay) R.drawable.ic_wx_partly_day else R.drawable.ic_wx_partly_night
            WeatherCondition.CLOUDY -> R.drawable.ic_wx_cloudy
            WeatherCondition.FOG -> R.drawable.ic_wx_fog
            WeatherCondition.DRIZZLE, WeatherCondition.RAIN -> R.drawable.ic_wx_rain
            WeatherCondition.SHOWERS -> if (w.isDay) R.drawable.ic_wx_showers_day else R.drawable.ic_wx_showers_night
            WeatherCondition.SNOW -> R.drawable.ic_wx_snow
            WeatherCondition.SNOW_SHOWERS -> if (w.isDay) R.drawable.ic_wx_snow_showers_day else R.drawable.ic_wx_snow_showers_night
            WeatherCondition.THUNDERSTORM -> R.drawable.ic_wx_thunder
        }
    }
}

/** Everything the widget shows besides the system-driven clock. */
data class WidgetContent(
    val is24Hour: Boolean,
    val alarmText: String?,
    val weather: WeatherSnapshot?,
    val temperatureText: String,
    val weatherDescription: String,
    val event: CalendarEvent?,
    val eventText: String?,
) {
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
                WeatherCondition.PARTLY_CLOUDY -> R.string.wx_partly_cloudy
                WeatherCondition.CLOUDY -> R.string.wx_cloudy
                WeatherCondition.FOG -> R.string.wx_fog
                WeatherCondition.DRIZZLE, WeatherCondition.RAIN -> R.string.wx_rain
                WeatherCondition.SHOWERS -> R.string.wx_showers
                WeatherCondition.SNOW -> R.string.wx_snow
                WeatherCondition.SNOW_SHOWERS -> R.string.wx_snow_showers
                WeatherCondition.THUNDERSTORM -> R.string.wx_thunder
                null -> null
            }?.let(c::getString).orEmpty()
            val event = CalendarEvents.next(context)
            val eventText = event?.let { formatEvent(c, it, settings.is24Hour) }
            return WidgetContent(
                is24Hour = settings.is24Hour,
                alarmText = alarmText,
                weather = weather,
                temperatureText = weather?.let { WeatherCodes.formatTemperature(it.temperatureCelsius, settings.temperatureUnit) }.orEmpty(),
                weatherDescription = description,
                event = event,
                eventText = eventText,
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
