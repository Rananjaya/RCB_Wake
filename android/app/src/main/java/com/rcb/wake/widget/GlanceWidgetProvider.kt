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


        private const val ACTION_NEXT_PAGE = "com.rcb.wake.action.GLANCE_NEXT_PAGE"
        private const val ROTATE_MILLIS = 6_000
        private fun pageKey(id: Int) = "glance_page_$id"
        private fun soonKey(id: Int) = "glance_soon_$id"
        private fun earphonesKey(id: Int) = "glance_earphones_$id"

        private fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val content = WidgetContent.load(context)
            CalendarEvents.scheduleRefresh(context, content.event)
            val settings = SettingsStore.load(context)
            val c = Localized.context(context)
            val headphones = if (settings.widgetShowHeadphones) Headphones.current(context) else null
            val hasNext = content.eventText != null || content.alarmText != null || headphones != null
            val state = AppStorage.state(context)
            val now = System.currentTimeMillis()
            // An event or alarm within SOON_MILLIS takes over: show "Coming up" and stop rotating.
            val soonEnd = soonUntil(content, now)
            val soon = soonEnd != null
            for (id in ids) {
                // When something becomes soon, jump to "Soon" once; after that, taps still switch.
                if (soon && state.getLong(soonKey(id), 0L) != soonEnd) {
                    state.edit().putLong(soonKey(id), soonEnd!!).putInt(pageKey(id), 1).apply()
                }
                // When earphones connect, show the second page once (where they are listed); taps still switch.
                val connected = headphones?.name
                if (connected != state.getString(earphonesKey(id), null)) {
                    val edit = state.edit().putString(earphonesKey(id), connected)
                    if (connected != null) edit.putInt(pageKey(id), 1)
                    edit.apply()
                }
                val page = if (hasNext) state.getInt(pageKey(id), 0).coerceIn(0, 1) else 0
                manager.updateAppWidget(id, build(context, c, content, settings, headphones, hasNext, page, id, soon))
            }
            scheduleNextRender(context, content, now)
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
            soon: Boolean,
        ): RemoteViews {
            // A separate still layout, because a running flipper keeps its old interval.
            // Automatic switching: always while something is soon, otherwise only if the setting is on.
            val rotate = hasNext && (settings.widgetRotatePages || soon)
            val views = RemoteViews(context.packageName, if (rotate) R.layout.widget_glance else R.layout.widget_glance_static)

            // Page 1: greeting (optional), date with year, weather.
            views.setViewVisibility(R.id.glance_next_title, if (soon) View.VISIBLE else View.GONE)
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

            // Page 2: next event, next alarm, earphones. While something is soon, only the soon event/alarm.
            val now = System.currentTimeMillis()
            val showEvent = content.eventText != null && (!soon || eventSoon(content, now))
            val showAlarm = content.alarmText != null && (!soon || alarmSoon(content, now))
            views.setViewVisibility(R.id.glance_event, if (showEvent) View.VISIBLE else View.GONE)
            content.eventText?.let { views.setTextViewText(R.id.glance_event, it) }
            views.setViewVisibility(R.id.glance_alarm, if (showAlarm) View.VISIBLE else View.GONE)
            content.alarmText?.let { views.setTextViewText(R.id.glance_alarm, it) }
            // Earphones stay visible, also while something is soon.
            views.setViewVisibility(R.id.glance_headphones, if (headphones != null) View.VISIBLE else View.GONE)
            headphones?.let { views.setTextViewText(R.id.glance_headphones_text, it.label()) }

            views.setDisplayedChild(R.id.glance_pages, page)
            listOf(R.id.glance_dots_1, R.id.glance_dots_2).forEach {
                views.setViewVisibility(it, if (hasNext) View.VISIBLE else View.GONE)
            }
            if (rotate) views.setInt(R.id.glance_pages, "setFlipInterval", ROTATE_MILLIS)
            applyTextColor(views, content)

            // Every tap goes to WidgetTapActivity, which tells a single tap (next page) from a
            // double tap (open the app). Widget clicks may start activities; broadcasts may not.
            val tap = PendingIntent.getActivity(
                context,
                id,
                Intent(context, WidgetTapActivity::class.java)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    .putExtra(WidgetTapActivity.EXTRA_HAS_PAGES, hasNext)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.glance_root, tap)
            return views
        }

        /** Shown "soon" from SOON_MILLIS before the event or alarm until it starts (events: until they end). */
        private const val SOON_MILLIS = 10 * 60_000L

        /** The event is soon: from SOON_MILLIS before it starts until it ends (timed events only). */
        fun eventSoon(content: WidgetContent, now: Long): Boolean =
            content.event?.let { !it.allDay && it.begin - SOON_MILLIS <= now && now < it.end } == true

        /** The alarm is soon: from SOON_MILLIS before it rings until it rings. */
        fun alarmSoon(content: WidgetContent, now: Long): Boolean =
            content.alarmAt?.let { it - SOON_MILLIS <= now && now < it } == true

        /** If something is soon, when that state ends; otherwise null. */
        fun soonUntil(content: WidgetContent, now: Long): Long? {
            val ends = mutableListOf<Long>()
            if (eventSoon(content, now)) ends += content.event!!.end
            if (alarmSoon(content, now)) ends += content.alarmAt!!
            return ends.maxOrNull()
        }

        /** Next moment the widget must change by itself: a greeting boundary, or a "soon" start or end. */
        private fun scheduleNextRender(context: Context, content: WidgetContent, now: Long) {
            val zone = ZoneId.systemDefault()
            val times = mutableListOf(nextBoundary(LocalDateTime.now(zone)).atZone(zone).toInstant().toEpochMilli())
            soonUntil(content, now)?.let { times += it + 1_000 }
            content.event?.takeIf { !it.allDay }?.let { times += it.begin - SOON_MILLIS }
            content.alarmAt?.let { times += it - SOON_MILLIS }
            val at = times.filter { it > now }.minOrNull() ?: return
            // RTC (not _WAKEUP): never wakes the device; a change due while asleep shows on wake.
            context.getSystemService(AlarmManager::class.java).set(AlarmManager.RTC, at, greetingIntent(context))
        }

        fun nextPage(context: Context, id: Int, step: Int = 1) {
            val state = AppStorage.state(context)
            state.edit().putInt(pageKey(id), (state.getInt(pageKey(id), 0) + step + 2) % 2).apply()
            render(context, AppWidgetManager.getInstance(context), intArrayOf(id))
        }

        private fun tintIcon(views: RemoteViews, color: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                views.setColorStateList(R.id.glance_headphones_icon, "setImageTintList", android.content.res.ColorStateList.valueOf(color))
            } else {
                views.setInt(R.id.glance_headphones_icon, "setColorFilter", color)
            }
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
                    tintIcon(views, color)
                }
                // Explicit white: earlier Material You / accent colours would otherwise remain.
                else -> {
                    (primary + primary2).forEach { views.setTextColor(it, ClockWidgetProvider.WHITE) }
                    secondary.forEach { views.setTextColor(it, ClockWidgetProvider.WHITE_SECONDARY) }
                    tintIcon(views, ClockWidgetProvider.WHITE_SECONDARY)
                }
            }
        }
    }
}
