package com.chapelotas.app.domain.usecases

import com.chapelotas.app.domain.entities.ChapelotasNotification
import com.chapelotas.app.domain.entities.NotificationPriority
import com.chapelotas.app.domain.entities.NotificationType
import com.chapelotas.app.domain.repositories.AIRepository
import com.chapelotas.app.domain.repositories.CalendarRepository
import com.chapelotas.app.domain.repositories.NotificationRepository
import com.chapelotas.app.domain.repositories.PreferencesRepository
import javax.inject.Inject

/**
 * Caso de uso para mostrar una alerta crítica inmediatamente
 * Esta es la "llamada simulada" que no se puede ignorar
 */
class ShowCriticalAlertUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val notificationRepository: NotificationRepository,
    private val aiRepository: AIRepository,
    private val preferencesRepository: PreferencesRepository
) {
    /**
     * Muestra una alerta crítica para un evento
     * @param eventId ID del evento crítico
     */
    suspend operator fun invoke(eventId: Long): Result<Unit> {
        return try {
            // 1. Obtener el evento
            val events = calendarRepository.getTodayEvents() + calendarRepository.getTomorrowEvents()
            val event = events.find { it.id == eventId }
                ?: return Result.failure(Exception("Evento no encontrado"))

            // 2. Obtener preferencias para el tono
            val preferences = preferencesRepository.getUserPreferences()

            // 3. Generar mensaje crítico con IA
            val criticalMessage = if (preferences.isSarcasticModeEnabled) {
                aiRepository.generateNotificationMessage(
                    event = event,
                    messageType = "CRITICAL_SARCASTIC",
                    isSarcastic = true,
                    additionalContext = "El usuario marcó esto como CRÍTICO. Sé MUY insistente y sarcástico."
                )
            } else {
                aiRepository.generateNotificationMessage(
                    event = event,
                    messageType = "CRITICAL_URGENT",
                    isSarcastic = false,
                    additionalContext = "Evento CRÍTICO. Sé firme pero respetuoso."
                )
            }

            // 4. Crear y mostrar notificación crítica inmediatamente
            val criticalNotification = ChapelotasNotification(
                id = java.util.UUID.randomUUID().toString(),
                eventId = eventId,
                scheduledTime = java.time.LocalDateTime.now(),
                message = criticalMessage,
                priority = NotificationPriority.CRITICAL,
                type = NotificationType.CRITICAL_ALERT
            )

            // 5. Mostrar inmediatamente (esto activará la pantalla completa)
            notificationRepository.showImmediateNotification(criticalNotification)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}