package com.chapelotas.app.domain.usecases

import com.chapelotas.app.domain.entities.CalendarEvent
import com.chapelotas.app.domain.repositories.AIRepository
import com.chapelotas.app.domain.repositories.CalendarRepository
import com.chapelotas.app.domain.repositories.PreferencesRepository
import javax.inject.Inject

/**
 * Caso de uso para obtener el resumen del día
 * Este es el corazón de Chapelotas - genera el mensaje matutino
 */
class GetDailySummaryUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val aiRepository: AIRepository,
    private val preferencesRepository: PreferencesRepository
) {
    /**
     * Ejecuta el caso de uso
     * @return El resumen del día generado por la IA
     */
    suspend operator fun invoke(): Result<DailySummaryResult> {
        return try {
            // 1. Obtener preferencias del usuario
            val preferences = preferencesRepository.getUserPreferences()

            // --- CORRECCIÓN AQUÍ: Se elimina el filtrado por IDs de calendario ---
            // 2. Obtener eventos de hoy
            val todayEvents = calendarRepository.getTodayEvents()

            // 3. Obtener eventos críticos
            val criticalEventIds = calendarRepository.getCriticalEventIds()
            val eventsWithCriticality = todayEvents.map { event ->
                event.copy(isCritical = event.id in criticalEventIds)
            }

            // 4. Generar resumen con IA
            val summary = aiRepository.generateDailySummary(
                todayEvents = eventsWithCriticality,
                isSarcastic = preferences.isSarcasticModeEnabled
            )

            // 5. Identificar próximo evento
            val nextEvent = findNextEvent(todayEvents)

            Result.success(
                DailySummaryResult(
                    summary = summary,
                    events = eventsWithCriticality,
                    hasEvents = todayEvents.isNotEmpty(),
                    nextEvent = nextEvent,
                    criticalEventsCount = eventsWithCriticality.count { it.isCritical }
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Encuentra el próximo evento del día
     */
    private fun findNextEvent(events: List<CalendarEvent>): CalendarEvent? {
        val now = java.time.LocalDateTime.now()
        return events
            .filter { it.startTime.isAfter(now) }
            .minByOrNull { it.startTime }
    }
}

/**
 * Resultado del resumen diario
 */
data class DailySummaryResult(
    val summary: String,
    val events: List<CalendarEvent>,
    val hasEvents: Boolean,
    val nextEvent: CalendarEvent?,
    val criticalEventsCount: Int
)