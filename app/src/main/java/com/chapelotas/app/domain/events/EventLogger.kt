package com.chapelotas.app.domain.events

import com.chapelotas.app.domain.debug.DebugLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventLogger @Inject constructor(
    private val eventBus: EventBus,
    private val debugLog: DebugLog
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun startLogging() {
        scope.launch {
            eventBus.events
                .catch { e ->
                    debugLog.add("EVENT_LOGGER: ❌ Error en el logger: ${e.message}")
                }
                .collect { event ->
                    logEvent(event)
                }
        }
    }

    private fun logEvent(event: AppEvent) {
        val message = when (event) {
            is AlarmEvent.AlarmScheduled ->
                "⏰ ALARMA PROGRAMADA: ${event.alarmTime} del ${event.alarmDate}"
            is AlarmEvent.AlarmCancelled ->
                "⏰ ALARMA CANCELADA: RequestCode ${event.requestCode}"
            is AlarmEvent.AlarmTriggered ->
                "⏰ ALARMA DISPARADA: RequestCode ${event.requestCode}"

            is TaskEvent.TaskCreated ->
                "📝 TAREA CREADA: '${event.title}' (ID: ${event.taskId}, Calendario: ${event.isFromCalendar})"
            is TaskEvent.TaskUpdated ->
                "📝 TAREA ACTUALIZADA: ${event.taskId} - ${event.field} cambió"
            is TaskEvent.TaskDeleted ->
                "📝 TAREA ELIMINADA: ${event.taskId}"
            is TaskEvent.TaskStatusChanged ->
                "📝 ESTADO CAMBIADO: ${event.taskId} -> ${event.newStatus}"
            is TaskEvent.TaskAcknowledged ->
                "📝 TAREA RECONOCIDA: ${event.taskId}"
            is TaskEvent.MessageAdded ->
                "💬 MENSAJE AÑADIDO: Tarea ${event.taskId} - De: ${event.sender}"

            is CalendarEvent.SyncStarted ->
                "📅 SYNC INICIADO: ${event.startDate} a ${event.endDate}"
            is CalendarEvent.SyncCompleted ->
                "📅 SYNC COMPLETADO: ${event.eventsCount} eventos, ${event.newTasksCount} tareas nuevas"
            is CalendarEvent.SyncFailed ->
                "📅 SYNC FALLÓ: ${event.error}"
            is CalendarEvent.CalendarChanged ->
                "📅 CALENDARIO CAMBIÓ: Detectado cambio en calendario del dispositivo"

            is NotificationEvent.NotificationShown ->
                "🔔 NOTIFICACIÓN MOSTRADA: ${event.notificationId} - ${event.message}"
            is NotificationEvent.NotificationCancelled ->
                "🔔 NOTIFICACIÓN CANCELADA: ${event.notificationId}"
            is NotificationEvent.NotificationActionClicked ->
                "🔔 ACCIÓN NOTIFICACIÓN: ${event.notificationId} - Acción: ${event.action}"

            is SystemEvent.AppStarted -> "🚀 APP INICIADA"
            is SystemEvent.AppResumed -> "🚀 APP RESUMIDA"
            is SystemEvent.AppPaused -> "🚀 APP PAUSADA"
            is SystemEvent.PermissionGranted -> "✅ PERMISO CONCEDIDO: ${event.permission}"
            is SystemEvent.PermissionDenied -> "❌ PERMISO DENEGADO: ${event.permission}"
            is SystemEvent.ServiceStarted -> "🔧 SERVICIO INICIADO: ${event.serviceName}"
            is SystemEvent.ServiceStopped -> "🔧 SERVICIO DETENIDO: ${event.serviceName}"
            is SystemEvent.DayChanged -> "🌙 DÍA CAMBIADO: La fecha ha cambiado, refrescando la UI."

            // --- ✅ CORRECCIÓN AQUÍ ---
            // Añadimos los nuevos eventos y eliminamos el antiguo 'SettingsChanged'.
            is SystemEvent.AlarmSettingsChanged -> "⚙️ AJUSTES DE ALARMA CAMBIARON: Disparando recálculo de alarma."
            is SystemEvent.PersonalitySettingsChanged -> "🧠 AJUSTES DE PERSONALIDAD CAMBIARON."
            // --- FIN DE LA CORRECCIÓN ---
        }

        debugLog.add("EVENT_BUS: $message")
    }
}