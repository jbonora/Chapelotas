package com.chapelotas.app.domain.usecases

import android.util.Log
import com.chapelotas.app.domain.debug.DebugLog
import com.chapelotas.app.domain.entities.ChapelotasNotification
import com.chapelotas.app.domain.entities.NotificationAction
import com.chapelotas.app.domain.models.AppSettings
import com.chapelotas.app.domain.models.Task
import com.chapelotas.app.domain.repositories.NotificationRepository
import com.chapelotas.app.domain.repositories.PreferencesRepository
import com.chapelotas.app.domain.repositories.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderEngine @Inject constructor(
    private val taskRepository: TaskRepository,
    private val notificationRepository: NotificationRepository,
    private val preferencesRepository: PreferencesRepository,
    private val debugLog: DebugLog
) {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val _settings = MutableStateFlow(AppSettings())
    private val settings = _settings.asStateFlow()

    init {
        coroutineScope.launch {
            preferencesRepository.observeAppSettings().distinctUntilChanged().collect { newSettings ->
                _settings.value = newSettings
                debugLog.add("⚙️ SETTINGS: Configuración de Chapelotas actualizada.")
            }
        }
    }

    companion object {
        private const val TAG = "ReminderEngine"
    }

    /**
     * Punto de entrada principal del motor. Revisa periódicamente las tareas pendientes.
     */
    suspend fun checkAndSendReminders() {
        val now = LocalDateTime.now()
        val pendingTasks = taskRepository.getTasksNeedingReminder()

        for (task in pendingTasks) {
            evaluateTask(task, now)
        }
    }

    /**
     * Asegura la integridad de los datos al inicio o tras una sincronización.
     * Re-calcula el próximo aviso para todas las tareas activas.
     */
    suspend fun initializeTaskReminders() {
        debugLog.add("ENGINE: Re-inicializando recordatorios para todas las tareas activas...")
        // Obtenemos todas las tareas que no están finalizadas para re-calcular su próximo aviso.
        val activeTasks = taskRepository.getTasksNeedingReminder()
        for (task in activeTasks) {
            val nextReminder = getNextReminderTime(task, LocalDateTime.now())
            if (task.nextReminderAt != nextReminder) {
                taskRepository.updateInitialReminderState(task.id, task.reminderCount, nextReminder)
            }
        }
    }

    /**
     * Lógica central clara y modular.
     * Determina la temporalidad de la tarea y delega a la función correspondiente.
     */
    private suspend fun evaluateTask(task: Task, now: LocalDateTime) {
        if (task.isFinished) {
            // Lógica para el mensaje de felicitación único (si es necesario)
            return
        }

        val endTime = task.endTime ?: task.scheduledTime.plusHours(1)

        // Si la tarea necesita un recordatorio (porque es el momento o porque el anterior falló)
        if (task.nextReminderAt == null || now.isAfter(task.nextReminderAt)) {
            when {
                now.isAfter(endTime) -> evaluatePastTask(task, now)
                now.isAfter(task.scheduledTime) -> evaluatePresentTask(task, now)
                else -> evaluateFutureTask(task, now)
            }
        }
    }

    // --- MÉTODOS MODULARES POR TEMPORALIDAD ---

    private suspend fun evaluateFutureTask(task: Task, now: LocalDateTime) {
        val nextReminderTime = getNextUpcomingReminderTime(task, now)

        val messageTemplate = if (task.isAcknowledged) {
            "Me dijiste OK para '{TASK_TITLE}' pero como siempre te olvidás, te lo recuerdo (faltan {MINUTES} min)."
        } else {
            "En {MINUTES} min empieza '{TASK_TITLE}'. A ver si alguna vez te reivindicás."
        }
        val minutesUntil = ChronoUnit.MINUTES.between(now, task.scheduledTime) + 1
        val message = messageTemplate
            .replace("{TASK_TITLE}", task.title)
            .replace("{MINUTES}", minutesUntil.toString())

        val actions = if (task.isAcknowledged) listOf(NotificationAction.FINISH_DONE) else listOf(NotificationAction.ACKNOWLEDGE, NotificationAction.FINISH_DONE)
        sendReminder(task, message, nextReminderTime, actions)
    }

    private suspend fun evaluatePresentTask(task: Task, now: LocalDateTime) {
        val currentSettings = settings.first()
        val message: String
        val actions: List<NotificationAction>

        if (task.isAcknowledged) {
            message = "Bueh... a ver si algún día terminás '${task.title}'..."
            actions = listOf(NotificationAction.FINISH_DONE)
        } else {
            message = "Ya deberías estar en '${task.title}'... ¿No pensás darme bola?"
            actions = listOf(NotificationAction.ACKNOWLEDGE, NotificationAction.FINISH_DONE)
        }
        sendReminder(task, message, now.plusMinutes(currentSettings.ongoingInterval.toLong()), actions)
    }

    private suspend fun evaluatePastTask(task: Task, now: LocalDateTime) {
        val currentSettings = settings.first()
        val message: String
        val actions: List<NotificationAction>

        if (task.isAcknowledged) {
            message = "¿Ya abandonaste o seguís con la tarea '${task.title}'? Dale, definí."
            actions = listOf(NotificationAction.FINISH_DONE)
        } else {
            message = "${currentSettings.userName}, la tarea '${task.title}' ya pasó y ni le diste bola. ¿QUÉ ONDA?"
            actions = listOf(NotificationAction.ACKNOWLEDGE, NotificationAction.FINISH_DONE)
        }
        sendReminder(task, message, now.plusMinutes(currentSettings.missedInterval.toLong()), actions)
    }

    // --- LÓGICA DE ENVÍO Y CÁLCULO DE TIEMPO ---

    private suspend fun sendReminder(task: Task, message: String, nextReminderTime: LocalDateTime?, actions: List<NotificationAction>) {
        Log.i(TAG, ">> Enviando recordatorio para '${task.title}': $message")

        val notification = ChapelotasNotification(
            id = "rem_${task.id}_${System.currentTimeMillis()}",
            eventId = task.id,
            message = message,
            actions = actions
        )
        notificationRepository.showImmediateNotification(notification)
        taskRepository.recordReminderSent(task.id, nextReminderTime)
    }

    // --- INICIO DE LA CORRECCIÓN ---
    /**
     * Calcula el próximo aviso MÁS CERCANO en el futuro basado en los `Settings`.
     */
    private fun getNextUpcomingReminderTime(task: Task, now: LocalDateTime): LocalDateTime? {
        val settings = _settings.value
        val reminderMinutes = listOf(settings.firstReminder, settings.secondReminder, settings.thirdReminder, 0).distinct()

        // 1. Calcula todos los posibles momentos de aviso
        val potentialTimes = reminderMinutes.map { task.scheduledTime.minusMinutes(it.toLong()) }

        // 2. Filtra solo los que están en el futuro
        val futureTimes = potentialTimes.filter { it.isAfter(now) }

        // 3. Devuelve el más cercano en el tiempo (el mínimo)
        return futureTimes.minOrNull()
    }
    // --- FIN DE LA CORRECCIÓN ---

    /**
     * Lógica unificada para calcular el próximo recordatorio de cualquier tarea activa.
     */
    private fun getNextReminderTime(task: Task, now: LocalDateTime): LocalDateTime? {
        if (task.isFinished) return null

        val endTime = task.endTime ?: task.scheduledTime.plusHours(1)
        val settings = _settings.value

        return when {
            now.isAfter(endTime) -> now.plusMinutes(settings.missedInterval.toLong())
            now.isAfter(task.scheduledTime) -> now.plusMinutes(settings.ongoingInterval.toLong())
            else -> getNextUpcomingReminderTime(task, now)
        }
    }
}