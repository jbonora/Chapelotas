package com.chapelotas.app.domain.usecases

import com.chapelotas.app.domain.debug.DebugLog
import com.chapelotas.app.domain.permissions.AppStatusManager
import com.chapelotas.app.domain.permissions.PermissionStatus
import com.chapelotas.app.domain.repositories.CalendarRepository
import com.chapelotas.app.domain.repositories.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarSyncUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val taskRepository: TaskRepository,
    private val reminderEngine: ReminderEngine,
    private val debugLog: DebugLog,
    // Se inyecta el nuevo manager de estado
    private val appStatusManager: AppStatusManager
) {

    suspend fun syncToday(): Int {
        debugLog.add("SYNC: ==> Iniciando syncToday...")
        // --- LÍNEA MODIFICADA ---
        if (appStatusManager.hasCalendarPermission() == PermissionStatus.DENIED) {
            debugLog.add("SYNC: 🔴 ERROR - No hay permisos de calendario.")
            return 0
        }

        val calendarEvents = calendarRepository.getEventsForDate(LocalDate.now())
        debugLog.add("SYNC: Encontrados ${calendarEvents.size} eventos en calendario.")
        taskRepository.syncWithCalendarEvents(calendarEvents)
        reminderEngine.initializeOrUpdateAllReminders()
        debugLog.add("SYNC: <== syncToday finalizado.")
        return calendarEvents.size
    }

    suspend fun startMonitoring() {
        debugLog.add("MONITOR: ==> Iniciando monitoreo (startMonitoring).")
        // --- LÍNEA MODIFICADA ---
        if (appStatusManager.hasCalendarPermission() == PermissionStatus.DENIED) {
            debugLog.add("MONITOR: 🔴 ABORTADO - No hay permisos para iniciar el monitoreo.")
            return
        }

        calendarRepository.observeCalendarChanges().collect {
            debugLog.add("MONITOR: ⚡️ ¡Cambio detectado en el calendario! Sincronizando...")
            syncToday()
        }
    }
}