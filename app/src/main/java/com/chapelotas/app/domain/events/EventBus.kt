package com.chapelotas.app.domain.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AppEvent {
    val timestamp: Long
        get() = System.currentTimeMillis()
}

sealed class AlarmEvent : AppEvent {
    data class AlarmScheduled(
        val alarmTime: String,
        val alarmDate: String,
        val requestCode: Int
    ) : AlarmEvent()

    data class AlarmCancelled(
        val requestCode: Int
    ) : AlarmEvent()

    data class AlarmTriggered(
        val requestCode: Int
    ) : AlarmEvent()
}

sealed class TaskEvent : AppEvent {
    data class TaskCreated(
        val taskId: String,
        val title: String,
        val isFromCalendar: Boolean
    ) : TaskEvent()

    data class TaskUpdated(
        val taskId: String,
        val field: String,
        val oldValue: Any?,
        val newValue: Any?
    ) : TaskEvent()

    data class TaskDeleted(
        val taskId: String
    ) : TaskEvent()

    data class TaskStatusChanged(
        val taskId: String,
        val newStatus: String,
        val isFinished: Boolean
    ) : TaskEvent()

    data class TaskAcknowledged(
        val taskId: String
    ) : TaskEvent()

    data class MessageAdded(
        val taskId: String,
        val message: String,
        val sender: String,
        val incrementCounter: Boolean
    ) : TaskEvent()
}

sealed class CalendarEvent : AppEvent {
    data class SyncStarted(
        val startDate: String,
        val endDate: String
    ) : CalendarEvent()

    data class SyncCompleted(
        val eventsCount: Int,
        val newTasksCount: Int
    ) : CalendarEvent()

    data class SyncFailed(
        val error: String
    ) : CalendarEvent()

    object CalendarChanged : CalendarEvent()
}

sealed class NotificationEvent : AppEvent {
    data class NotificationShown(
        val notificationId: String,
        val taskId: String?,
        val message: String
    ) : NotificationEvent()

    data class NotificationCancelled(
        val notificationId: String
    ) : NotificationEvent()

    data class NotificationActionClicked(
        val notificationId: String,
        val action: String
    ) : NotificationEvent()
}

sealed class SystemEvent : AppEvent {
    object AppStarted : SystemEvent()
    object AppResumed : SystemEvent()
    object AppPaused : SystemEvent()
    object DayChanged : SystemEvent()

    // --- ✅ CAMBIO PRINCIPAL AQUÍ ---
    // Reemplazamos el evento genérico por eventos específicos.
    object AlarmSettingsChanged : SystemEvent()
    object PersonalitySettingsChanged : SystemEvent()
    // --- FIN DEL CAMBIO ---

    data class PermissionGranted(
        val permission: String
    ) : SystemEvent()

    data class PermissionDenied(
        val permission: String
    ) : SystemEvent()

    data class ServiceStarted(
        val serviceName: String
    ) : SystemEvent()

    data class ServiceStopped(
        val serviceName: String
    ) : SystemEvent()
}

@Singleton
class EventBus @Inject constructor() {

    private val _events = MutableSharedFlow<AppEvent>(
        replay = 0,
        extraBufferCapacity = 100
    )
    val events: SharedFlow<AppEvent> = _events.asSharedFlow()

    private val _alarmEvents = MutableSharedFlow<AlarmEvent>(
        replay = 0,
        extraBufferCapacity = 20
    )
    val alarmEvents: SharedFlow<AlarmEvent> = _alarmEvents.asSharedFlow()

    private val _taskEvents = MutableSharedFlow<TaskEvent>(
        replay = 0,
        extraBufferCapacity = 50
    )
    val taskEvents: SharedFlow<TaskEvent> = _taskEvents.asSharedFlow()

    private val _calendarEvents = MutableSharedFlow<CalendarEvent>(
        replay = 0,
        extraBufferCapacity = 20
    )
    val calendarEvents: SharedFlow<CalendarEvent> = _calendarEvents.asSharedFlow()

    private val _notificationEvents = MutableSharedFlow<NotificationEvent>(
        replay = 0,
        extraBufferCapacity = 30
    )
    val notificationEvents: SharedFlow<NotificationEvent> = _notificationEvents.asSharedFlow()

    private val _systemEvents = MutableSharedFlow<SystemEvent>(
        replay = 0,
        extraBufferCapacity = 20
    )
    val systemEvents: SharedFlow<SystemEvent> = _systemEvents.asSharedFlow()

    suspend fun emit(event: AppEvent) {
        _events.emit(event)

        when (event) {
            is AlarmEvent -> _alarmEvents.emit(event)
            is TaskEvent -> _taskEvents.emit(event)
            is CalendarEvent -> _calendarEvents.emit(event)
            is NotificationEvent -> _notificationEvents.emit(event)
            is SystemEvent -> _systemEvents.emit(event)
        }
    }

    fun tryEmit(event: AppEvent): Boolean {
        val generalSuccess = _events.tryEmit(event)

        val specificSuccess = when (event) {
            is AlarmEvent -> _alarmEvents.tryEmit(event)
            is TaskEvent -> _taskEvents.tryEmit(event)
            is CalendarEvent -> _calendarEvents.tryEmit(event)
            is NotificationEvent -> _notificationEvents.tryEmit(event)
            is SystemEvent -> _systemEvents.tryEmit(event)
        }

        return generalSuccess && specificSuccess
    }
}