package com.chapelotas.app.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Log de todas las notificaciones mostradas
 * Para analytics y mejorar la IA con el tiempo
 */
@Entity(
    tableName = "notification_logs",
    indices = [
        Index("eventId"),
        Index("shownAt"),
        Index("userAction")
    ]
)
data class NotificationLog(
    @PrimaryKey(autoGenerate = true)
    val logId: Long = 0,

    // Referencia a la notificación
    val notificationId: Long,
    val eventId: String,
    val eventTitle: String,

    // Cuándo se mostró
    val shownAt: LocalDateTime,
    val minutesBeforeEvent: Int,

    // Tipo y contenido
    val type: NotificationType,
    val priority: NotificationPriority,
    val message: String,
    val wasAiGenerated: Boolean = true,

    // Respuesta del usuario
    val userAction: UserAction? = null,
    val actionTimestamp: LocalDateTime? = null,
    val responseTimeSeconds: Long? = null, // Tiempo hasta que el usuario reaccionó

    // Contexto adicional
    val wasSnoozed: Boolean = false,
    val snoozeCount: Int = 0,
    val deviceWasLocked: Boolean = false,
    val appWasInForeground: Boolean = false,

    // Para análisis
    val eventWasAttended: Boolean? = null, // Se puede actualizar después
    val wasEffective: Boolean? = null // La notificación cumplió su propósito
) {
    /**
     * Calcula efectividad basada en la acción del usuario
     */
    fun calculateEffectiveness(): Boolean {
        return when (userAction) {
            UserAction.OPENED, UserAction.MARKED_DONE -> true
            UserAction.DISMISSED, UserAction.IGNORED -> false
            UserAction.SNOOZED -> wasSnoozed && snoozeCount <= 2
            null -> false
        }
    }
}

enum class UserAction {
    OPENED,      // Abrió la app desde la notificación
    DISMISSED,   // Descartó la notificación
    SNOOZED,     // Pospuso
    MARKED_DONE, // Marcó como completado
    IGNORED      // No interactuó (timeout)
}