package com.chapelotas.app.domain.entities

import java.time.LocalDateTime

/**
 * Representa un evento del calendario
 * Esta es la entidad principal que maneja Chapelotas
 */
data class CalendarEvent(
    val id: Long,
    val title: String,
    val description: String?,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val location: String?,
    val isAllDay: Boolean = false,
    val calendarId: Long,
    val calendarName: String,
    // Campos específicos de Chapelotas
    val isCritical: Boolean = false,  // Marcado para alerta crítica (llamada simulada)
    val hasBeenSummarized: Boolean = false  // Ya fue incluido en un resumen
) {
    /**
     * Duración del evento en minutos
     */
    val durationInMinutes: Long
        get() = java.time.Duration.between(startTime, endTime).toMinutes()

    /**
     * Verifica si el evento es hoy
     */
    fun isToday(): Boolean {
        val today = LocalDateTime.now().toLocalDate()
        return startTime.toLocalDate() == today
    }

    /**
     * Verifica si el evento es mañana
     */
    fun isTomorrow(): Boolean {
        val tomorrow = LocalDateTime.now().toLocalDate().plusDays(1)
        return startTime.toLocalDate() == tomorrow
    }

    /**
     * Tiempo hasta el evento (puede ser negativo si ya pasó)
     */
    fun minutesUntilStart(): Long {
        return java.time.Duration.between(LocalDateTime.now(), startTime).toMinutes()
    }

    /**
     * Descripción amigable del tiempo hasta el evento
     */
    fun timeUntilStartDescription(): String {
        val minutes = minutesUntilStart()
        return when {
            minutes < -60 -> "Empezó hace ${-minutes / 60} horas"
            minutes < 0 -> "Empezó hace ${-minutes} minutos"
            minutes == 0L -> "Empieza AHORA"
            minutes < 60 -> "En $minutes minutos"
            minutes < 1440 -> "En ${minutes / 60} horas"
            else -> "En ${minutes / 1440} días"
        }
    }
}