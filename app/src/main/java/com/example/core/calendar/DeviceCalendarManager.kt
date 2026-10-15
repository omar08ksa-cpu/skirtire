package com.example.core.calendar

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * نموذج حدث التقويم الموحد (تقويم الهاتف / سامسونج أو الأحداث المحلية)
 */
data class CalendarEventItem(
    val id: String,
    val title: String,
    val description: String = "",
    val location: String = "",
    val startTimestamp: Long,
    val endTimestamp: Long,
    val startTimeFormatted: String = "",
    val endTimeFormatted: String = "",
    val dateKey: String = "", // YYYY-MM-DD
    val colorHex: String = "#2563EB",
    val isAllDay: Boolean = false,
    val calendarDisplayName: String = "تقويم الهاتف",
    val source: String = "DEVICE_SAMSUNG" // DEVICE_SAMSUNG, LOCAL_APP
)

object DeviceCalendarManager {
    private const val TAG = "DeviceCalendarManager"

    fun hasCalendarPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * جلب أحداث تقويم الهاتف (بما في ذلك تقويم سامسونج وتقويم Google على الجهاز)
     * في نطاق زمني محدد (افتراضياً: شهرين قبل وشهرين بعد)
     */
    suspend fun fetchDeviceCalendarEvents(
        context: Context,
        startMillis: Long = System.currentTimeMillis() - 60L * 24 * 3600 * 1000,
        endMillis: Long = System.currentTimeMillis() + 90L * 24 * 3600 * 1000
    ): List<CalendarEventItem> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission(context)) {
            Log.w(TAG, "READ_CALENDAR permission not granted.")
            return@withContext emptyList()
        }

        val eventsList = mutableListOf<CalendarEventItem>()
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        try {
            // استخدام CalendarContract.Instances لدعم الأحداث المتكررة والمفردة بدقة
            val builder: Uri.Builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, startMillis)
            ContentUris.appendId(builder, endMillis)

            val projection = arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.DESCRIPTION,
                CalendarContract.Instances.EVENT_LOCATION,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.DISPLAY_COLOR,
                CalendarContract.Instances.CALENDAR_DISPLAY_NAME
            )

            val cursor: Cursor? = context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )

            cursor?.use { c ->
                val idIdx = c.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                val titleIdx = c.getColumnIndex(CalendarContract.Instances.TITLE)
                val descIdx = c.getColumnIndex(CalendarContract.Instances.DESCRIPTION)
                val locIdx = c.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
                val beginIdx = c.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIdx = c.getColumnIndex(CalendarContract.Instances.END)
                val allDayIdx = c.getColumnIndex(CalendarContract.Instances.ALL_DAY)
                val colorIdx = c.getColumnIndex(CalendarContract.Instances.DISPLAY_COLOR)
                val calNameIdx = c.getColumnIndex(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)

                while (c.moveToNext()) {
                    val eventId = if (idIdx >= 0) c.getLong(idIdx).toString() else ""
                    val title = if (titleIdx >= 0) c.getString(titleIdx).orEmpty() else "حدث بدون عنوان"
                    val desc = if (descIdx >= 0) c.getString(descIdx).orEmpty() else ""
                    val loc = if (locIdx >= 0) c.getString(locIdx).orEmpty() else ""
                    val begin = if (beginIdx >= 0) c.getLong(beginIdx) else 0L
                    val end = if (endIdx >= 0) c.getLong(endIdx) else begin
                    val allDay = if (allDayIdx >= 0) c.getInt(allDayIdx) == 1 else false
                    val colorInt = if (colorIdx >= 0) c.getInt(colorIdx) else 0
                    val calName = if (calNameIdx >= 0) c.getString(calNameIdx).orEmpty().ifBlank { "تقويم الهاتف" } else "تقويم الهاتف"

                    val colorHex = if (colorInt != 0) {
                        String.format("#%06X", 0xFFFFFF and colorInt)
                    } else {
                        "#2563EB"
                    }

                    val dateKey = dateKeyFormat.format(Date(begin))
                    val startStr = if (allDay) "طوال اليوم" else timeFormat.format(Date(begin))
                    val endStr = if (allDay) "" else timeFormat.format(Date(end))

                    eventsList.add(
                        CalendarEventItem(
                            id = "dev_$eventId",
                            title = title.ifBlank { "موعد / حدث" },
                            description = desc,
                            location = loc,
                            startTimestamp = begin,
                            endTimestamp = end,
                            startTimeFormatted = startStr,
                            endTimeFormatted = endStr,
                            dateKey = dateKey,
                            colorHex = colorHex,
                            isAllDay = allDay,
                            calendarDisplayName = calName,
                            source = "DEVICE_SAMSUNG"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying device calendar instances", e)
        }

        eventsList
    }

    /**
     * إضافة حدث إلى تقويم الهاتف مباشرة
     */
    suspend fun addEventToDeviceCalendar(
        context: Context,
        title: String,
        description: String,
        location: String,
        startMillis: Long,
        endMillis: Long,
        isAllDay: Boolean
    ): Long? = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return@withContext null
        }

        try {
            // العثور على تقويم أساسي متاح
            val calCursor = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.IS_PRIMARY),
                null,
                null,
                null
            )
            var primaryCalId: Long? = null
            calCursor?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                    primaryCalId = cursor.getLong(idIdx)
                }
            }

            if (primaryCalId == null) {
                primaryCalId = 1L
            }

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, description)
                put(CalendarContract.Events.CALENDAR_ID, primaryCalId)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.EVENT_LOCATION, location)
                put(CalendarContract.Events.ALL_DAY, if (isAllDay) 1 else 0)
            }

            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            uri?.lastPathSegment?.toLongOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting event to device calendar", e)
            null
        }
    }
}
