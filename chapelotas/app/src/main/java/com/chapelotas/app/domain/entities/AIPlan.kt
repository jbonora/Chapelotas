package com.chapelotas.app.domain.entities

import java.time.LocalDateTime

/**
 * Plan de comunicación generado por la IA
 * Define cuándo y cómo Chapelotas debe notificar sobre los eventos
 */
data class AIPlan(
    val id: String,  // UUID único
    val generatedAt: LocalDateTime,
    val eventsAnalyzed: List<Long>,  // IDs de eventos considerados
    val notifications: List<PlannedNotification>,
    val aiInsights: String?,  // Comentarios de la IA sobre el día
    val suggestedFocus: String?  // Sugerencia de en qué enfocarse
) {
    /**
     * Obtiene todas las notificaciones pendientes
     */
    fun getPendingNotifications(): List<PlannedNotification> {
        return notifications.filter { !it.hasBeenScheduled }
    }

    /**
     * Cuenta las alertas críticas planeadas
     */
    fun criticalAlertsCount(): Int {
        return notifications.count { it.priority == NotificationPriority.CRITICAL }
    }
}

/**
 * Notificación planeada por la IA (antes de ser programada)
 */
data class PlannedNotification(
    val eventId: Long,
    val suggestedTime: LocalDateTime,
    val suggestedMessage: String,
    val priority: NotificationPriority,
    val type: NotificationType,
    val rationale: String?,  // Por qué la IA sugiere esta notificación
    var hasBeenScheduled: Boolean = false
) {
    /**
     * Convierte a ChapelotasNotification para programar
     */
    fun toScheduledNotification(): ChapelotasNotification {
        return ChapelotasNotification(
            id = java.util.UUID.randomUUID().toString(),
            eventId = eventId,
            scheduledTime = suggestedTime,
            message = suggestedMessage,
            priority = priority,
            type = type
        )
    }
}