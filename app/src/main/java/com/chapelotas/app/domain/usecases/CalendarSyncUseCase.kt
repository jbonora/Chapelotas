package com.chapelotas.app.domain.usecases

import com.chapelotas.app.domain.debug.DebugLog
import com.chapelotas.app.domain.events.AppEvent
import com.chapelotas.app.domain.events.EventBus
import com.chapelotas.app.domain.models.Task
import com.chapelotas.app.domain.repositories.CalendarRepository
import com.chapelotas.app.domain.repositories.TaskRepository
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarSyncUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val taskRepository: TaskRepository,
    private val eventBus: EventBus,
    private val reminderEngine: ReminderEngine,
    private val debugLog: DebugLog
) {

    suspend fun syncToday(): Int {
        debugLog.add("SYNC: ==> Iniciando syncToday...")
        if (!calendarRepository.hasCalendarReadPermission()) {
            debugLog.add("SYNC: 🔴 ERROR - No hay permisos de calendario.")
            return 0
        }

        val calendarEvents = calendarRepository.getEventsForDate(LocalDate.now())
        debugLog.add("SYNC: Encontrados ${calendarEvents.size} eventos en calendario.")

        // La lógica de comparación de tareas antes y después se simplifica o elimina
        // ya que la UI reacciona directamente a los cambios en la base de datos.
        taskRepository.syncWithCalendarEvents(calendarEvents)

        debugLog.add("SYNC: Emitiendo 'SyncCompleted' al EventBus.")
        eventBus.emit(AppEvent.SyncCompleted)
        debugLog.add("SYNC: <== syncToday finalizado.")
        return calendarEvents.size
    }

    suspend fun startMonitoring() {
        debugLog.add("MONITOR: ==> Iniciando monitoreo (startMonitoring).")
        if (!calendarRepository.hasCalendarReadPermission()) {
            debugLog.add("MONITOR: 🔴 ABORTADO - No hay permisos para iniciar el monitoreo.")
            return
        }

        calendarRepository.observeCalendarChanges().collect {
            debugLog.add("MONITOR: ⚡️ ¡Cambio detectado en el calendario! Sincronizando...")
            syncToday()
        }
    }

    // --- INICIO DE LA CORRECCIÓN ---
    // La función getDaySummary ahora usa las nuevas banderas booleanas
    suspend fun getDaySummary(tasks: List<Task>): DaySummary {
        val now = LocalDateTime.now()
        val totalTasks = tasks.size
        val completedTasks = tasks.count { it.isFinished }
        val upcomingTasks = tasks.count { !it.isFinished && now.isBefore(it.scheduledTime) }
        val ongoingTasks = tasks.count { !it.isFinished && now.isAfter(it.scheduledTime) && now.isBefore(it.endTime ?: it.scheduledTime.plusHours(1)) }
        val missedTasks = tasks.count { !it.isFinished && now.isAfter(it.endTime ?: it.scheduledTime.plusHours(1)) }

        return DaySummary(
            totalTasks = totalTasks,
            completedTasks = completedTasks,
            upcomingTasks = upcomingTasks,
            ongoingTasks = ongoingTasks,
            missedTasks = missedTasks
        )
    }

    data class DaySummary(
        val totalTasks: Int,
        val completedTasks: Int,
        val upcomingTasks: Int,
        val ongoingTasks: Int,
        val missedTasks: Int
    ) {
        val pendingTasks: Int
            get() = upcomingTasks + ongoingTasks + missedTasks

        val completionRate: Float
            get() = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
    }
    // --- FIN DE LA CORRECCIÓN ---
}