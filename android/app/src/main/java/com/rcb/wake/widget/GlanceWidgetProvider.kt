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
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.rcb.wake.MainActivity
import com.rcb.wake.R
import com.rcb.wake.notification.Localized
import com.rcb.wake.storage.AppStorage
import com.rcb.wake.storage.SettingsStore
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * "My Day" widget: a time-of-day greeting, the date with the current weather, and the next
 * calendar event. Uses the same data and settings (weather, text colour, place name) as the clock
 * widget. The greeting is re-rendered at each boundary by an inexact, non-waking alarm.
 */
class GlanceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        render(context, manager, ids)
        ClockWidgetProvider.scheduleWeatherRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_GREETING -> updateAll(context)
            ACTION_NEXT_PAGE -> {
                val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (id != AppWidgetManager.INVALID_APPWIDGET_ID) nextPage(context, id)
            }
            else -> super.onReceive(context, intent)
        }
    }

    override fun onDisabled(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(greetingIntent(context))
        ClockWidgetProvider.scheduleWeatherRefresh(context)
    }

    enum class Greeting(val res: Int) {
        MORNING(R.string.greeting_morning),
        AFTERNOON(R.string.greeting_afternoon),
        EVENING(R.string.greeting_evening),
        NIGHT(R.string.greeting_night),
    }

    companion object {
        private const val ACTION_GREETING = "com.rcb.wake.action.GLANCE_GREETING"

        /** Morning 5–12, afternoon 12–17, evening 17–21, night 21–5. */
        fun greetingFor(hour: Int): Greeting = when (hour) {
            in 5..11 -> Greeting.MORNING
            in 12..16 -> Greeting.AFTERNOON
            in 17..20 -> Greeting.EVENING
            else -> Greeting.NIGHT
        }

        /** The next time the greeting changes, after [now]. */
        fun nextBoundary(now: LocalDateTime): LocalDateTime {
            val today = now.toLocalDate()
            return listOf(5, 12, 17, 21)
                .map { today.atTime(it, 0) }
                .firstOrNull { it.isAfter(now) }
                ?: today.plusDays(1).atTime(5, 0)
        }

        fun ids(context: Context): IntArray =
            AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, GlanceWidgetProvider::class.java))

        fun updateAll(context: Context) {
            val ids = ids(context)
            if (ids.isNotEmpty()) render(context, AppWidgetManager.getInstance(context), ids)
        }

        private fun greetingIntent(context: Context) = PendingIntent.getBroadcast(
            context,
            902,
            Intent(context, GlanceWidgetProvider::class.java).setAction(ACTION_GREETING),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        private fun scheduleGreeting(context: Context) {
            val zone = ZoneId.systemDefault()
            val at = nextBoundary(LocalDateTime.now(zone)).atZone(zone).toInstant().toEpochMilli()
            // RTC (not _WAKEUP): never wakes the device; a change due while asleep shows on wake.
            context.getSystemService(AlarmManager::class.java).set(AlarmManager.RTC, at, greetingIntent(context))
        }

        private const val ACTION_NEXT_PAGE = "com.rcb.wake.action.GLANCE_NEXT_PAGE"
        private fun pageKey(id: Int) = "glance_page_$id"

        private fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val content = WidgetContent.load(context)
            CalendarEvents.scheduleRefresh(context, content.event)
            val settings = SettingsStore.load(context)
            val c = Localized.context(context)
            val headphones = if (settings.widgetShowHeadphones) Headphones.current(context) else null
            val hasNext = content.eventText != null || content.alarmText != null || headphones != null
            val state = AppStorage.state(context)
            for (id in ids) {
                val page = if (hasNext) state.getInt(pageKey(id), 0).coerceIn(0, 1) else 0
                manager.updateAppWidget(id, build(context, c, content, settings, headphones, hasNext, page, id))
            }
            scheduleGreeting(context)
        }

        private fun build(
            context: Context,
            c: Context,
            content: WidgetContent,
            settings: com.rcb.wake.storage.WakeSettings,
            headphones: HeadphonesInfo?,
            hasNext: Boolean,
            page: Int,
            id: Int,
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_glance)

            // Page 1: greeting (optional), date with year, weather.
            views.setTextViewText(R.id.glance_greeting, c.getString(greetingFor(LocalDateTime.now().hour).res))
            views.setViewVisibility(R.id.glance_greeting, if (settings.widgetShowGreeting) View.VISIBLE else View.GONE)
            // Without the greeting, the date becomes the headline.
            views.setTextViewTextSize(R.id.glance_date, TypedValue.COMPLEX_UNIT_SP, if (settings.widgetShowGreeting) 16f else 24f)
            val weather = content.weather
            views.setViewVisibility(R.id.glance_weather, if (weather != null) View.VISIBLE else View.GONE)
            if (weather != null) {
                views.setImageViewResource(R.id.glance_weather_icon, ClockWidgetProvider.weatherIcon(weather))
                views.setContentDescription(R.id.glance_weather_icon, content.weatherDescription)
                views.setTextViewText(
                    R.id.glance_weather_temp,
                    content.placeName?.let { "${content.temperatureText} · $it" } ?: content.temperatureText,
                )
            }

            // Page 2: next event, next alarm, earphones.
            views.setViewVisibility(R.id.glance_event, if (content.eventText != null) View.VISIBLE else View.GONE)
            content.eventText?.let { views.setTextViewText(R.id.glance_event, it) }
            views.setViewVisibility(R.id.glance_alarm, if (content.alarmText != null) View.VISIBLE else View.GONE)
            content.alarmText?.let { views.setTextViewText(R.id.glance_alarm, it) }
            views.setViewVisibility(R.id.glance_headphones, if (headphones != null) View.VISIBLE else View.GONE)
            headphones?.let { views.setTextViewText(R.id.glance_headphones_text, it.label()) }

            views.setDisplayedChild(R.id.glance_pages, page)
            views.setViewVisibility(R.id.glance_dots, if (hasNext) View.VISIBLE else View.GONE)
            views.setInt(R.id.glance_dot_1, "setImageAlpha", if (page == 0) 255 else 90)
            views.setInt(R.id.glance_dot_2, "setImageAlpha", if (page == 1) 255 else 90)
            applyTextColor(views, content)

            // Tap: next page when there is a second page, otherwise open the app.
            val tap = if (hasNext) {
                PendingIntent.getBroadcast(
                    context,
                    id,
                    Intent(context, GlanceWidgetProvider::class.java)
                        .setAction(ACTION_NEXT_PAGE)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            } else {
                PendingIntent.getActivity(
                    context,
                    903,
                    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }
            views.setOnClickPendingIntent(R.id.glance_root, tap)
            return views
        }

        private fun nextPage(context: Context, id: Int) {
            val state = AppStorage.state(context)
            state.edit().putInt(pageKey(id), (state.getInt(pageKey(id), 0) + 1) % 2).apply()
            render(context, AppWidgetManager.getInstance(context), intArrayOf(id))
        }

        private fun applyTextColor(views: RemoteViews, content: WidgetContent) {
            val primary = listOf(R.id.glance_greeting)
            val primary2 = listOf(R.id.glance_next_title)
            val secondary = listOf(R.id.glance_date, R.id.glance_weather_temp, R.id.glance_headphones_text, R.id.glance_event, R.id.glance_alarm)
            when {
                content.textColor == "material" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    (primary + primary2).forEach { views.setColorStateList(it, "setTextColor", android.R.color.system_accent1_100) }
                    secondary.forEach { views.setColorStateList(it, "setTextColor", android.R.color.system_accent2_50) }
                    views.setColorStateList(R.id.glance_headphones_icon, "setImageTintList", android.R.color.system_accent2_50)
                }
                content.textColor == "accent" -> {
                    val color = Color.parseColor(content.accentHex)
                    (primary + primary2 + secondary).forEach { views.setTextColor(it, color) }
                    views.setInt(R.id.glance_headphones_icon, "setColorFilter", color)
                }
            }
        }
    }
}
