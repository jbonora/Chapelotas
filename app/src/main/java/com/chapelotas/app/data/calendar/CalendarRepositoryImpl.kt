package com.chapelotas.app.data.calendar

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.chapelotas.app.domain.entities.CalendarEvent
import com.chapelotas.app.domain.repositories.CalendarRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CalendarRepository {

    private val contentResolver: ContentResolver = context.contentResolver
    private val criticalEventIds = mutableSetOf<Long>()

    companion object {
        private val INSTANCE_PROJECTION = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME
        )
        private const val EVENT_ID_INDEX = 0
        private const val EVENT_TITLE_INDEX = 1
        private const val EVENT_DESCRIPTION_INDEX = 2
        private const val EVENT_START_INDEX = 3
        private const val EVENT_END_INDEX = 4
        private const val EVENT_ALL_DAY_INDEX = 5
        private const val EVENT_LOCATION_INDEX = 6
        private const val EVENT_CALENDAR_ID_INDEX = 7
        private const val EVENT_CALENDAR_NAME_INDEX = 8

        private val CALENDAR_PROJECTION = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        )
        private const val CALENDAR_ID_INDEX = 0
        private const val CALENDAR_NAME_INDEX = 1
    }

    override fun hasCalendarReadPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun getAvailableCalendars(): Map<Long, String> = withContext(Dispatchers.IO) {
        if (!hasCalendarReadPermission()) {
            Log.w("CalendarRepo", "Intento de acceso a calendarios sin permiso.")
            return@withContext emptyMap()
        }

        val calendars = mutableMapOf<Long, String>()
        val uri: Uri = CalendarContract.Calendars.CONTENT_URI
        val selection = "${CalendarContract.Calendars.VISIBLE} = ?"
        val selectionArgs = arrayOf("1")
        try {
            contentResolver.query(uri, CALENDAR_PROJECTION, selection, selectionArgs, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(CALENDAR_ID_INDEX)
                    val name = cursor.getString(CALENDAR_NAME_INDEX) ?: "Calendar $id"
                    calendars[id] = name
                }
            }
        } catch (e: Exception) {
            Log.e("CalendarRepo", "Error al buscar calendarios: ${e.message}")
        }
        calendars
    }

    override suspend fun getEventsForDate(date: LocalDate): List<CalendarEvent> = withContext(Dispatchers.IO) {
        if (!hasCalendarReadPermission()) return@withContext emptyList()
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        getEventsInTimeRange(startOfDay, endOfDay)
    }

    override suspend fun getEventsInRange(startDate: LocalDate, endDate: LocalDate): List<CalendarEvent> = withContext(Dispatchers.IO) {
        if (!hasCalendarReadPermission()) return@withContext emptyList()
        val startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        getEventsInTimeRange(startMillis, endMillis)
    }

    override fun observeCalendarChanges(): Flow<Unit> = callbackFlow {
        if (!hasCalendarReadPermission()) {
            Log.w("CalendarRepo", "No se puede observar el calendario sin permisos.")
            close(SecurityException("Permiso READ_CALENDAR denegado para observar cambios."))
            return@callbackFlow
        }
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { trySend(Unit) }
        }
        contentResolver.registerContentObserver(CalendarContract.Events.CONTENT_URI, true, observer)
        awaitClose { contentResolver.unregisterContentObserver(observer) }
    }

    override suspend fun markEventAsCritical(eventId: Long, isCritical: Boolean) {
        if (isCritical) {
            criticalEventIds.add(eventId)
        } else {
            criticalEventIds.remove(eventId)
        }
    }

    override suspend fun getCriticalEventIds(): Set<Long> {
        return criticalEventIds.toSet()
    }

    private fun getEventsInTimeRange(startMillis: Long, endMillis: Long): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val builder: Uri.Builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)
        try {
            contentResolver.query(builder.build(), INSTANCE_PROJECTION, null, null, "${CalendarContract.Instances.BEGIN} ASC")
                ?.use { cursor ->
                    while (cursor.moveToNext()) {
                        events.add(parseEventFromCursor(cursor))
                    }
                }
        } catch (e: Exception) {
            Log.e("CalendarRepo", "ERROR al consultar eventos: ${e.message}")
        }
        return events
    }

    private fun parseEventFromCursor(cursor: Cursor): CalendarEvent {
        val id = cursor.getLong(EVENT_ID_INDEX)
        val title = cursor.getString(EVENT_TITLE_INDEX) ?: "Sin título"
        val description = cursor.getString(EVENT_DESCRIPTION_INDEX)
        val startMillis = cursor.getLong(EVENT_START_INDEX)
        val endMillis = cursor.getLong(EVENT_END_INDEX)
        val isAllDay = cursor.getInt(EVENT_ALL_DAY_INDEX) == 1
        val location = cursor.getString(EVENT_LOCATION_INDEX)
        val calendarId = cursor.getLong(EVENT_CALENDAR_ID_INDEX)
        val calendarName = cursor.getString(EVENT_CALENDAR_NAME_INDEX) ?: "Calendar"
        val startTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startMillis), ZoneId.systemDefault())
        val endTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endMillis), ZoneId.systemDefault())
        return CalendarEvent(
            id = id,
            title = title,
            description = description,
            startTime = startTime,
            endTime = endTime,
            location = location,
            isAllDay = isAllDay,
            calendarId = calendarId,
            calendarName = calendarName,
            isCritical = id in criticalEventIds
        )
    }
}