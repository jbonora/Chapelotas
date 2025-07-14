package com.chapelotas.app.domain.usecases

import com.chapelotas.app.domain.entities.ChapelotasNotification
import com.chapelotas.app.domain.entities.NotificationPriority
import com.chapelotas.app.domain.entities.NotificationType
import com.chapelotas.app.domain.repositories.CalendarRepository
import com.chapelotas.app.domain.repositories.NotificationRepository
import javax.inject.Inject

/**
 * Caso de uso para marcar/desmarcar un evento como crítico
 * Los eventos críticos reciben el tratamiento de "llamada simulada"
 */
class MarkEventAsCriticalUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val notificationRepository: NotificationRepository
) {
    /**
     * Marca o desmarca un evento como crítico
     * @param eventId ID del evento
     * @param isCritical true para marcar como crítico, false para desmarcar
     */
    suspend operator fun invoke(eventId: Long, isCritical: Boolean): Result<Unit> {
        return try {
            // 1. Actualizar estado en el repositorio
            calendarRepository.markEventAsCritical(eventId, isCritical)

            // 2. Si se marca como crítico, programar alerta especial
            if (isCritical) {
                // Obtener el evento para saber cuándo es
                val events = calendarRepository.getTodayEvents()
                val event = events.find { it.id == eventId }
                    ?: calendarRepository.getTomorrowEvents().find { it.id == eventId }

                event?.let {
                    // Crear notificación crítica 5 minutos antes
                    val criticalNotification = ChapelotasNotification(
                        id = java.util.UUID.randomUUID().toString(),
                        eventId = eventId,
                        scheduledTime = it.startTime.minusMinutes(5),
                        message = "🚨 ALERTA CRÍTICA: ${it.title} en 5 minutos!",
                        priority = NotificationPriority.CRITICAL,
                        type = NotificationType.CRITICAL_ALERT
                    )

                    notificationRepository.scheduleNotification(criticalNotification)
                }
            } else {
                // Si se desmarca, cancelar alertas críticas
                val notifications = notificationRepository.getNotificationsForEvent(eventId)
                notifications
                    .filter { it.type == NotificationType.CRITICAL_ALERT }
                    .forEach { notificationRepository.cancelNotification(it.id) }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}