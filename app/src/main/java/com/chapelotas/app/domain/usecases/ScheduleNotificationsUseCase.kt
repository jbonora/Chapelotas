package com.chapelotas.app.domain.usecases

import com.chapelotas.app.domain.entities.CalendarEvent
import com.chapelotas.app.domain.entities.ChapelotasNotification
import com.chapelotas.app.domain.entities.UserPreferences
import com.chapelotas.app.domain.repositories.AIRepository
import com.chapelotas.app.domain.repositories.CalendarRepository
import com.chapelotas.app.domain.repositories.NotificationRepository
import com.chapelotas.app.domain.repositories.PreferencesRepository
import javax.inject.Inject

/**
 * Caso de uso para programar notificaciones inteligentes
 * La IA decide cuándo y cómo notificar cada evento
 */
class ScheduleNotificationsUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val notificationRepository: NotificationRepository,
    private val aiRepository: AIRepository,
    private val preferencesRepository: PreferencesRepository
) {
    /**
     * Programa notificaciones para los eventos del día
     */
    suspend operator fun invoke(forTomorrow: Boolean = false): Result<ScheduleResult> {
        return try {
            // 1. Obtener preferencias
            val preferences = preferencesRepository.getUserPreferences()

            // 2. Obtener eventos
            val events = if (forTomorrow) {
                calendarRepository.getTomorrowEvents(preferences.preferredCalendars.takeIf { it.isNotEmpty() })
            } else {
                calendarRepository.getTodayEvents(preferences.preferredCalendars.takeIf { it.isNotEmpty() })
            }

            if (events.isEmpty()) {
                return Result.success(ScheduleResult(0, emptyList()))
            }

            // 3. Obtener eventos críticos
            val criticalEventIds = calendarRepository.getCriticalEventIds()
            val eventsWithCriticality = events.map { event ->
                event.copy(isCritical = event.id in criticalEventIds)
            }

            // 4. Cancelar notificaciones existentes para estos eventos
            events.forEach { event ->
                notificationRepository.cancelNotificationsForEvent(event.id)
            }

            // 5. Generar plan de comunicación con IA
            val aiPlan = aiRepository.generateCommunicationPlan(
                events = eventsWithCriticality,
                userContext = buildUserContext(preferences)
            )

            // 6. Convertir plan a notificaciones programables
            val notifications = aiPlan.notifications.map { planned ->
                planned.toScheduledNotification()
            }

            // 7. Programar todas las notificaciones
            notificationRepository.scheduleMultipleNotifications(notifications)

            Result.success(
                ScheduleResult(
                    notificationsScheduled = notifications.size,
                    notifications = notifications
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Construye contexto del usuario para la IA
     */
    private fun buildUserContext(preferences: UserPreferences): String {
        return buildString {
            appendLine("Modo sarcástico: ${if (preferences.isSarcasticModeEnabled) "ACTIVADO" else "desactivado"}")
            appendLine("Recordatorios: ${preferences.minutesBeforeEventForReminder} minutos antes")
            appendLine("Usuario nuevo: ${if (preferences.isFirstTimeUser) "SÍ" else "NO"}")
        }
    }
}

/**
 * Resultado de la programación
 */
data class ScheduleResult(
    val notificationsScheduled: Int,
    val notifications: List<ChapelotasNotification>
)