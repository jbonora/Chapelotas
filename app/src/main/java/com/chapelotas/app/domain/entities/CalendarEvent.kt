package com.chapelotas.app.domain.entities

import com.chapelotas.app.domain.models.TaskType // <-- AÑADIR IMPORT
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

data class CalendarEvent(
    val id: Long,
    val title: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    // PASO 1.4: Añadimos el tipo de tarea a la entidad del evento de calendario.
    val taskType: TaskType,
    val timeZone: ZoneId = ZoneId.systemDefault(),
    val location: String? = null,
    val description: String? = null,
    val calendarId: Long,
    val calendarName: String,
    val isAllDay: Boolean = false,
    val isCritical: Boolean = false,
    val isRecurring: Boolean = false
) {
    fun getStartZonedTime(): ZonedDateTime = ZonedDateTime.of(startTime, timeZone)
    fun getEndZonedTime(): ZonedDateTime = ZonedDateTime.of(endTime, timeZone)
    fun getStartInLocalZone(): ZonedDateTime = getStartZonedTime().withZoneSameInstant(ZoneId.systemDefault())
    fun getEndInLocalZone(): ZonedDateTime = getEndZonedTime().withZoneSameInstant(ZoneId.systemDefault())
    fun getStartMillis(): Long = getStartZonedTime().toInstant().toEpochMilli()
    fun getEndMillis(): Long = getEndZonedTime().toInstant().toEpochMilli()
}