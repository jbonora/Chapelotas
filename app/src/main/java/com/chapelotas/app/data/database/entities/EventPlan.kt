package com.chapelotas.app.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Plan para cada evento - equivalente a EventoConNotificaciones del JSON
 * Combina datos inmutables del calendario con configuración de Chapelotas
 */
@Entity(
    tableName = "event_plans",
    foreignKeys = [
        ForeignKey(
            entity = DayPlan::class,
            parentColumns = ["date"],
            childColumns = ["dayDate"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("dayDate"),
        Index("startTime"),
        Index("isCritical")
    ]
)
data class EventPlan(
    @PrimaryKey
    val eventId: String, // ID del calendario (inmutable)

    // Relación con el día
    val dayDate: LocalDate,

    // ===== DATOS INMUTABLES DEL CALENDARIO =====
    val calendarId: Long,
    val calendarEventId: Long, // ID numérico original
    val title: String,
    val description: String?,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val location: String?,
    val isAllDay: Boolean = false,

    // ===== DATOS MODIFICABLES POR USUARIO/IA =====
    val isCritical: Boolean = false,
    val distance: EventDistance = EventDistance.CERCA,
    val suggestedNotificationMinutes: String = "15", // "60,30,10" para múltiples

    // ===== ANÁLISIS DE IA =====
    val hasConflict: Boolean = false,
    val aiImportanceScore: Float? = null, // 0.0 a 1.0
    val aiSuggestedCritical: Boolean = false,
    val aiReason: String? = null,

    // ===== ESTADO =====
    val notificationsSent: Int = 0,
    val lastNotificationTime: LocalDateTime? = null,
    val userDismissed: Boolean = false,
    val userSnoozedUntil: LocalDateTime? = null,

    // Metadata
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Lista de minutos para notificaciones
     */
    fun getNotificationMinutesList(): List<Int> {
        return suggestedNotificationMinutes
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .sorted()
            .reversed() // Mayor a menor (60, 30, 15)
    }

    /**
     * Calcula minutos hasta el evento
     */
    fun minutesUntilStart(): Long {
        return java.time.Duration
            .between(LocalDateTime.now(), startTime)
            .toMinutes()
    }

    /**
     * Determina si el evento ya empezó
     */
    fun hasStarted(): Boolean {
        return LocalDateTime.now().isAfter(startTime)
    }

    /**
     * Determina si el evento ya terminó
     */
    fun hasEnded(): Boolean {
        return LocalDateTime.now().isAfter(endTime)
    }

    /**
     * Duración en minutos
     */
    fun durationMinutes(): Long {
        return java.time.Duration
            .between(startTime, endTime)
            .toMinutes()
    }
}