package com.chapelotas.app.domain.usecases

import com.chapelotas.app.domain.entities.CalendarEvent
import com.chapelotas.app.domain.repositories.AIRepository
import com.chapelotas.app.domain.repositories.CalendarRepository
import com.chapelotas.app.domain.repositories.PreferencesRepository
import javax.inject.Inject

/**
 * Caso de uso para obtener el resumen de mañana
 * Se ejecuta por la noche para preparar al usuario
 */
class GetTomorrowSummaryUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val aiRepository: AIRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(): Result<TomorrowSummaryResult> {
        return try {
            // 1. Obtener preferencias
            val preferences = preferencesRepository.getUserPreferences()

            // --- CORRECCIÓN AQUÍ: Se elimina el filtrado por IDs de calendario ---
            // 2. Obtener eventos de mañana
            val tomorrowEvents = calendarRepository.getTomorrowEvents()

            // 3. Obtener eventos críticos
            val criticalEventIds = calendarRepository.getCriticalEventIds()
            val eventsWithCriticality = tomorrowEvents.map { event ->
                event.copy(isCritical = event.id in criticalEventIds)
            }

            // 4. Obtener contexto de hoy (opcional)
            val todayEvents = calendarRepository.getTodayEvents()
            val todayContext = if (todayEvents.isNotEmpty()) {
                "Hoy tuviste ${todayEvents.size} eventos"
            } else {
                "Hoy fue un día tranquilo"
            }

            // 5. Generar resumen con IA
            val summary = aiRepository.generateTomorrowSummary(
                tomorrowEvents = eventsWithCriticality,
                todayContext = todayContext,
                isSarcastic = preferences.isSarcasticModeEnabled
            )

            // 6. Identificar primer evento
            val firstEvent = tomorrowEvents.minByOrNull { it.startTime }

            Result.success(
                TomorrowSummaryResult(
                    summary = summary,
                    events = eventsWithCriticality,
                    hasEvents = tomorrowEvents.isNotEmpty(),
                    firstEvent = firstEvent,
                    criticalEventsCount = eventsWithCriticality.count { it.isCritical },
                    needsEarlyAlarm = firstEvent?.startTime?.hour ?: 24 < 9
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Resultado del resumen de mañana
 */
data class TomorrowSummaryResult(
    val summary: String,
    val events: List<CalendarEvent>,
    val hasEvents: Boolean,
    val firstEvent: CalendarEvent?,
    val criticalEventsCount: Int,
    val needsEarlyAlarm: Boolean  // Si el primer evento es antes de las 9 AM
)