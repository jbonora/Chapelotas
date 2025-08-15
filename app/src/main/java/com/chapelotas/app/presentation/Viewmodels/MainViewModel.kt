package com.chapelotas.app.presentation.viewmodels

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chapelotas.app.data.notifications.ChapelotasNotificationService
import com.chapelotas.app.di.Constants
import com.chapelotas.app.domain.debug.DebugLog
import com.chapelotas.app.domain.events.AlarmEvent
import com.chapelotas.app.domain.events.CalendarEvent
import com.chapelotas.app.domain.events.EventBus
import com.chapelotas.app.domain.events.SystemEvent
import com.chapelotas.app.domain.events.TaskEvent
import com.chapelotas.app.domain.models.Sender
import com.chapelotas.app.domain.models.Task
import com.chapelotas.app.domain.models.TaskType
import com.chapelotas.app.domain.models.TaskStatus
import com.chapelotas.app.domain.permissions.AppStatusManager
import com.chapelotas.app.domain.permissions.PermissionStatus
import com.chapelotas.app.domain.personality.PersonalityProvider
import com.chapelotas.app.domain.repositories.NotificationRepository
import com.chapelotas.app.domain.repositories.PreferencesRepository
import com.chapelotas.app.domain.repositories.TaskRepository
import com.chapelotas.app.domain.time.TimeManager
import com.chapelotas.app.domain.time.TimeRemaining
import com.chapelotas.app.domain.usecases.AlarmSchedulerUseCase
import com.chapelotas.app.domain.usecases.CalendarSyncUseCase
import com.chapelotas.app.domain.usecases.ReminderEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// Modelo de datos actualizado con tiempo restante
sealed class DisplayableItem {
    abstract val id: String

    data class Event(
        val task: Task,
        val timeRemaining: TimeRemaining? = null,
        val formattedTime: String = ""
    ) : DisplayableItem() {
        override val id: String = task.id
    }

    object TodoHeader : DisplayableItem() {
        override val id: String = "todo_header"
    }

    data class Todo(
        val task: Task,
        val timeRemaining: TimeRemaining? = null,
        val formattedTime: String = ""
    ) : DisplayableItem() {
        override val id: String = task.id
    }
}

data class MainUiState(
    val isLoading: Boolean = false,
    val needsPermission: Boolean = false,
    val error: String? = null,
    val showBatteryProtectionDialog: Boolean = false,
    val nextAlarmDuration: Duration? = null,
    val currentTime: LocalDateTime = LocalDateTime.now()
)

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val calendarSyncUseCase: CalendarSyncUseCase,
    private val taskRepository: TaskRepository,
    private val preferencesRepository: PreferencesRepository,
    private val debugLog: DebugLog,
    private val appStatusManager: AppStatusManager,
    private val reminderEngine: ReminderEngine,
    private val notificationRepository: NotificationRepository,
    private val personalityProvider: PersonalityProvider,
    private val eventBus: EventBus,
    private val alarmScheduler: AlarmSchedulerUseCase,
    private val timeManager: TimeManager  // NUEVO: Inyección del TimeManager
) : ViewModel() {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _showNewDayDialog = MutableStateFlow(false)
    val showNewDayDialog: StateFlow<Boolean> = _showNewDayDialog.asStateFlow()

    private val _currentDate = MutableStateFlow(LocalDate.now())
    private var countdownJob: Job? = null

    // CAMBIO PRINCIPAL: Combinar tareas con tiempo actual para actualización continua
    val todayItems: StateFlow<List<DisplayableItem>> = combine(
        _currentDate.flatMapLatest { date ->
            taskRepository.observeTasksForDate(date)
        },
        timeManager.currentTimeFlow // Actualización cada segundo
    ) { tasks, currentTime ->
        buildDisplayableItems(tasks, currentTime)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        debugLog.add("VIEWMODEL: ==> MainViewModel inicializado.")
        subscribeToEvents()
        checkPermissionsAndInitialSync()
        checkNextAlarm()
        checkIfBatteryProtectionIsNeeded()
        startMidnightDetector()
        startTimeUpdater() // NUEVO: Iniciar actualizador de tiempo
    }

    // NUEVA FUNCIÓN: Construir items con tiempo actualizado
    private fun buildDisplayableItems(tasks: List<Task>, currentTime: LocalDateTime): List<DisplayableItem> {
        val (todos, events) = tasks.partition { it.taskType == TaskType.TODO }

        return buildList {
            // Agregar eventos con tiempo restante
            addAll(
                events.sortedBy { it.scheduledTime }.map { task ->
                    val timeRemaining = if (!task.isFinished) {
                        timeManager.getTimeUntil(task.scheduledTime)
                    } else null

                    val formattedTime = when {
                        task.isFinished -> "✓ Finalizado"
                        timeRemaining != null -> {
                            val timeStr = timeManager.formatTimeRemaining(timeRemaining)
                            val scheduleStr = task.scheduledTime.format(timeFormatter)
                            "$scheduleStr ($timeStr)"
                        }
                        else -> task.scheduledTime.format(timeFormatter)
                    }

                    DisplayableItem.Event(
                        task = task,
                        timeRemaining = timeRemaining,
                        formattedTime = formattedTime
                    )
                }
            )

            // Agregar TODOs
            if (todos.isNotEmpty()) {
                add(DisplayableItem.TodoHeader)
                addAll(
                    todos.sortedWith(
                        compareBy({ it.isFinished }, { !it.isStarted })
                    ).map { task ->
                        DisplayableItem.Todo(
                            task = task,
                            timeRemaining = null,
                            formattedTime = when {
                                task.isFinished -> "✓ Completado"
                                task.isStarted -> "En progreso"
                                else -> "Pendiente"
                            }
                        )
                    }
                )
            }
        }
    }

    // NUEVA FUNCIÓN: Actualizar el tiempo en el UI state
    private fun startTimeUpdater() {
        viewModelScope.launch {
            timeManager.currentTimeFlow.collect { currentTime ->
                _uiState.update { it.copy(currentTime = currentTime) }

                // Actualizar última actividad periódicamente
                if (currentTime.second == 0) { // Solo cada minuto
                    preferencesRepository.updateLastActivityTime(System.currentTimeMillis())
                }
            }
        }
    }

    private fun startMidnightDetector() {
        viewModelScope.launch {
            while (true) {
                val now = LocalDateTime.now()
                val tomorrow = now.toLocalDate().plusDays(1).atStartOfDay()
                val millisUntilMidnight = Duration.between(now, tomorrow).toMillis()

                val delayTime = if (millisUntilMidnight > 24 * 60 * 60 * 1000) {
                    23 * 60 * 60 * 1000L
                } else {
                    millisUntilMidnight + 100
                }

                debugLog.add("🕐 MIDNIGHT: Esperando ${delayTime / 1000 / 60} minutos hasta próximo check")
                delay(delayTime)

                val newDate = LocalDate.now()
                if (newDate != _currentDate.value) {
                    debugLog.add("🌙 MIDNIGHT: ¡Nuevo día detectado! Mostrando diálogo...")
                    _showNewDayDialog.value = true
                }
            }
        }
    }

    fun onNewDayRefreshAccepted() {
        viewModelScope.launch {
            debugLog.add("🌙 MIDNIGHT: Usuario aceptó refresh de nuevo día")
            _showNewDayDialog.value = false
            _currentDate.value = LocalDate.now()
            eventBus.emit(SystemEvent.DayChanged)
            _uiState.update { it.copy(isLoading = true) }
            val today = LocalDate.now()
            calendarSyncUseCase.syncDateRange(today, today.plusDays(30), triggerReminders = true)
            alarmScheduler.scheduleNextAlarm()
        }
    }

    fun onNewDayRefreshDismissed() {
        debugLog.add("🌙 MIDNIGHT: Usuario canceló refresh de nuevo día")
        _showNewDayDialog.value = false
    }

    private fun startMonitoringAndSync() {
        viewModelScope.launch {
            val intent = Intent(context, ChapelotasNotificationService::class.java).apply {
                action = Constants.ACTION_START_MONITORING
            }
            context.startService(intent)

            _uiState.update { it.copy(isLoading = true) }
            val today = LocalDate.now()
            calendarSyncUseCase.syncDateRange(today, today.plusDays(30), triggerReminders = true)
        }
    }

    private fun checkPermissionsAndInitialSync() {
        viewModelScope.launch {
            if (appStatusManager.hasCalendarPermission() == PermissionStatus.DENIED) {
                _uiState.update { it.copy(needsPermission = true) }
            } else {
                _uiState.update { it.copy(needsPermission = false) }
                startMonitoringAndSync()
            }
        }
    }

    private fun subscribeToEvents() {
        // Escuchar eventos de alarma
        viewModelScope.launch {
            eventBus.alarmEvents.collect { event ->
                when (event) {
                    is AlarmEvent.AlarmScheduled -> {
                        delay(500)
                        checkNextAlarm()
                    }
                    is AlarmEvent.AlarmCancelled, is AlarmEvent.AlarmTriggered -> {
                        stopCountdown()
                        _uiState.update { it.copy(nextAlarmDuration = null) }
                    }
                }
            }
        }

        // Escuchar eventos del calendario
        viewModelScope.launch {
            eventBus.calendarEvents.collect { event ->
                when (event) {
                    is CalendarEvent.SyncCompleted -> {
                        _uiState.update { it.copy(isLoading = false, error = null) }
                        if (event.newTasksCount > 0) {
                            debugLog.add("VIEWMODEL: Sync completado con ${event.newTasksCount} tareas nuevas.")
                            alarmScheduler.scheduleNextAlarm()
                        }
                    }
                    is CalendarEvent.SyncFailed -> {
                        _uiState.update {
                            it.copy(isLoading = false, error = "Error sincronizando: ${event.error}")
                        }
                    }
                    else -> {}
                }
            }
        }

        // Escuchar eventos del sistema
        viewModelScope.launch {
            eventBus.systemEvents.collect { event ->
                if (event is SystemEvent.AlarmSettingsChanged) {
                    alarmScheduler.scheduleNextAlarm()
                }
            }
        }

        // NUEVO: Escuchar cambios en tareas para refrescar inmediatamente
        viewModelScope.launch {
            eventBus.taskEvents.collect { event ->
                when (event) {
                    is TaskEvent.TaskStatusChanged,
                    is TaskEvent.TaskAcknowledged,
                    is TaskEvent.TaskUpdated -> {
                        // El UI se actualizará automáticamente porque observamos el Flow de tareas
                        debugLog.add("VIEWMODEL: Evento de tarea recibido, UI se actualizará automáticamente")
                    }
                    else -> {}
                }
            }
        }
    }

    private fun startCountdown(duration: Duration) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remainingDuration = duration
            while (remainingDuration.isPositive()) {
                _uiState.update { it.copy(nextAlarmDuration = remainingDuration) }
                delay(1000)
                remainingDuration = remainingDuration.minusSeconds(1)
            }
            _uiState.update { it.copy(nextAlarmDuration = null) }
        }
    }

    private fun stopCountdown() {
        countdownJob?.cancel()
        countdownJob = null
    }

    private fun Duration.isPositive(): Boolean = !this.isNegative && !this.isZero

    private fun checkNextAlarm() {
        viewModelScope.launch {
            debugLog.add("VIEWMODEL: ==> checkNextAlarm() iniciado")
            stopCountdown()

            val finalHuaweiTime = preferencesRepository.getFinalHuaweiAlarmTime().first()
            if (finalHuaweiTime != null) {
                val now = LocalDateTime.now()
                if (finalHuaweiTime.isAfter(now)) {
                    val duration = Duration.between(now, finalHuaweiTime)
                    startCountdown(duration)
                    return@launch
                } else {
                    preferencesRepository.clearFinalHuaweiAlarmTime()
                }
            }

            try {
                val alarmManager = context.getSystemService<AlarmManager>()
                val nextAlarmClock = alarmManager?.nextAlarmClock
                if (nextAlarmClock != null && nextAlarmClock.showIntent?.creatorPackage == context.packageName) {
                    val triggerTime = Instant.ofEpochMilli(nextAlarmClock.triggerTime)
                    val now = Instant.now()
                    val duration = Duration.between(now, triggerTime)
                    if (!duration.isNegative) {
                        _uiState.update { it.copy(nextAlarmDuration = duration) }
                        startCountdown(duration)
                    } else {
                        _uiState.update { it.copy(nextAlarmDuration = null) }
                    }
                } else {
                    _uiState.update { it.copy(nextAlarmDuration = null) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(nextAlarmDuration = null) }
            }
        }
    }

    fun onPermissionGranted() {
        viewModelScope.launch {
            debugLog.add("VIEWMODEL: ✅ Permiso CONCEDIDO por el usuario desde HomeScreen.")
            _uiState.update { it.copy(needsPermission = false) }
            eventBus.emit(SystemEvent.PermissionGranted(permission = "android.permission.READ_CALENDAR"))
            startMonitoringAndSync()
        }
    }

    private fun checkIfBatteryProtectionIsNeeded() {
        viewModelScope.launch {
            val optimizacionDesactivada = appStatusManager.isBatteryOptimizationDisabled() == PermissionStatus.GRANTED
            if (!optimizacionDesactivada && preferencesRepository.areAlarmsConfigured()) {
                _uiState.update { it.copy(showBatteryProtectionDialog = true) }
            }
        }
    }

    fun onBatteryProtectionDialogDismissed() {
        viewModelScope.launch {
            preferencesRepository.setAlarmsConfigured(true)
            _uiState.update { it.copy(showBatteryProtectionDialog = false) }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
            eventBus.emit(TaskEvent.TaskDeleted(taskId = taskId))
        }
    }

    private fun performActionAndRespond(
        taskId: String,
        userActionText: String,
        actionType: String,
        taskFunction: suspend (String) -> Unit
    ) {
        viewModelScope.launch {
            taskRepository.addMessageToLog(taskId, userActionText, Sender.USUARIO, incrementCounter = false)
            taskFunction(taskId)
            val task = taskRepository.getTask(taskId) ?: return@launch
            val settings = preferencesRepository.observeAppSettings().first()
            val responseMessage = personalityProvider.get(
                personalityKey = settings.personalityProfile,
                contextKey = "action_confirmation.$actionType",
                placeholders = mapOf("{USER_NAME}" to settings.userName, "{TASK_NAME}" to task.title)
            )
            taskRepository.addMessageToLog(taskId, responseMessage, Sender.CHAPELOTAS, incrementCounter = true)
            eventBus.emit(
                TaskEvent.MessageAdded(taskId, responseMessage, Sender.CHAPELOTAS.name, true)
            )
        }
    }

    fun acknowledgeTask(taskId: String) {
        performActionAndRespond(taskId, "Entendido", "acknowledge") {
            taskRepository.acknowledgeTask(it)
            viewModelScope.launch {
                eventBus.emit(TaskEvent.TaskAcknowledged(taskId = it))
            }
        }
    }

    fun toggleFinishStatus(task: Task) {
        if (task.isFinished) {
            viewModelScope.launch {
                taskRepository.resetTaskStatus(task.id)
                reminderEngine.initializeOrUpdateAllReminders()
                eventBus.emit(TaskEvent.TaskStatusChanged(task.id, "REOPENED", false))
            }
        } else {
            performActionAndRespond(task.id, "Finalizar Tarea", "finish") {
                if (task.isTodo && !task.isStarted) {
                    taskRepository.startTask(it)
                }
                taskRepository.finishTask(it)
                notificationRepository.cancelTaskNotification(it)
                reminderEngine.initializeOrUpdateAllReminders()
                viewModelScope.launch {
                    eventBus.emit(TaskEvent.TaskStatusChanged(it, "FINISHED", true))
                }
            }
        }
    }
}