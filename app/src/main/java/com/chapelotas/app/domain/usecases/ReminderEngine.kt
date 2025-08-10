package com.chapelotas.app.domain.usecases

import android.content.Context
import android.util.Log
import com.chapelotas.app.battery.HuaweiWakeUpManager
import com.chapelotas.app.di.Constants
import com.chapelotas.app.domain.debug.DebugLog
import com.chapelotas.app.domain.entities.ChapelotasNotification
import com.chapelotas.app.domain.entities.NotificationAction
import com.chapelotas.app.domain.models.*
import com.chapelotas.app.domain.personality.PersonalityProvider
import com.chapelotas.app.domain.repositories.NotificationRepository
import com.chapelotas.app.domain.repositories.PreferencesRepository
import com.chapelotas.app.domain.repositories.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderEngine @Inject constructor(
    private val taskRepository: TaskRepository,
    private val notificationRepository: NotificationRepository,
    private val preferencesRepository: PreferencesRepository,
    private val debugLog: DebugLog,
    private val personalityProvider: PersonalityProvider,
    private val huaweiWakeUpManager: HuaweiWakeUpManager,
    @ApplicationContext private val context: Context
) {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val isProcessing = AtomicBoolean(false)
    private val processingTasks = ConcurrentHashMap<String, Job>()

    private val settings: StateFlow<AppSettings> = preferencesRepository.observeAppSettings()
        .catch { e ->
            debugLog.add("❌ ENGINE: Error observando configuración: ${e.message}")
            emit(AppSettings()) // Emit default settings on error
        }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = AppSettings()
        )

    companion object {
        private const val TAG = "ReminderEngine"
        private const val NOTIFICATION_ID_PREFIX = "rem_"
        internal const val MINUTES_OFFSET_FOR_UPCOMING = 1L
        private const val MAX_CONCURRENT_PROCESSING = 10
        private const val PROCESSING_TIMEOUT_MS = 30000L
        private const val RETRY_DELAY_MS = 1000L
        private const val MAX_RETRIES = 3
    }

    suspend fun processAndSendReminder(taskId: String) {
        if (taskId.isBlank()) {
            debugLog.add("❌ ENGINE: TaskId vacío o nulo")
            return
        }

        // Prevent duplicate processing of the same task
        val existingJob = processingTasks[taskId]
        if (existingJob?.isActive == true) {
            debugLog.add("⚠️ ENGINE: Tarea '$taskId' ya está siendo procesada")
            return
        }

        val processingJob = coroutineScope.launch {
            processReminderWithRetry(taskId)
        }

        processingTasks[taskId] = processingJob

        try {
            withTimeout(PROCESSING_TIMEOUT_MS) {
                processingJob.join()
            }
        } catch (e: TimeoutCancellationException) {
            debugLog.add("⏰ ENGINE: Timeout procesando tarea '$taskId'")
            processingJob.cancel()
        } finally {
            processingTasks.remove(taskId)
        }
    }

    private suspend fun processReminderWithRetry(taskId: String) {
        var lastException: Exception? = null

        repeat(MAX_RETRIES) { attempt ->
            try {
                processReminderInternal(taskId)
                return // Success, exit retry loop
            } catch (e: Exception) {
                lastException = e
                debugLog.add("❌ ENGINE: Error en intento ${attempt + 1}/$MAX_RETRIES para tarea '$taskId': ${e.message}")

                if (attempt < MAX_RETRIES - 1) {
                    delay(RETRY_DELAY_MS * (attempt + 1)) // Exponential backoff
                }
            }
        }

        // All retries failed
        debugLog.add("💀 ENGINE: Falló el procesamiento de tarea '$taskId' después de $MAX_RETRIES intentos: ${lastException?.message}")
    }

    private suspend fun processReminderInternal(taskId: String) {
        debugLog.add("🔔 ENGINE: Procesando recordatorio para tarea '$taskId'")

        val task = getTaskIfValid(taskId) ?: return
        val settingsValue = settings.value

        // Check if task is for a future date
        if (task.scheduledTime.toLocalDate().isAfter(LocalDate.now())) {
            debugLog.add("📅 ENGINE: Alarma disparada para tarea futura ('${task.title}'). No se notifica, solo se reprograma.")
            rescheduleTaskReminder(task, settingsValue)
            return
        }

        // Check work hours
        if (isOutsideWorkHours(task, settingsValue)) {
            debugLog.add("🌙 ENGINE: Fuera de horario laboral. Notificación para '${task.title}' suprimida.")
            rescheduleTaskReminder(task, settingsValue)
            return
        }

        val strategy = ReminderStrategy.fromTask(task, settingsValue)
        if (strategy is NoReminderStrategy) {
            debugLog.add("⚠️ ENGINE: NoReminderStrategy para tarea '${task.title}' - no se envía notificación")
            return
        }

        val message = generateReminderMessage(task, strategy, settingsValue)
        if (message.isBlank()) {
            debugLog.add("⚠️ ENGINE: Mensaje vacío generado para tarea '${task.title}'")
            return
        }

        debugLog.add("💬 ENGINE: Mensaje generado: '$message'")

        // Send notification
        sendNotificationSafely(task, message, strategy.actions)

        // Schedule next reminder
        rescheduleTaskReminder(task, settingsValue)
    }

    private suspend fun rescheduleTaskReminder(task: Task, settings: AppSettings) {
        try {
            val nextReminderTime = getNextReminderTime(task, settings)
            debugLog.add("⏰ ENGINE: Próximo recordatorio calculado: ${nextReminderTime?.format(timeFormatter) ?: "NINGUNO"}")
            scheduleNextReminder(task, nextReminderTime)
        } catch (e: Exception) {
            debugLog.add("❌ ENGINE: Error reprogramando recordatorio para '${task.title}': ${e.message}")
        }
    }

    private suspend fun generateReminderMessage(
        task: Task,
        strategy: ReminderStrategy,
        settings: AppSettings
    ): String {
        return try {
            strategy.getMessage(task, LocalDateTime.now(), settings, personalityProvider)
        } catch (e: Exception) {
            debugLog.add("❌ ENGINE: Error generando mensaje para '${task.title}': ${e.message}")
            "Recordatorio: ${task.title}" // Fallback message
        }
    }

    private suspend fun getTaskIfValid(taskId: String): Task? {
        return try {
            debugLog.add("🔍 ENGINE: Validando tarea '$taskId'")

            val task = taskRepository.getTask(taskId)
            if (task == null) {
                debugLog.add("❌ ENGINE: Tarea '$taskId' no encontrada")
                cleanupTaskNotifications(taskId)
                return null
            }

            if (task.isFinished) {
                debugLog.add("✅ ENGINE: Tarea '$taskId' ya está finalizada")
                cleanupTaskNotifications(taskId)
                return null
            }

            debugLog.add("✅ ENGINE: Tarea '$taskId' válida - Título: '${task.title}'")
            task
        } catch (e: Exception) {
            debugLog.add("❌ ENGINE: Error validando tarea '$taskId': ${e.message}")
            null
        }
    }

    private suspend fun cleanupTaskNotifications(taskId: String) {
        try {
            notificationRepository.cancelReminder(taskId)
            notificationRepository.cancelTaskNotification(taskId)
        } catch (e: Exception) {
            debugLog.add("⚠️ ENGINE: Error limpiando notificaciones para tarea '$taskId': ${e.message}")
        }
    }

    suspend fun initializeOrUpdateAllReminders() {
        if (!isProcessing.compareAndSet(false, true)) {
            debugLog.add("⚠️ ENGINE: Inicialización ya en progreso")
            return
        }

        try {
            debugLog.add("🚀 ENGINE: ==> Inicializando/Actualizando TODAS las alarmas...")

            val activeTasks = getAllActiveTasks()
            val currentSettings = settings.value

            debugLog.add("📊 ENGINE: Encontradas ${activeTasks.size} tareas activas")

            if (activeTasks.isEmpty()) {
                debugLog.add("ℹ️ ENGINE: No hay tareas activas para programar")
                return
            }

            // Process tasks in batches to avoid overwhelming the system
            activeTasks.chunked(MAX_CONCURRENT_PROCESSING).forEach { batch ->
                processTaskBatch(batch, currentSettings)
            }

            debugLog.add("✅ ENGINE: <== Proceso de inicialización completado")
        } catch (e: Exception) {
            debugLog.add("❌ ENGINE: Error en inicialización general: ${e.message}")
        } finally {
            isProcessing.set(false)
        }
    }

    private suspend fun getAllActiveTasks(): List<Task> {
        return try {
            taskRepository.observeAllTasks().first().filter { !it.isFinished }
        } catch (e: Exception) {
            debugLog.add("❌ ENGINE: Error obteniendo tareas activas: ${e.message}")
            emptyList()
        }
    }

    private suspend fun processTaskBatch(tasks: List<Task>, settings: AppSettings) {
        coroutineScope {
            tasks.map { task ->
                async {
                    try {
                        processTaskReminder(task, settings)
                    } catch (e: Exception) {
                        debugLog.add("❌ ENGINE: Error procesando tarea '${task.title}': ${e.message}")
                    }
                }
            }.awaitAll()
        }
    }

    private suspend fun processTaskReminder(task: Task, settings: AppSettings) {
        val nextReminder = getNextReminderTime(task, settings)

        debugLog.add("📋 ENGINE: Tarea '${task.title}':")
        debugLog.add("   - Hora programada: ${task.scheduledTime.format(timeFormatter)}")
        debugLog.add("   - Estado: ${task.status}")
        debugLog.add("   - Próximo recordatorio calculado: ${nextReminder?.format(timeFormatter) ?: "NINGUNO"}")
        debugLog.add("   - Recordatorio actual en DB: ${task.nextReminderAt?.format(timeFormatter) ?: "NINGUNO"}")

        if (task.nextReminderAt != nextReminder) {
            debugLog.add("   ⚡ Actualizando recordatorio...")
            scheduleNextReminder(task, nextReminder)
        } else {
            debugLog.add("   ✓ Recordatorio ya está actualizado")
        }
    }

    private fun getNextReminderTime(task: Task, settings: AppSettings): LocalDateTime? {
        return try {
            val strategy = ReminderStrategy.fromTask(task, settings)
            val nextTime = strategy.getNextReminderTime(task, LocalDateTime.now(), settings)

            debugLog.add("🎯 ENGINE: Estrategia ${strategy::class.simpleName} para '${task.title}' -> Próximo: ${nextTime?.format(timeFormatter) ?: "NINGUNO"}")

            nextTime
        } catch (e: Exception) {
            debugLog.add("❌ ENGINE: Error calculando próximo recordatorio para '${task.title}': ${e.message}")
            null
        }
    }

    private suspend fun scheduleNextReminder(task: Task, nextReminderTime: LocalDateTime?) {
        try {
            if (nextReminderTime != null && nextReminderTime.isAfter(LocalDateTime.now().minusSeconds(5))) {
                debugLog.add("📅 ENGINE: Programando recordatorio para '${task.title}' a las ${nextReminderTime.format(timeFormatter)}")

                // Schedule with notification repository
                notificationRepository.scheduleExactReminder(task.id, nextReminderTime)

                // Update task in repository
                taskRepository.updateInitialReminderState(task.id, task.reminderCount, nextReminderTime)

                // Schedule Huawei prewarming if necessary
                huaweiWakeUpManager.schedulePrewarmingIfNecessary(task, nextReminderTime)

                debugLog.add("✅ ENGINE: Recordatorio programado exitosamente")
            } else {
                debugLog.add("🗑️ ENGINE: Cancelando recordatorio para '${task.title}' (tiempo pasado o nulo)")

                notificationRepository.cancelReminder(task.id)
                taskRepository.updateInitialReminderState(task.id, task.reminderCount, null)
            }
        } catch (e: Exception) {
            debugLog.add("❌ ENGINE: Error programando recordatorio para '${task.title}': ${e.message}")
        }
    }

    private suspend fun sendNotificationSafely(task: Task, message: String, actions: List<NotificationAction>) {
        try {
            debugLog.add("📤 ENGINE: Enviando notificación para '${task.title}'")
            Log.i(TAG, ">> Enviando recordatorio para '${task.title}': $message")

            // Update task log
            taskRepository.addMessageToLog(task.id, message, Sender.CHAPELOTAS, incrementCounter = true)
            taskRepository.recordReminderSent(task.id, task.nextReminderAt)

            val channelId = determineNotificationChannel(task)
            debugLog.add("📢 ENGINE: Canal de notificación: $channelId")

            val notification = ChapelotasNotification(
                id = "$NOTIFICATION_ID_PREFIX${task.id}_${System.currentTimeMillis()}",
                eventId = task.id,
                message = message,
                actions = actions,
                channelId = channelId
            )

            notificationRepository.showImmediateNotification(notification)
            debugLog.add("✅ ENGINE: Notificación enviada")
        } catch (e: Exception) {
            debugLog.add("❌ ENGINE: Error enviando notificación para '${task.title}': ${e.message}")
        }
    }

    private fun parseWorkTime(timeString: String): LocalTime? {
        return try {
            LocalTime.parse(timeString)
        } catch (e: DateTimeParseException) {
            debugLog.add("⚠️ ENGINE: Error al parsear horario laboral: $timeString")
            null
        }
    }

    private fun isOutsideWorkHours(task: Task, settings: AppSettings): Boolean {
        try {
            if (settings.workHours24h || task.isTodo) {
                debugLog.add("⏰ HORARIO: 24h activado o es ToDo - no hay restricción")
                return false
            }

            val workStartTime = parseWorkTime(settings.workStartTime)
            val workEndTime = parseWorkTime(settings.workEndTime)

            if (workStartTime == null || workEndTime == null) {
                debugLog.add("⚠️ HORARIO: Error parseando horarios - asumiendo 24h")
                return false
            }

            val currentTime = LocalDateTime.now().toLocalTime()
            val isOutside = currentTime.isBefore(workStartTime) || currentTime.isAfter(workEndTime)

            debugLog.add("⏰ HORARIO: Actual=${currentTime.format(timeFormatter)}, Trabajo=${workStartTime.format(timeFormatter)}-${workEndTime.format(timeFormatter)}, Fuera=$isOutside")

            return isOutside
        } catch (e: Exception) {
            debugLog.add("❌ ENGINE: Error verificando horario laboral: ${e.message}")
            return false // Default to allowing notifications
        }
    }

    private fun getInsistenceChannel(): String {
        return try {
            val channel = when (settings.value.insistenceSoundProfile) {
                InsistenceProfile.NORMAL -> Constants.CHANNEL_ID_INSISTENCE_NORMAL
                InsistenceProfile.MEDIUM -> Constants.CHANNEL_ID_INSISTENCE_MEDIUM
                InsistenceProfile.LOW -> Constants.CHANNEL_ID_INSISTENCE_LOW
                else -> Constants.CHANNEL_ID_INSISTENCE_MEDIUM
            }

            debugLog.add("🔊 ENGINE: Perfil de insistencia: ${settings.value.insistenceSoundProfile} -> Canal: $channel")
            channel
        } catch (e: Exception) {
            debugLog.add("❌ ENGINE: Error obteniendo canal de insistencia: ${e.message}")
            Constants.CHANNEL_ID_INSISTENCE_MEDIUM // Default fallback
        }
    }

    private fun determineNotificationChannel(task: Task): String {
        return try {
            if (task.isTodo || (task.status == TaskStatus.ONGOING && !task.isAcknowledged)) {
                return Constants.CHANNEL_ID_GENERAL
            }

            when (task.status) {
                TaskStatus.ONGOING, TaskStatus.DELAYED -> getInsistenceChannel()
                else -> Constants.CHANNEL_ID_GENERAL
            }
        } catch (e: Exception) {
            debugLog.add("❌ ENGINE: Error determinando canal de notificación: ${e.message}")
            Constants.CHANNEL_ID_GENERAL // Default fallback
        }
    }

    /**
     * Clean up resources when the engine is no longer needed
     */
    fun cleanup() {
        try {
            debugLog.add("🧹 ENGINE: Limpiando recursos...")

            // Cancel all processing jobs
            processingTasks.values.forEach { job ->
                job.cancel()
            }
            processingTasks.clear()

            // Cancel the main scope
            coroutineScope.cancel()

            debugLog.add("✅ ENGINE: Recursos limpiados")
        } catch (e: Exception) {
            debugLog.add("❌ ENGINE: Error limpiando recursos: ${e.message}")
        }
    }
}

// Strategy classes remain the same but with added error handling...
sealed class ReminderStrategy(val actions: List<NotificationAction>) {
    abstract fun getMessage(task: Task, now: LocalDateTime, settings: AppSettings, personalityProvider: PersonalityProvider): String
    abstract fun getNextReminderTime(task: Task, now: LocalDateTime, settings: AppSettings): LocalDateTime?

    companion object {
        fun fromTask(task: Task, settings: AppSettings): ReminderStrategy {
            return try {
                when (task.taskType) {
                    TaskType.TODO -> {
                        if (task.scheduledTime.toLocalDate().isEqual(LocalDate.now())) {
                            TodoReminderStrategy()
                        } else {
                            NoReminderStrategy
                        }
                    }
                    TaskType.EVENT -> when (task.status) {
                        TaskStatus.UPCOMING -> UpcomingReminderStrategy(task.isAcknowledged)
                        TaskStatus.ONGOING -> OngoingReminderStrategy(task.isAcknowledged)
                        TaskStatus.DELAYED -> PastReminderStrategy(task.isAcknowledged)
                        else -> NoReminderStrategy
                    }
                }
            } catch (e: Exception) {
                // Log error and return safe fallback
                NoReminderStrategy
            }
        }
    }
}

// Rest of the strategy classes remain the same as in your original implementation...
class TodoReminderStrategy : ReminderStrategy(listOf(NotificationAction.FINISH_DONE)) {
    companion object {
        private const val MIN_DELAY_MINUTES = 5
    }

    override fun getMessage(task: Task, now: LocalDateTime, settings: AppSettings, personalityProvider: PersonalityProvider): String {
        return "Recordatorio pendiente: '${task.title}'"
    }

    override fun getNextReminderTime(task: Task, now: LocalDateTime, settings: AppSettings): LocalDateTime? {
        val nowTime = now.toLocalTime()
        return if (nowTime.minute < MIN_DELAY_MINUTES) {
            now.withMinute(MIN_DELAY_MINUTES).withSecond(0).withNano(0)
        } else {
            now.plusHours(1).withMinute(MIN_DELAY_MINUTES).withSecond(0).withNano(0)
        }
    }
}

abstract class AcknowledgeableReminderStrategy(
    protected val isAcknowledged: Boolean
) : ReminderStrategy(
    if (isAcknowledged) listOf(NotificationAction.FINISH_DONE)
    else listOf(NotificationAction.ACKNOWLEDGE, NotificationAction.FINISH_DONE)
) {
    protected fun getContextKeySuffix() = if (isAcknowledged) "acknowledged" else "unacknowledged"
}

class UpcomingReminderStrategy(isAcknowledged: Boolean) : AcknowledgeableReminderStrategy(isAcknowledged) {
    override fun getMessage(task: Task, now: LocalDateTime, settings: AppSettings, personalityProvider: PersonalityProvider): String {
        val minutesUntil = ChronoUnit.MINUTES.between(now, task.scheduledTime) + ReminderEngine.MINUTES_OFFSET_FOR_UPCOMING
        val contextKey = "upcoming_reminder.${getContextKeySuffix()}"
        return personalityProvider.get(
            personalityKey = settings.personalityProfile,
            contextKey = contextKey,
            placeholders = mapOf(
                "{TASK_NAME}" to task.title,
                "{MINUTES}" to minutesUntil.toString(),
                "{USER_NAME}" to settings.userName
            )
        )
    }

    override fun getNextReminderTime(task: Task, now: LocalDateTime, settings: AppSettings): LocalDateTime? {
        val reminderMinutes = listOf(settings.firstReminder, settings.secondReminder, settings.thirdReminder, 0)
            .distinct().sortedDescending()

        val nextScheduledTime = reminderMinutes
            .map { task.scheduledTime.minusMinutes(it.toLong()) }
            .firstOrNull { it.isAfter(now) }

        if (nextScheduledTime == null) {
            if (task.scheduledTime.isAfter(now) && !task.isAcknowledged) {
                return now
            }
        }
        return nextScheduledTime
    }
}

class OngoingReminderStrategy(isAcknowledged: Boolean) : AcknowledgeableReminderStrategy(isAcknowledged) {
    override fun getMessage(task: Task, now: LocalDateTime, settings: AppSettings, personalityProvider: PersonalityProvider): String {
        val contextKey = "ongoing_reminder.${getContextKeySuffix()}"
        return personalityProvider.get(
            personalityKey = settings.personalityProfile,
            contextKey = contextKey,
            placeholders = mapOf("{TASK_NAME}" to task.title, "{USER_NAME}" to settings.userName)
        )
    }

    override fun getNextReminderTime(task: Task, now: LocalDateTime, settings: AppSettings): LocalDateTime {
        val interval = if (isAcknowledged) settings.lowUrgencyInterval.toLong() else settings.highUrgencyInterval.toLong()
        return now.plusMinutes(interval)
    }
}

class PastReminderStrategy(isAcknowledged: Boolean) : AcknowledgeableReminderStrategy(isAcknowledged) {
    override fun getMessage(task: Task, now: LocalDateTime, settings: AppSettings, personalityProvider: PersonalityProvider): String {
        val contextKey = "delayed_reminder.${getContextKeySuffix()}"
        return personalityProvider.get(
            personalityKey = settings.personalityProfile,
            contextKey = contextKey,
            placeholders = mapOf("{TASK_NAME}" to task.title, "{USER_NAME}" to settings.userName)
        )
    }

    override fun getNextReminderTime(task: Task, now: LocalDateTime, settings: AppSettings): LocalDateTime {
        return now.plusMinutes(settings.highUrgencyInterval.toLong())
    }
}

object NoReminderStrategy : ReminderStrategy(emptyList()) {
    override fun getMessage(task: Task, now: LocalDateTime, settings: AppSettings, personalityProvider: PersonalityProvider): String = ""
    override fun getNextReminderTime(task: Task, now: LocalDateTime, settings: AppSettings): LocalDateTime? = null
}