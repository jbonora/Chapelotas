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
    val isCritical: Boolean = false,
    val hasBeenSummarized: Boolean = false
) {
    companion object {
        // --- CAMBIO CLAVE: Método para crear un evento vacío para el chequeo proactivo ---
        fun createEmpty(): CalendarEvent {
            return CalendarEvent(
                id = 0,
                title = "Chequeo proactivo",
                description = null,
                startTime = LocalDateTime.now(),
                endTime = LocalDateTime.now(),
                location = null,
                isAllDay = true,
                calendarId = 0,
                calendarName = "",
                isCritical = false,
                hasBeenSummarized = true
            )
        }
    }

    val durationInMinutes: Long
        get() = java.time.Duration.between(startTime, endTime).toMinutes()

    fun isToday(): Boolean {
        val today = LocalDateTime.now().toLocalDate()
        return startTime.toLocalDate() == today
    }

    fun isTomorrow(): Boolean {
        val tomorrow = LocalDateTime.now().toLocalDate().plusDays(1)
        return startTime.toLocalDate() == tomorrow
    }

    fun minutesUntilStart(): Long {
        return java.time.Duration.between(LocalDateTime.now(), startTime).toMinutes()
    }

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