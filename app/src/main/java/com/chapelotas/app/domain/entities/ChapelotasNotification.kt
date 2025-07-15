package com.chapelotas.app.domain.entities

import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Representa una notificación programada por Chapelotas
 * La IA decide cuándo y cómo notificar cada evento
 */
data class ChapelotasNotification(
    val id: String,
    val eventId: Long,
    val scheduledTime: LocalDateTime,
    val message: String,
    val priority: NotificationPriority,
    val type: NotificationType,
    val hasBeenShown: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneId.systemDefault())
) {
    fun shouldShowNow(): Boolean {
        return !hasBeenShown && LocalDateTime.now(ZoneId.systemDefault()).isAfter(scheduledTime)
    }

    fun minutesUntilShow(): Long {
        return java.time.Duration.between(LocalDateTime.now(ZoneId.systemDefault()), scheduledTime).toMinutes()
    }
}

/**
 * Prioridad de la notificación
 */
enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
}

/**
 * Tipo de notificación según su propósito
 */
enum class NotificationType {
    DAILY_SUMMARY,
    TOMORROW_SUMMARY,
    EVENT_REMINDER,
    CRITICAL_ALERT,
    PREPARATION_TIP,
    SARCASTIC_NUDGE,
    // --- CORRECCIÓN AQUÍ ---
    CONFLICT_ALERT
    // --- FIN DE LA CORRECCIÓN ---
}