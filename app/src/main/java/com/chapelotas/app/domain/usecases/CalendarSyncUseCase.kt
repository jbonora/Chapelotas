package com.chapelotas.app.domain.usecases

import com.chapelotas.app.domain.debug.DebugLog
import com.chapelotas.app.domain.entities.ChapelotasNotification
import com.chapelotas.app.domain.events.CalendarEvent
import com.chapelotas.app.domain.events.EventBus
import com.chapelotas.app.domain.events.TaskEvent
import com.chapelotas.app.domain.models.Sender
import com.chapelotas.app.domain.permissions.AppStatusManager
import com.chapelotas.app.domain.permissions.PermissionStatus
import com.chapelotas.app.domain.repositories.CalendarRepository
import com.chapelotas.app.domain.repositories.NotificationRepository
import com.chapelotas.app.domain.repositories.TaskRepository
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarSyncUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val taskRepository: TaskRepository,
    private val reminderEngine: ReminderEngine,
    private val debugLog: DebugLog,
    private val appStatusManager: AppStatusManager,
    private val notificationRepository: NotificationRepository,
    private val eventBus: EventBus
) {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    suspend fun syncDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
        triggerReminders: Boolean = true
    ) {
        debugLog.add("📅 SYNC: ==> Iniciando syncDateRange de $startDate a $endDate...")
        eventBus.emit(CalendarEvent.SyncStarted(startDate.toString(), endDate.toString()))

        if (appStatusManager.hasCalendarPermission() == PermissionStatus.DENIED) {
            debugLog.add("📅 SYNC: 🔴 ERROR - No hay permisos de calendario.")
            eventBus.emit(CalendarEvent.SyncFailed("No hay permisos de calendario"))
            return
        }

        try {
            val calendarEvents = calendarRepository.getEventsInRange(startDate, endDate)
            debugLog.add("📅 SYNC: Encontrados ${calendarEvents.size} eventos en calendario.")

            // Log de eventos encontrados para debug
            calendarEvents.forEach { event ->
                debugLog.add("📅 SYNC: Evento: '${event.title}' - ${event.startTime}")
            }

            val newTasks = taskRepository.syncWithCalendarEvents(calendarEvents, startDate, endDate)

            if (newTasks.isNotEmpty()) {
                debugLog.add("📅 SYNC: 🟢 Detectadas ${newTasks.size} tareas NUEVAS:")
                for (task in newTasks) {
                    debugLog.add("   - '${task.title}' a las ${task.scheduledTime.format(timeFormatter)}")

                    eventBus.emit(TaskEvent.TaskCreated(task.id, task.title, task.isFromCalendar))
                    val logMessage = "¡Agendado! He añadido '${task.title}' a tu lista."
                    taskRepository.addMessageToLog(task.id, logMessage, Sender.CHAPELOTAS, true)
                    eventBus.emit(TaskEvent.MessageAdded(task.id, logMessage, Sender.CHAPELOTAS.name, true))

                    if (task.scheduledTime.toLocalDate().isEqual(LocalDate.now())) {
                        val notificationMessage = "Nueva Tarea Hoy: '${task.title}' a las ${task.scheduledTime.format(timeFormatter)}"
                        val notification = ChapelotasNotification("new_task_${task.id}", task.id, notificationMessage, emptyList())
                        notificationRepository.showImmediateNotification(notification)
                        debugLog.add("📅 SYNC: Notificación enviada para tarea de hoy")
                    }
                }
            } else {
                debugLog.add("📅 SYNC: No hay tareas nuevas (todas ya existían)")
            }

            if (triggerReminders) {
                debugLog.add("📅 SYNC: Activando ReminderEngine para actualizar todas las alarmas...")
                reminderEngine.initializeOrUpdateAllReminders()
            } else {
                debugLog.add("📅 SYNC: Omitiendo actualización de recordatorios (triggerReminders=false)")
            }

            debugLog.add("📅 SYNC: <== syncDateRange finalizado exitosamente.")
            eventBus.emit(CalendarEvent.SyncCompleted(calendarEvents.size, newTasks.size))

        } catch (e: Exception) {
            debugLog.add("📅 SYNC: 🔴 ERROR durante la sincronización: ${e.message}")
            debugLog.add("📅 SYNC: Stack trace: ${e.stackTraceToString()}")
            eventBus.emit(CalendarEvent.SyncFailed(e.message ?: "Error desconocido"))
            throw e
        }
    }

    suspend fun startMonitoring() {
        debugLog.add("👁️ MONITOR: ==> Iniciando monitoreo del calendario.")

        if (appStatusManager.hasCalendarPermission() == PermissionStatus.DENIED) {
            debugLog.add("👁️ MONITOR: 🔴 ABORTADO - Sin permisos de calendario.")
            return
        }

        // --- CORRECCIÓN: Se elimina la sincronización inicial de esta función ---
        // El ViewModel se encarga de la primera sincronización. El servicio
        // solo se encarga de inicializar los recordatorios para tareas ya existentes
        // y de escuchar futuros cambios.

        // Pequeño delay para asegurar que todo se inicialice
        delay(2000)

        // Verificar tareas existentes y programar recordatorios
        debugLog.add("👁️ MONITOR: Inicializando recordatorios para tareas existentes...")
        reminderEngine.initializeOrUpdateAllReminders()

        debugLog.add("👁️ MONITOR: Comenzando observación de cambios en el calendario...")

        // NOTA: Este collect es infinito y está bien así - es el monitor principal
        try {
            calendarRepository.observeCalendarChanges().collect {
                debugLog.add("👁️ MONITOR: ⚡️ ¡Cambio detectado en el calendario! Sincronizando...")
                eventBus.emit(CalendarEvent.CalendarChanged)
                val now = LocalDate.now()
                syncDateRange(now, now.plusDays(6), triggerReminders = true)
            }
        } catch (e: Exception) {
            debugLog.add("👁️ MONITOR: ❌ Error en el monitoreo: ${e.message}")
            // El monitoreo se detuvo, intentar reiniciar después de un delay
            delay(5000)
            debugLog.add("👁️ MONITOR: Intentando reiniciar monitoreo...")
            startMonitoring() // Reintentar
        }
    }
}