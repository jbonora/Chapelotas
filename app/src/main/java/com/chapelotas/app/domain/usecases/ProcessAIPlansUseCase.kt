package com.chapelotas.app.domain.usecases

import android.util.Log
import com.chapelotas.app.data.database.ChapelotasDatabase
import com.chapelotas.app.data.database.entities.ConflictSeverity
import com.chapelotas.app.data.database.entities.ConflictType
import com.chapelotas.app.data.database.entities.EventConflict
import com.chapelotas.app.data.database.entities.EventPlan
import com.chapelotas.app.domain.entities.CalendarEvent
import com.chapelotas.app.domain.repositories.AIRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProcessAIPlansUseCase @Inject constructor(
    private val database: ChapelotasDatabase,
    private val aiRepository: AIRepository,
    private val unifiedMonkey: UnifiedMonkeyService
) {
    companion object {
        private const val TAG = "ProcessAIPlans"
    }

    /**
     * Procesa una lista de eventos del calendario con la AI Estricta
     * y actualiza los EventPlan con las sugerencias de la AI
     */
    suspend fun processEventsWithAI(
        calendarEvents: List<CalendarEvent>,
        existingPlans: List<EventPlan>
    ) = withContext(Dispatchers.IO) {
        try {
            if (calendarEvents.isEmpty()) {
                Log.d(TAG, "No hay eventos para procesar con AI")
                return@withContext
            }

            Log.d(TAG, "Procesando ${calendarEvents.size} eventos con AI Estricta")

            // 1. Llamar a la AI Estricta para analizar todos los eventos
            val aiPlans = aiRepository.createDailyPlanBatch(calendarEvents)

            if (aiPlans.isEmpty()) {
                Log.w(TAG, "AI no devolvió planes. Usando configuración por defecto.")
                return@withContext
            }

            Log.d(TAG, "AI devolvió ${aiPlans.size} planes")

            // 2. Procesar cada plan de la AI
            for ((eventId, aiPlan) in aiPlans) {
                val eventIdWithDate = "${eventId}_${LocalDate.now()}"
                val existingPlan = existingPlans.find { it.calendarEventId == eventId }

                if (existingPlan != null) {
                    // Solo actualizar si el usuario NO ha modificado manualmente
                    if (!existingPlan.userModified) {
                        Log.d(TAG, "Actualizando evento $eventId con sugerencias de AI")

                        // Actualizar criticidad si la AI lo sugiere
                        if (aiPlan.isCritical != existingPlan.isCritical) {
                            // Usamos el método update completo para no marcar userModified
                            val updatedPlan = existingPlan.copy(
                                isCritical = aiPlan.isCritical,
                                updatedAt = LocalDateTime.now(ZoneId.systemDefault())
                            )
                            database.eventPlanDao().update(updatedPlan)
                        }

                        // Procesar conflictos detectados por la AI
                        if (aiPlan.conflict?.hasConflict == true) {
                            processConflict(existingPlan, aiPlan.conflict)
                        }

                        // Re-planificar notificaciones con la nueva información
                        val updatedPlan = database.eventPlanDao().getEvent(eventIdWithDate)
                        updatedPlan?.let {
                            unifiedMonkey.planNotificationsForEvent(it)
                        }
                    } else {
                        Log.d(TAG, "Evento $eventId fue modificado por usuario. Saltando AI.")
                    }
                }
            }

            Log.d(TAG, "Procesamiento de AI completado")

        } catch (e: Exception) {
            Log.e(TAG, "Error procesando eventos con AI", e)
            // En caso de error, continuar sin AI (fail silently)
        }
    }

    /**
     * Procesa un conflicto detectado por la AI
     */
    private suspend fun processConflict(
        eventPlan: EventPlan,
        conflictInfo: com.chapelotas.app.data.ai.AIConflictInfo
    ) {
        if (!conflictInfo.hasConflict || conflictInfo.conflictingEventId == null) {
            return
        }

        val conflictingEventId = "${conflictInfo.conflictingEventId}_${LocalDate.now()}"
        val conflictingEvent = database.eventPlanDao().getEvent(conflictingEventId) ?: return

        val conflictType = when (conflictInfo.conflictType) {
            "OVERLAP" -> ConflictType.OVERLAPPING
            "TOO_CLOSE" -> ConflictType.TOO_CLOSE
            else -> ConflictType.OVERLAPPING
        }

        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        val conflict = EventConflict(
            event1Id = eventPlan.eventId,
            event1Title = eventPlan.title,
            event1Time = eventPlan.startTime.format(timeFormatter),
            event2Id = conflictingEventId,
            event2Title = conflictingEvent.title,
            event2Time = conflictingEvent.startTime.format(timeFormatter),
            type = conflictType,
            date = LocalDateTime.now(ZoneId.systemDefault()),
            overlapMinutes = if (conflictType == ConflictType.OVERLAPPING) {
                calculateOverlapMinutes(eventPlan, conflictingEvent)
            } else null,
            distanceMinutes = if (conflictType == ConflictType.TOO_CLOSE) {
                calculateDistanceMinutes(eventPlan, conflictingEvent)
            } else null,
            aiMessage = "Conflicto detectado por AI Estricta: ${getConflictMessage(conflictType, eventPlan, conflictingEvent)}",
            severity = determineSeverity(eventPlan, conflictingEvent, conflictType),
            resolved = false,
            userNotified = false,
            detectedAt = LocalDateTime.now(ZoneId.systemDefault())
        )

        try {
            database.conflictDao().insert(conflict)

            // Marcar ambos eventos como en conflicto
            database.eventPlanDao().update(
                eventPlan.copy(hasConflict = true, updatedAt = LocalDateTime.now(ZoneId.systemDefault()))
            )

            database.eventPlanDao().update(
                conflictingEvent.copy(hasConflict = true, updatedAt = LocalDateTime.now(ZoneId.systemDefault()))
            )

            Log.d(TAG, "Conflicto registrado: ${eventPlan.title} con ${conflictingEvent.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando conflicto", e)
        }
    }

    private fun calculateOverlapMinutes(event1: EventPlan, event2: EventPlan): Int {
        val overlapStart = maxOf(event1.startTime, event2.startTime)
        val overlapEnd = minOf(event1.endTime, event2.endTime)
        return if (overlapStart < overlapEnd) {
            Duration.between(overlapStart, overlapEnd).toMinutes().toInt()
        } else 0
    }

    private fun calculateDistanceMinutes(event1: EventPlan, event2: EventPlan): Int {
        return if (event1.endTime <= event2.startTime) {
            Duration.between(event1.endTime, event2.startTime).toMinutes().toInt()
        } else {
            Duration.between(event2.endTime, event1.startTime).toMinutes().toInt()
        }
    }

    private fun getConflictMessage(type: ConflictType, event1: EventPlan, event2: EventPlan): String {
        return when (type) {
            ConflictType.OVERLAPPING -> "Los eventos se superponen en el horario"
            ConflictType.TOO_CLOSE -> "Hay muy poco tiempo entre los eventos para trasladarse"
            ConflictType.SAME_LOCATION -> "Ambos eventos están en el mismo lugar al mismo tiempo"
        }
    }

    private fun determineSeverity(event1: EventPlan, event2: EventPlan, type: ConflictType): ConflictSeverity {
        return when {
            event1.isCritical || event2.isCritical -> ConflictSeverity.HIGH
            type == ConflictType.OVERLAPPING -> ConflictSeverity.HIGH
            type == ConflictType.TOO_CLOSE && (event1.location != null || event2.location != null) -> ConflictSeverity.MEDIUM
            else -> ConflictSeverity.LOW
        }
    }
}