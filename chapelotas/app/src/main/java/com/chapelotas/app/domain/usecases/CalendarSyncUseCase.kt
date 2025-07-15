package com.chapelotas.app.domain.usecases

import android.util.Log
import com.chapelotas.app.data.database.ChapelotasDatabase
import com.chapelotas.app.data.database.entities.EventPlan
import com.chapelotas.app.domain.events.ChapelotasEvent
import com.chapelotas.app.domain.events.ChapelotasEventBus
import com.chapelotas.app.domain.repositories.CalendarRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sincroniza eventos del calendario del dispositivo con la base de datos local
 */
@Singleton
class CalendarSyncUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val database: ChapelotasDatabase,
    private val eventBus: ChapelotasEventBus
) {
    companion object {
        private const val TAG = "CalendarSync"
    }

    suspend fun syncTodayEvents() {
        try {
            Log.d(TAG, "Iniciando sincronización de eventos de hoy")

            // Obtener eventos del calendario del dispositivo
            val calendarEvents = calendarRepository.getTodayEvents()
            Log.d(TAG, "Encontrados ${calendarEvents.size} eventos en el calendario")

            // Obtener eventos existentes en nuestra BD
            val existingEvents = database.eventPlanDao().getEventsByDate(LocalDate.now())
            val existingEventIds = existingEvents.map { it.calendarEventId }.toSet()

            var newEventsCount = 0
            var updatedEventsCount = 0

            // Procesar cada evento del calendario
            calendarEvents.forEach { calendarEvent ->
                if (calendarEvent.id !in existingEventIds) {
                    // Evento nuevo - crear EventPlan
                    val eventPlan = EventPlan(
                        eventId = "cal_${calendarEvent.id}",
                        calendarEventId = calendarEvent.id,
                        dayDate = LocalDate.now(),
                        title = calendarEvent.title,
                        startTime = calendarEvent.startTime,
                        endTime = calendarEvent.endTime,
                        location = calendarEvent.location,
                        description = calendarEvent.description,
                        isAllDay = calendarEvent.isAllDay,
                        isCritical = false,
                        hasConflict = false,
                        distance = com.chapelotas.app.data.database.entities.EventDistance.CERCA,
                        resolutionStatus = com.chapelotas.app.data.database.entities.EventResolutionStatus.PENDING,
                        aiPlanStatus = "AUTO_APPROVED",
                        userModified = false
                    )

                    database.eventPlanDao().insert(eventPlan)
                    newEventsCount++

                    Log.d(TAG, "Nuevo evento sincronizado: ${eventPlan.title}")
                } else {
                    // Evento existente - actualizar si cambió
                    val existingEvent = existingEvents.find { it.calendarEventId == calendarEvent.id }
                    if (existingEvent != null && hasEventChanged(existingEvent, calendarEvent)) {
                        val updatedEvent = existingEvent.copy(
                            title = calendarEvent.title,
                            startTime = calendarEvent.startTime,
                            endTime = calendarEvent.endTime,
                            location = calendarEvent.location,
                            description = calendarEvent.description,
                            updatedAt = java.time.LocalDateTime.now(ZoneId.systemDefault())
                        )

                        database.eventPlanDao().update(updatedEvent)
                        updatedEventsCount++

                        Log.d(TAG, "Evento actualizado: ${updatedEvent.title}")
                    }
                }
            }

            // Detectar eventos eliminados del calendario
            val calendarEventIds = calendarEvents.map { it.id }.toSet()
            val deletedEvents = existingEvents.filter {
                it.calendarEventId !in calendarEventIds &&
                        it.resolutionStatus != com.chapelotas.app.data.database.entities.EventResolutionStatus.COMPLETED
            }

            deletedEvents.forEach { event ->
                // Marcar como cancelado en lugar de eliminar
                database.eventPlanDao().updateResolutionStatus(
                    event.eventId,
                    com.chapelotas.app.data.database.entities.EventResolutionStatus.CANCELLED
                )
                Log.d(TAG, "Evento cancelado: ${event.title}")
            }

            // Emitir evento de sincronización completada
            eventBus.emit(ChapelotasEvent.CalendarSyncCompleted(
                newEvents = newEventsCount,
                updatedEvents = updatedEventsCount,
                deletedEvents = deletedEvents.size
            ))

            Log.d(TAG, "Sincronización completada: $newEventsCount nuevos, $updatedEventsCount actualizados, ${deletedEvents.size} cancelados")

        } catch (e: Exception) {
            Log.e(TAG, "Error durante sincronización", e)
            eventBus.emit(ChapelotasEvent.MonkeyError(
                error = "Error sincronizando calendario: ${e.message}",
                willRetry = false,
                retryInSeconds = 0
            ))
        }
    }

    private fun hasEventChanged(
        existingEvent: EventPlan,
        calendarEvent: com.chapelotas.app.domain.entities.CalendarEvent
    ): Boolean {
        return existingEvent.title != calendarEvent.title ||
                existingEvent.startTime != calendarEvent.startTime ||
                existingEvent.endTime != calendarEvent.endTime ||
                existingEvent.location != calendarEvent.location ||
                existingEvent.description != calendarEvent.description
    }
}