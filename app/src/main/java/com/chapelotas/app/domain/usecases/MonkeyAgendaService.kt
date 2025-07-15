package com.chapelotas.app.domain.usecases

import android.util.Log
import com.chapelotas.app.data.database.ChapelotasDatabase
import com.chapelotas.app.data.database.entities.ChatThread
import com.chapelotas.app.data.database.entities.ConversationLog
import com.chapelotas.app.data.database.entities.EventPlan
import com.chapelotas.app.data.database.entities.EventResolutionStatus
import com.chapelotas.app.data.database.entities.MonkeyAgenda
import com.chapelotas.app.domain.repositories.AIRepository
import com.chapelotas.app.domain.repositories.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
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
            // Verificar si es primera vez del día
            checkFirstLaunchOfDay()
            return
        }

        // Acciones de más de 2 horas de antigüedad
        val veryOldActions = pendingActions.filter {
            it.scheduledTime.isBefore(now.minusHours(2))
        }

        Log.d(TAG, """
            🔍 DEBUG processNextAction:
            - Acciones pendientes totales: ${pendingActions.size}
            - Acciones muy viejas (>2h): ${veryOldActions.size}
            - Umbral para resumen: ${veryOldActions.isNotEmpty()}
        """.trimIndent())

        // Si hay CUALQUIER acción muy vieja, hacer resumen
        if (veryOldActions.isNotEmpty()) {
            Log.d(TAG, "Detectadas ${veryOldActions.size} acciones viejas. Generando resumen.")
            processMissedActionsSummary(veryOldActions)

            // Marcar las viejas como completadas
            veryOldActions.forEach {
                database.monkeyAgendaDao().markAsCompleted(it.id)
            }

            // Procesar solo las recientes (si las hay)
            val recentActions = pendingActions - veryOldActions.toSet()
            if (recentActions.isNotEmpty()) {
                recentActions.take(2).forEach { processAction(it) }
            }
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

        // Verificar horario de trabajo (excepto para CLEANUP que puede correr siempre)
        if (action.actionType != "CLEANUP") {
            val dayPlan = database.dayPlanDao().getTodayPlan()
            if (dayPlan != null && !dayPlan.is24hMode) {
                val now = LocalTime.now()
                if (now.isBefore(dayPlan.workStartTime) || now.isAfter(dayPlan.workEndTime)) {
                    Log.d(TAG, "🐵 Fuera de horario laboral (${dayPlan.workStartTime} - ${dayPlan.workEndTime}). Saltando acción.")
                    database.monkeyAgendaDao().markAsCompleted(action.id)
                    return
                }
            }
        }

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
        val minutesUntil = Duration.between(LocalDateTime.now(ZoneId.systemDefault()), event.startTime).toMinutes()

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
        Log.d(TAG, "Procesando resumen diario")
        val events = database.eventPlanDao().getEventsByDate(LocalDate.now())
        if (events.isNotEmpty()) {
            generateDailySummaryMessage(events)
        }
    }

    /**
     * Procesa chequeo de inactividad
     */
    private suspend fun processIdleCheck(action: MonkeyAgenda) {
        val dayPlan = database.dayPlanDao().getTodayPlan() ?: return

        // Solo en modo sarcástico
        if (!dayPlan.sarcasticMode) {
            Log.d(TAG, "Modo sarcástico desactivado, saltando chequeo de inactividad")
            return
        }

        // Verificar horario (respetar modo 24h)
        if (!dayPlan.is24hMode) {
            val now = LocalTime.now()
            if (now.isBefore(dayPlan.workStartTime) || now.isAfter(dayPlan.workEndTime)) {
                Log.d(TAG, "🐵 Fuera de horario laboral (${dayPlan.workStartTime} - ${dayPlan.workEndTime}), saltando chequeo")
                return
            }
        }

        // Calcular tiempo de inactividad
        val lastActivity = getLastUserActivity()
        val idleMinutes = if (lastActivity != null) {
            Duration.between(lastActivity, LocalDateTime.now(ZoneId.systemDefault())).toMinutes()
        } else {
            60 // Si no hay actividad previa, asumimos 1 hora
        }

        Log.d(TAG, "🕐 Tiempo de inactividad: $idleMinutes minutos")

        // Solo molestar si han pasado más de 45 minutos sin actividad
        if (idleMinutes >= 45) {
            generateSarcasticIdleMessage(idleMinutes)
        }
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
     * Verificar si es la primera vez del día
     */
    private suspend fun checkFirstLaunchOfDay() {
        val dayPlan = database.dayPlanDao().getTodayPlan() ?: return

        // Si es la primera vez del día y hay eventos
        val todayEvents = database.eventPlanDao().getEventsByDate(LocalDate.now())
        if (todayEvents.isNotEmpty()) {
            val pendingEvents = todayEvents.filter {
                it.resolutionStatus != EventResolutionStatus.COMPLETED
            }

            if (pendingEvents.isNotEmpty()) {
                generateDailySummaryMessage(pendingEvents)
            }
        }
    }

    /**
     * Generar mensaje de bienvenida/resumen del día
     */
    private suspend fun generateDailySummaryMessage(events: List<EventPlan>) {
        val dayPlan = database.dayPlanDao().getTodayPlan() ?: return

        val eventsList = events
            .sortedBy { it.startTime }
            .joinToString("\n") {
                val timeStr = it.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                val criticalEmoji = if (it.isCritical) "🚨" else ""
                "• $timeStr - ${it.title} $criticalEmoji"
            }

        val prompt = """
        Sos Chapelotas, secretaria ${if (dayPlan.sarcasticMode) "sarcástica argentina" else "profesional"}.
        
        Es la primera vez que me abren hoy. Tengo que dar un resumen del día.
        
        Eventos para hoy (${events.size} en total):
        $eventsList
        
        Genera un mensaje de bienvenida/resumen CORTO (máximo 3 líneas).
        ${if (dayPlan.sarcasticMode) "Podés ser sarcástico sobre la cantidad de cosas que tiene que hacer." else "Sé profesional y motivador."}
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
            id = "daily_welcome_${System.currentTimeMillis()}",
            eventId = 0L,
            scheduledTime = LocalDateTime.now(ZoneId.systemDefault()),
            message = message,
            priority = com.chapelotas.app.domain.entities.NotificationPriority.NORMAL,
            type = com.chapelotas.app.domain.entities.NotificationType.DAILY_SUMMARY
        )

        notificationRepository.showImmediateNotification(notification)

        Log.d(TAG, "📅 Mensaje de bienvenida del día enviado")
    }

    /**
     * Obtiene la última actividad del usuario
     */
    private suspend fun getLastUserActivity(): LocalDateTime? {
        // Buscar la última interacción del usuario en el chat
        val lastUserMessage = database.conversationLogDao().getRecentHistory(50)
            .filter { it.role == "user" }
            .maxByOrNull { it.timestamp }

        // Buscar la última modificación de eventos
        val lastEventUpdate = database.eventPlanDao().getEventsByDate(LocalDate.now())
            .mapNotNull { it.updatedAt }
            .maxOrNull()

        // Retornar la más reciente entre ambas
        return listOfNotNull(lastUserMessage?.timestamp, lastEventUpdate).maxOrNull()
    }

    /**
     * Genera mensaje sarcástico por inactividad
     */
    private suspend fun generateSarcasticIdleMessage(idleMinutes: Long) {
        val hoursIdle = idleMinutes / 60
        val minutesRemainder = idleMinutes % 60

        // Contexto para el prompt
        val timeString = when {
            hoursIdle > 0 -> "$hoursIdle hora${if(hoursIdle > 1) "s" else ""} y $minutesRemainder minutos"
            else -> "$idleMinutes minutos"
        }

        // Buscar próximos eventos
        val nextEvent = database.eventPlanDao().getEventsByDate(LocalDate.now())
            .filter {
                it.startTime.isAfter(LocalDateTime.now(ZoneId.systemDefault())) &&
                        it.resolutionStatus != EventResolutionStatus.COMPLETED
            }
            .minByOrNull { it.startTime }

        val nextEventContext = nextEvent?.let {
            "El próximo evento es '${it.title}' a las ${it.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        } ?: "No hay más eventos hoy"

        val prompt = """
        Sos Chapelotas, una secretaria ejecutiva argentina MUY sarcástica.
        Tu jefe lleva $timeString sin hacer absolutamente NADA.
        $nextEventContext.
        
        Tenés que:
        1. Gastarle/burlarte porque está al pedo hace rato
        2. Ser creativo con las burlas (podés usar referencias argentinas)
        3. Si es mucho tiempo (>2 horas), ser más insistente/preocupado sarcásticamente
        4. Mensaje corto, máximo 2 líneas
        5. Usar emojis para más sarcasmo
        
        Ejemplos del tono:
        - "Che, ¿seguís vivo? Hace 2 horas que no movés un dedo 🦥"
        - "Mirá vos, practicando para las olimpiadas de hacer nada 🏆"
        - "¿Necesitás que te mande un café? Digo, para despertar... 😴"
        """

        val message = aiRepository.callOpenAIForText(prompt, temperature = 0.8)

        // Guardar en el thread general
        database.conversationLogDao().insert(
            ConversationLog(
                timestamp = LocalDateTime.now(ZoneId.systemDefault()),
                role = "assistant",
                content = message,
                eventId = null,
                threadId = "general"
            )
        )

        // Actualizar thread general
        database.chatThreadDao().updateLastMessage("general", message)

        // Mostrar notificación burlona
        val notification = com.chapelotas.app.domain.entities.ChapelotasNotification(
            id = "idle_${System.currentTimeMillis()}",
            eventId = 0L,
            scheduledTime = LocalDateTime.now(ZoneId.systemDefault()),
            message = message,
            priority = com.chapelotas.app.domain.entities.NotificationPriority.LOW,
            type = com.chapelotas.app.domain.entities.NotificationType.SARCASTIC_NUDGE
        )

        notificationRepository.showImmediateNotification(notification)

        Log.d(TAG, "😏 Mensaje sarcástico enviado por $idleMinutes minutos de inactividad")
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