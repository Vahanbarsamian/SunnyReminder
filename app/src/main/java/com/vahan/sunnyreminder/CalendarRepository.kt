package com.vahan.sunnyreminder

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import android.util.Log

data class CalendarEvent(
    val title: String,
    val startTime: Long
)

class CalendarRepository(private val context: Context) {

    fun getUpcomingEvents(hoursAhead: Int = 24): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val contentResolver = context.contentResolver
        
        val now = System.currentTimeMillis()
        val future = now + (hoursAhead * 60 * 60 * 1000)

        // Querying Instances allows seeing events from ALL calendars (including URL-based ones)
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now)
        ContentUris.appendId(builder, future)

        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN
        )

        try {
            val cursor = contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )

            cursor?.use {
                val titleIndex = it.getColumnIndex(CalendarContract.Instances.TITLE)
                val startIndex = it.getColumnIndex(CalendarContract.Instances.BEGIN)

                while (it.moveToNext()) {
                    val title = it.getString(titleIndex) ?: "Événement sans titre"
                    val startTime = it.getLong(startIndex)
                    events.add(CalendarEvent(title, startTime))
                }
            }
        } catch (e: Exception) {
            Log.e("CalendarRepository", "Error querying calendar instances", e)
        }

        return events
    }
}
