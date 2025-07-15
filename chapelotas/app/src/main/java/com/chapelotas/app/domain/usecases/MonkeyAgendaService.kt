package com.chapelotas.app.domain.usecases

import android.util.Log
import com.chapelotas.app.data.database.ChapelotasDatabase
import com.chapelotas.app.data.database.entities.ChatThread
import com.chapelotas.app.data.database.entities.ConversationLog
import com.chapelotas.app.data.database.entities.MonkeyAgenda
import com.chapelotas.app.domain.repositories.AIRepository
import com.chapelotas.app.domain.repositories.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servicio que procesa la MonkeyAgenda
 * Una entrada = Una acción = Sin loops
 */
@Singleton
class MonkeyAgendaService @Inject constructor(
    private val database: ChapelotasDatabase,
    private val notificationRepository: NotificationRepository,
    private val aiRepository: AIRepository,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "MonkeyAgenda"
    }

    /**
     * Procesa la siguiente acción pendiente
     */
    suspend fun processNextAction() {
        val now = LocalDateTime.now(ZoneId.systemDefault())

        // Obtener todas las acciones pendientes que ya deberían ejecutarse
        val pendingActions = database.monkeyAgendaDao().getPendingActionsUntilNow(now)

        if (pendingActions.isEmpty()) {
            Log.d(TAG, "No hay acciones pendientes")
            return
        }

        // Si hay muchas acciones viejas, hacer resumen
        val veryOldActions = pendingActions.filter {
            it.scheduledTime.isBefore(now.minusHours(2))
        }

        if (veryOldActions.size > 5) {
            Log.d(TAG, "Detectadas ${veryOldActions.size} acciones muy viejas. Generando resumen.")
            processMissedActionsSummary(veryOldActions)

            // Marcar las viejas como completadas
            veryOldActions.forEach {
                database.monkeyAgendaDao().markAsCompleted(it.id)
            }

            // Procesar solo las recientes
            val recentActions = pendingActions - veryOldActions.toSet()
            recentActions.take(3).forEach { processAction(it) }
        } else {
            // Caso normal: procesar máximo 3 acciones
            pendingActions.take(3).forEach { processAction(it) }
        }
    }

    /**
     * Procesa una acción individual
     */
    private suspend fun processAction(action: MonkeyAgenda) {
        Log.d(TAG, "Procesando acción: ${action.actionType} para ${action.scheduledTime}")

        // Marcar como procesando
        database.monkeyAgendaDao().markAsProcessing(action.id)

        try {
            when (action.actionType) {
                "NOTIFY_EVENT" -> processEventNotification(action)
                "DAILY_SUMMARY" -> processDailySummary(action)
                "IDLE_CHECK" -> processIdleCheck(action)
                "CLEANUP" -> processCleanup(action)
                else -> Log.w(TAG, "Tipo de acción desconocido: ${action.actionType}")
            }

            // Marcar como completada
            database.monkeyAgendaDao().markAsCompleted(action.id)

        } catch (e: Exception) {
            Log.e(TAG, "Error procesando acción", e)
            // En caso de error, marcarla como completada igual para evitar loops
            database.monkeyAgendaDao().markAsCompleted(action.id)
        }
    }

    /**
     * Procesa notificación de evento
     */
    private suspend fun processEventNotification(action: MonkeyAgenda) {
        val eventId = action.eventId ?: return
        val event = database.eventPlanDao().getEvent(eventId) ?: return

        // Verificar si el evento ya está completado
        if (event.resolutionStatus.name == "COMPLETED") {
            Log.d(TAG, "Evento ${event.title} ya está completado. Saltando notificación.")
            return
        }

        // Obtener o crear thread para el evento
        val threadId = "event_$eventId"
        var thread = database.chatThreadDao().getThread(threadId)

        if (thread == null) {
            thread = ChatThread(
                threadId = threadId,
                eventId = eventId,
                title = event.title,
                threadType = "EVENT",
                eventTime = event.startTime
            )
            database.chatThreadDao().insertOrUpdate(thread)
        }

        // Generar mensaje con AI
        val dayPlan = database.dayPlanDao().getTodayPlan()
        val minutesUntil = java.time.Duration.between(LocalDateTime.now(ZoneId.systemDefault()), event.startTime).toMinutes()

        val prompt = """
        Sos Chapelotas, secretaria ${if (dayPlan?.sarcasticMode == true) "sarcástica argentina" else "profesional"}.
        
        Evento: ${event.title}
        Hora: ${event.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))}
        ${event.location?.let { "Lugar: $it" } ?: ""}
        Faltan: $minutesUntil minutos
        
        Genera un recordatorio CORTO (máximo 2 líneas).
        ${if (event.isCritical) "ES CRÍTICO! Sé más enfático." else ""}
        """

        val message = aiRepository.callOpenAIForText(prompt, temperature = 0.7)

        // Guardar mensaje en el chat
        database.conversationLogDao().insert(
            ConversationLog(
                timestamp = LocalDateTime.now(ZoneId.systemDefault()),
                role = "assistant",
                content = message,
                eventId = eventId,
                threadId = threadId
            )
        )

        // Actualizar thread
        database.chatThreadDao().updateLastMessage(threadId, message, LocalDateTime.now(ZoneId.systemDefault()))

        // Mostrar notificación
        val notification = com.chapelotas.app.domain.entities.ChapelotasNotification(
            id = "agenda_${action.id}",
            eventId = event.calendarEventId,
            scheduledTime = action.scheduledTime,
            message = message,
            priority = if (event.isCritical)
                com.chapelotas.app.domain.entities.NotificationPriority.HIGH
            else com.chapelotas.app.domain.entities.NotificationPriority.NORMAL,
            type = com.chapelotas.app.domain.entities.NotificationType.EVENT_REMINDER
        )

        notificationRepository.showImmediateNotification(notification)
    }

    /**
     * Procesa resumen de acciones perdidas
     */
    private suspend fun processMissedActionsSummary(missedActions: List<MonkeyAgenda>) {
        val eventIds = missedActions.mapNotNull { it.eventId }.distinct()
        val events = eventIds.mapNotNull { database.eventPlanDao().getEvent(it) }

        if (events.isEmpty()) return

        val dayPlan = database.dayPlanDao().getTodayPlan()
        val eventsList = events.joinToString("\n") {
            "• ${it.title} (${it.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))})"
        }

        val prompt = """
        Sos Chapelotas, secretaria ${if (dayPlan?.sarcasticMode == true) "sarcástica argentina" else "profesional"}.
        
        Mientras estuve dormido/sin conexión se perdieron ${events.size} recordatorios:
        $eventsList
        
        Genera un mensaje CORTO (máximo 3 líneas) resumiendo la situación.
        ${if (dayPlan?.sarcasticMode == true) "Podés hacer un chiste sobre estar durmiendo." else ""}
        """

        val message = aiRepository.callOpenAIForText(prompt, temperature = 0.7)

        // Guardar en thread general
        database.conversationLogDao().insert(
            ConversationLog(
                timestamp = LocalDateTime.now(ZoneId.systemDefault()),
                role = "assistant",
                content = message,
                threadId = "general"
            )
        )

        // Actualizar thread general
        database.chatThreadDao().updateLastMessage("general", message)

        // Mostrar notificación
        val notification = com.chapelotas.app.domain.entities.ChapelotasNotification(
            id = "summary_${System.currentTimeMillis()}",
            eventId = 0L,
            scheduledTime = LocalDateTime.now(ZoneId.systemDefault()),
            message = message,
            priority = com.chapelotas.app.domain.entities.NotificationPriority.NORMAL,
            type = com.chapelotas.app.domain.entities.NotificationType.DAILY_SUMMARY
        )

        notificationRepository.showImmediateNotification(notification)
    }

    /**
     * Procesa resumen diario
     */
    private suspend fun processDailySummary(action: MonkeyAgenda) {
        // TODO: Implementar resumen diario
        Log.d(TAG, "Procesando resumen diario")
    }

    /**
     * Procesa chequeo de inactividad
     */
    private suspend fun processIdleCheck(action: MonkeyAgenda) {
        // TODO: Implementar chequeo de inactividad
        Log.d(TAG, "Procesando chequeo de inactividad")
    }

    /**
     * Procesa limpieza
     */
    private suspend fun processCleanup(action: MonkeyAgenda) {
        // Archivar threads completados viejos
        val cutoffDate = LocalDateTime.now(ZoneId.systemDefault()).minusDays(7)
        database.chatThreadDao().archiveOldCompleted(cutoffDate)

        // Limpiar agenda vieja
        database.monkeyAgendaDao().deleteOldCompleted(cutoffDate)

        Log.d(TAG, "Limpieza completada")
    }

    /**
     * Programa una acción en la agenda
     */
    suspend fun scheduleAction(
        scheduledTime: LocalDateTime,
        actionType: String,
        eventId: String? = null
    ): Long {
        val action = MonkeyAgenda(
            scheduledTime = scheduledTime,
            actionType = actionType,
            eventId = eventId
        )

        return database.monkeyAgendaDao().insert(action)
    }

    /**
     * Obtiene la próxima acción pendiente para programar alarma
     */
    suspend fun getNextPendingTime(): LocalDateTime? {
        val nextAction = database.monkeyAgendaDao().getNextPendingAction()
        return nextAction?.scheduledTime
    }
}