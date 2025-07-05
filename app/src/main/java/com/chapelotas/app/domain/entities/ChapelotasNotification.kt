package com.chapelotas.app.domain.entities

import java.time.LocalDateTime

/**
 * Representa una notificación programada por Chapelotas
 * La IA decide cuándo y cómo notificar cada evento
 */
data class ChapelotasNotification(
    val id: String,  // UUID único
    val eventId: Long,  // ID del evento del calendario
    val scheduledTime: LocalDateTime,  // Cuándo mostrar la notificación
    val message: String,  // Mensaje generado por la IA
    val priority: NotificationPriority,
    val type: NotificationType,
    val hasBeenShown: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Verifica si es momento de mostrar esta notificación
     */
    fun shouldShowNow(): Boolean {
        return !hasBeenShown && LocalDateTime.now().isAfter(scheduledTime)
    }

    /**
     * Minutos hasta mostrar la notificación
     */
    fun minutesUntilShow(): Long {
        return java.time.Duration.between(LocalDateTime.now(), scheduledTime).toMinutes()
    }
}

/**
 * Prioridad de la notificación
 */
enum class NotificationPriority {
    LOW,      // Notificación silenciosa
    NORMAL,   // Notificación estándar con sonido
    HIGH,     // Notificación insistente
    CRITICAL  // Alerta crítica (pantalla completa)
}

/**
 * Tipo de notificación según su propósito
 */
enum class NotificationType {
    DAILY_SUMMARY,      // Resumen del día
    TOMORROW_SUMMARY,   // Resumen de mañana
    EVENT_REMINDER,     // Recordatorio de evento próximo
    CRITICAL_ALERT,     // Alerta crítica (llamada simulada)
    PREPARATION_TIP,    // Consejo de preparación
    SARCASTIC_NUDGE    // Recordatorio sarcástico extra
}