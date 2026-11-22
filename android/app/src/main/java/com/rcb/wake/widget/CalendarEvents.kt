package com.rcb.wake.widget

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import com.rcb.wake.storage.AppStorage

data class DeviceCalendar(
    val id: Long,
    val name: String,
    val accountName: String,
    val accountType: String,
    val color: Int,
)

data class CalendarEvent(val title: String, val begin: Long, val end: Long, val allDay: Boolean)

/**
 * Optional "next calendar event" line on the widget. Reads the system calendar provider, which
 * Google Calendar and other calendar apps sync into, from the calendars the user selected.
 * Event data is only read on the device and never stored or sent anywhere.
 */
object CalendarEvents {
    private const val TAG = "RcbWakeCalendar"
    private const val KEY_ENABLED = "calendarEnabled"
    private const val KEY_IDS = "calendarIds"
    private const val LOOKAHEAD_MILLIS = 7 * 24 * 60 * 60_000L
    private const val REFRESH_REQUEST_CODE = Int.MAX_VALUE / 2 - 2

    fun hasPermission(context: Context) =
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun isEnabled(context: Context) = AppStorage.settings(context).getBoolean(KEY_ENABLED, false)

    /** Empty means "all visible calendars". */
    fun selectedIds(context: Context): Set<Long> =
        AppStorage.settings(context).getString(KEY_IDS, "")!!
            .split(',').mapNotNull { it.trim().toLongOrNull() }.toSet()

    fun save(context: Context, enabled: Boolean, ids: Collection<Long>) {
        AppStorage.settings(context).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putString(KEY_IDS, ids.joinToString(","))
            .apply()
    }

    fun calendars(context: Context): List<DeviceCalendar> {
        if (!hasPermission(context)) return emptyList()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_COLOR,
        )
        return runCatching {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI, projection,
                "${CalendarContract.Calendars.VISIBLE} = 1", null,
                "${CalendarContract.Calendars.ACCOUNT_NAME}, ${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME}",
            )?.use { c ->
                buildList {
                    while (c.moveToNext()) {
                        add(DeviceCalendar(c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "", c.getString(3) ?: "", c.getInt(4)))
                    }
                }
            }.orEmpty()
        }.getOrElse {
            Log.w(TAG, "Cannot read calendars: ${it.javaClass.simpleName}")
            emptyList()
        }
    }

    /** The ongoing or next event in the next 7 days from the selected calendars, or null. */
    fun next(context: Context, now: Long = System.currentTimeMillis()): CalendarEvent? {
        if (!isEnabled(context) || !hasPermission(context)) return null
        val ids = selectedIds(context)
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, now - 24 * 60 * 60_000L) // include all-day events of today
            ContentUris.appendId(it, now + LOOKAHEAD_MILLIS)
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.SELF_ATTENDEE_STATUS,
        )
        return runCatching {
            context.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
                val events = buildList {
                    while (c.moveToNext()) {
                        val calendarId = c.getLong(4)
                        if (ids.isNotEmpty() && calendarId !in ids) continue
                        if (c.getInt(5) == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED) continue
                        add(CalendarEvent(c.getString(0).orEmpty(), c.getLong(1), c.getLong(2), c.getInt(3) == 1))
                    }
                }
                selectNext(events, now)
            }
        }.getOrElse {
            Log.w(TAG, "Cannot read events: ${it.javaClass.simpleName}")
            null
        }
    }

    /** Pure selection rule: the earliest event that has not ended; timed events win over all-day ones. */
    fun selectNext(events: List<CalendarEvent>, now: Long): CalendarEvent? {
        val pending = events.filter { it.end > now && it.title.isNotBlank() }
        return pending.filterNot { it.allDay }.minByOrNull { it.begin } ?: pending.minByOrNull { it.begin }
    }

    /** Re-render the widget when [event] ends so the next event replaces it. */
    fun scheduleRefresh(context: Context, event: CalendarEvent?) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, ClockWidgetProvider::class.java))
        val hasAnyWidget = ids.isNotEmpty() || GlanceWidgetProvider.ids(context).isNotEmpty()
        val intent = Intent(context, ClockWidgetProvider::class.java)
            .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        val pi = PendingIntent.getBroadcast(context, REFRESH_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pi)
        if (event == null || !hasAnyWidget) return
        // Inexact is fine: the line may linger up to a few minutes after the event ends.
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, event.end + 30_000, pi)
    }
}
