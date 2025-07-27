package com.chapelotas.app.domain.usecases

import android.util.Log
import com.chapelotas.app.domain.debug.DebugLog
import com.chapelotas.app.domain.entities.ChapelotasNotification
import com.chapelotas.app.domain.entities.NotificationAction
import com.chapelotas.app.domain.models.AppSettings
import com.chapelotas.app.domain.models.Task
import com.chapelotas.app.domain.models.TaskStatus
import com.chapelotas.app.domain.repositories.NotificationRepository
import com.chapelotas.app.domain.repositories.PreferencesRepository
import com.chapelotas.app.domain.repositories.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
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
    private val settings: StateFlow<AppSettings> = preferencesRepository.observeAppSettings()
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    companion object {
        private const val TAG = "ReminderEngine"
    }

    suspend fun processAndSendReminder(taskId: String) {
        val task = taskRepository.getTask(taskId)
        if (task == null || task.isFinished) {
            notificationRepository.cancelReminder(taskId)
            return
        }

        val now = LocalDateTime.now()
        val strategy = ReminderStrategy.fromTask(task, settings.value)
        if (strategy is NoReminderStrategy) return

        val message = strategy.getMessage(task, now)
        val nextReminderTime = strategy.getNextReminderTime(task, now, settings.value)

        sendNotification(task, message, strategy.actions, nextReminderTime)
        scheduleNextReminder(task.id, nextReminderTime)
    }

    suspend fun initializeOrUpdateAllReminders() {
        debugLog.add("ENGINE: Inicializando/Actualizando todas las alarmas...")
        val activeTasks = taskRepository.observeAllTasks().first().filter { !it.isFinished }
        val now = LocalDateTime.now()
        val currentSettings = settings.value

        for (task in activeTasks) {
            val strategy = ReminderStrategy.fromTask(task, currentSettings)
            val nextReminder = strategy.getNextReminderTime(task, now, currentSettings)

            if (task.status == TaskStatus.DELAYED) {
                val message = strategy.getMessage(task, now)
                sendNotification(task, message, strategy.actions, nextReminder)
            }

            if (task.nextReminderAt != nextReminder) {
                taskRepository.updateInitialReminderState(task.id, task.reminderCount, nextReminder)
                scheduleNextReminder(task.id, nextReminder)
            }
        }
    }

    private fun scheduleNextReminder(taskId: String, nextReminderTime: LocalDateTime?) {
        if (nextReminderTime != null) {
            notificationRepository.scheduleExactReminder(taskId, nextReminderTime)
        } else {
            notificationRepository.cancelReminder(taskId)
        }
    }

    // --- FUNCIÓN CORREGIDA ---
    // Ahora recibe el "nextReminderTime" para guardarlo correctamente en la base de datos.
    private suspend fun sendNotification(task: Task, message: String, actions: List<NotificationAction>, nextReminderTime: LocalDateTime?) {
        Log.i(TAG, ">> Enviando recordatorio para '${task.title}': $message")

        val notification = ChapelotasNotification(
            id = "rem_${task.id}_${System.currentTimeMillis()}",
            eventId = task.id,
            message = message,
            actions = actions
        )
        notificationRepository.showImmediateNotification(notification)
        // Ahora se guarda el valor correcto, lo que disparará la actualización en la UI de Debug.
        taskRepository.recordReminderSent(task.id, nextReminderTime)
    }
}


// --- El resto de las clases Strategy no necesitan cambios ---
sealed class ReminderStrategy(val actions: List<NotificationAction>) {
    abstract fun getMessage(task: Task, now: LocalDateTime): String
    abstract fun getNextReminderTime(task: Task, now: LocalDateTime, settings: AppSettings): LocalDateTime?

    companion object {
        fun fromTask(task: Task, settings: AppSettings): ReminderStrategy {
            return when (task.status) {
                TaskStatus.UPCOMING -> UpcomingReminderStrategy(task.isAcknowledged)
                TaskStatus.ONGOING -> OngoingReminderStrategy(task.isAcknowledged)
                TaskStatus.DELAYED -> PastReminderStrategy(task.isAcknowledged, settings.userName)
                TaskStatus.FINISHED -> NoReminderStrategy
            }
        }
    }
}

class UpcomingReminderStrategy(isAcknowledged: Boolean) : ReminderStrategy(
    if (isAcknowledged) listOf(NotificationAction.FINISH_DONE) else listOf(NotificationAction.ACKNOWLEDGE, NotificationAction.FINISH_DONE)
) {
    override fun getMessage(task: Task, now: LocalDateTime): String {
        val minutesUntil = ChronoUnit.MINUTES.between(now, task.scheduledTime) + 1
        return if (actions.contains(NotificationAction.ACKNOWLEDGE)) {
            "En $minutesUntil min empieza '${task.title}'. A ver si te ponés las pilas."
        } else {
            "Ya me dijiste que sí para '${task.title}', pero te lo recuerdo por si acaso (faltan $minutesUntil min)."
        }
    }

    override fun getNextReminderTime(task: Task, now: LocalDateTime, settings: AppSettings): LocalDateTime? {
        val reminderMinutes = listOf(settings.firstReminder, settings.secondReminder, settings.thirdReminder, 0).distinct()
        return reminderMinutes
            .map { task.scheduledTime.minusMinutes(it.toLong()) }
            .filter { it.isAfter(now) }
            .minOrNull()
    }
}

class OngoingReminderStrategy(private val isAcknowledged: Boolean) : ReminderStrategy(
    if (isAcknowledged) listOf(NotificationAction.FINISH_DONE) else listOf(NotificationAction.ACKNOWLEDGE, NotificationAction.FINISH_DONE)
) {
    override fun getMessage(task: Task, now: LocalDateTime): String {
        return if (isAcknowledged) {
            "Dale que va... A ver si terminamos con '${task.title}' de una vez."
        } else {
            "Ya deberías estar haciendo '${task.title}'... ¿Vas a hacer algo o no?"
        }
    }

    override fun getNextReminderTime(task: Task, now: LocalDateTime, settings: AppSettings): LocalDateTime {
        val interval = if (isAcknowledged) {
            settings.lowUrgencyInterval.toLong()
        } else {
            settings.highUrgencyInterval.toLong()
        }
        return now.plusMinutes(interval)
    }
}

class PastReminderStrategy(private val isAcknowledged: Boolean, private val userName: String) : ReminderStrategy(
    if (isAcknowledged) listOf(NotificationAction.FINISH_DONE) else listOf(NotificationAction.ACKNOWLEDGE, NotificationAction.FINISH_DONE)
) {
    override fun getMessage(task: Task, now: LocalDateTime): String {
        return if (isAcknowledged) {
            "¿Ya te rendiste o seguís con '${task.title}'? Definí, campeón."
        } else {
            "$userName, la tarea '${task.title}' ya pasó y ni la miraste. Un desastre."
        }
    }

    override fun getNextReminderTime(task: Task, now: LocalDateTime, settings: AppSettings): LocalDateTime {
        return now.plusMinutes(settings.highUrgencyInterval.toLong())
    }
}

object NoReminderStrategy : ReminderStrategy(emptyList()) {
    override fun getMessage(task: Task, now: LocalDateTime): String = ""
    override fun getNextReminderTime(task: Task, now: LocalDateTime, settings: AppSettings): LocalDateTime? = null
}