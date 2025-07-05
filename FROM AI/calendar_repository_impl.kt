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

/**
 * Implementación del repositorio de calendario usando el ContentProvider de Android
 */
@Singleton
class CalendarRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: com.chapelotas.app.domain.repositories.PreferencesRepository
) : CalendarRepository {
    
    private val contentResolver: ContentResolver = context.contentResolver
    private val criticalEventIds = mutableSetOf<Long>()
    
    companion object {
        // Columnas de Calendarios
        private val CALENDAR_PROJECTION = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.VISIBLE
        )
        
        // Columnas de Eventos
        private val EVENT_PROJECTION = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.CALENDAR_DISPLAY_NAME
        )
        
        // Índices de las columnas
        private const val CALENDAR_ID_INDEX = 0
        private const val CALENDAR_NAME_INDEX = 1
        
        private const val EVENT_ID_INDEX = 0
        private const val EVENT_TITLE_INDEX = 1
        private const val EVENT_DESCRIPTION_INDEX = 2
        private const val EVENT_START_INDEX = 3
        private const val EVENT_END_INDEX = 4
        private const val EVENT_ALL_DAY_INDEX = 5
        private const val EVENT_LOCATION_INDEX = 6
        private const val EVENT_CALENDAR_ID_INDEX = 7
        private const val EVENT_CALENDAR_NAME_INDEX = 8
    }
    
    override suspend fun getAvailableCalendars(): Map<Long, String> = withContext(Dispatchers.IO) {
        checkCalendarPermission()
        
        val calendars = mutableMapOf<Long, String>()
        
        val uri: Uri = CalendarContract.Calendars.CONTENT_URI
        val selection = "${CalendarContract.Calendars.VISIBLE} = ?"
        val selectionArgs = arrayOf("1")
        
        contentResolver.query(
            uri,
            CALENDAR_PROJECTION,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(CALENDAR_ID_INDEX)
                val name = cursor.getString(CALENDAR_NAME_INDEX) ?: "Calendar $id"
                calendars[id] = name
            }
        }
        
        calendars
    }
    
    override suspend fun getEventsForDate(
        date: LocalDate,
        calendarIds: Set<Long>?
    ): List<CalendarEvent> = withContext(Dispatchers.IO) {
        checkCalendarPermission()
        
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        getEventsInTimeRange(startOfDay, endOfDay, calendarIds)
    }
    
    override suspend fun getEventsInRange(
        startDate: LocalDate,
        endDate: LocalDate,
        calendarIds: Set<Long>?
    ): List<CalendarEvent> = withContext(Dispatchers.IO) {
        checkCalendarPermission()
        
        val startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        getEventsInTimeRange(startMillis, endMillis, calendarIds)
    }
    
    override fun observeCalendarChanges(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        
        contentResolver.registerContentObserver(
            CalendarContract.Events.CONTENT_URI,
            true,
            observer
        )
        
        awaitClose {
            contentResolver.unregisterContentObserver(observer)
        }
    }
    
    override suspend fun markEventAsCritical(eventId: Long, isCritical: Boolean) {
        if (isCritical) {
            criticalEventIds.add(eventId)
        } else {
            criticalEventIds.remove(eventId)
        }
        // En una app real, esto se guardaría en base de datos o SharedPreferences
    }
    
    override suspend fun getCriticalEventIds(): Set<Long> {
        return criticalEventIds.toSet()
    }
    
    /**
     * Obtiene eventos en un rango de tiempo
     */
    private fun getEventsInTimeRange(
        startMillis: Long,
        endMillis: Long,
        calendarIds: Set<Long>?
    ): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        
        val uri: Uri = CalendarContract.Events.CONTENT_URI
        
        var selection = "(${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?)"
        val selectionArgsList = mutableListOf(startMillis.toString(), endMillis.toString())
        
        // Filtrar por calendarios si se especifican
        if (!calendarIds.isNullOrEmpty()) {
            selection += " AND ${CalendarContract.Events.CALENDAR_ID} IN (${calendarIds.joinToString(",") { "?" }})"
            selectionArgsList.addAll(calendarIds.map { it.toString() })
        }
        
        val selectionArgs = selectionArgsList.toTypedArray()
        
        contentResolver.query(
            uri,
            EVENT_PROJECTION,
            selection,
            selectionArgs,
            "${CalendarContract.Events.DTSTART} ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val event = parseEventFromCursor(cursor)
                events.add(event)
            }
        }
        
        return events
    }
    
    /**
     * Parsea un evento desde el cursor
     */
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
        
        val startTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(startMillis),
            ZoneId.systemDefault()
        )
        
        val endTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(endMillis),
            ZoneId.systemDefault()
        )
        
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
    
    /**
     * Verifica que tenemos permisos de calendario
     */
    private fun checkCalendarPermission() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALENDAR
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("No hay permisos para leer el calendario")
        }
    }
}