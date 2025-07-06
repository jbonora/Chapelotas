package com.chapelotas.app.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Conflictos detectados entre eventos
 * Permite tracking de múltiples conflictos y su resolución
 */
@Entity(
    tableName = "event_conflicts",
    indices = [
        Index("event1Id"),
        Index("event2Id"),
        Index("resolved"),
        Index("date")
    ]
)
data class EventConflict(
    @PrimaryKey(autoGenerate = true)
    val conflictId: Long = 0,

    // Eventos en conflicto
    val event1Id: String,
    val event1Title: String,
    val event1Time: String, // HH:mm

    val event2Id: String,
    val event2Title: String,
    val event2Time: String, // HH:mm

    // Detalles del conflicto
    val type: ConflictType,
    val date: LocalDateTime,
    val overlapMinutes: Int? = null, // Para solapamientos
    val distanceMinutes: Int? = null, // Tiempo entre eventos

    // Mensaje de IA
    val aiMessage: String,
    val severity: ConflictSeverity = ConflictSeverity.MEDIUM,

    // Estado
    val resolved: Boolean = false,
    val resolvedAt: LocalDateTime? = null,
    val resolutionNote: String? = null,
    val userNotified: Boolean = false,

    // Metadata
    val detectedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Genera mensaje user-friendly del conflicto
     */
    fun getUserMessage(): String {
        return when (type) {
            ConflictType.OVERLAPPING ->
                "⚠️ '$event1Title' y '$event2Title' se superponen por $overlapMinutes minutos"

            ConflictType.TOO_CLOSE ->
                "⏰ Solo tenés $distanceMinutes minutos entre '$event1Title' y '$event2Title'"

            ConflictType.SAME_LOCATION ->
                "📍 Mismo lugar al mismo tiempo: '$event1Title' y '$event2Title'"
        }
    }

    /**
     * Verifica si involucra a un evento específico
     */
    fun involvesEvent(eventId: String): Boolean {
        return event1Id == eventId || event2Id == eventId
    }
}

enum class ConflictSeverity {
    LOW,      // Advertencia menor
    MEDIUM,   // Requiere atención
    HIGH      // Crítico, imposible cumplir
}