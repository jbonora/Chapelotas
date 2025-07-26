package com.chapelotas.app.domain.entities

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Evento del calendario con soporte completo de zonas horarias
 */
data class CalendarEvent(
    val id: Long,
    val title: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val timeZone: ZoneId = ZoneId.systemDefault(), // Zona horaria del evento
    val location: String? = null,
    val description: String? = null,
    val calendarId: Long,
    val calendarName: String,
    val isAllDay: Boolean = false,
    val isCritical: Boolean = false
) {
    /**
     * Obtiene el inicio como ZonedDateTime (con zona horaria)
     */
    fun getStartZonedTime(): ZonedDateTime = ZonedDateTime.of(startTime, timeZone)

    /**
     * Obtiene el fin como ZonedDateTime (con zona horaria)
     */
    fun getEndZonedTime(): ZonedDateTime = ZonedDateTime.of(endTime, timeZone)

    /**
     * Convierte el inicio a la zona horaria local del dispositivo
     */
    fun getStartInLocalZone(): ZonedDateTime = getStartZonedTime().withZoneSameInstant(ZoneId.systemDefault())

    /**
     * Convierte el fin a la zona horaria local del dispositivo
     */
    fun getEndInLocalZone(): ZonedDateTime = getEndZonedTime().withZoneSameInstant(ZoneId.systemDefault())

    /**
     * Obtiene el inicio en epoch millis (UTC)
     */
    fun getStartMillis(): Long = getStartZonedTime().toInstant().toEpochMilli()

    /**
     * Obtiene el fin en epoch millis (UTC)
     */
    fun getEndMillis(): Long = getEndZonedTime().toInstant().toEpochMilli()
}