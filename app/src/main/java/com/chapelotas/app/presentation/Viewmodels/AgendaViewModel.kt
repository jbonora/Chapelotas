package com.chapelotas.app.presentation.viewmodels

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chapelotas.app.data.notifications.ChapelotasNotificationService
import com.chapelotas.app.di.Constants
import com.chapelotas.app.domain.debug.DebugLog
import com.chapelotas.app.domain.events.*
import com.chapelotas.app.domain.models.*
import com.chapelotas.app.domain.permissions.AppStatusManager
import com.chapelotas.app.domain.permissions.PermissionStatus
import com.chapelotas.app.domain.personality.PersonalityProvider
import com.chapelotas.app.domain.repositories.NotificationRepository
import com.chapelotas.app.domain.repositories.PreferencesRepository
import com.chapelotas.app.domain.repositories.TaskRepository
import com.chapelotas.app.domain.time.TimeManager
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class AgendaUiState(
    val daysInfo: List<DayInfo> = emptyList(),
    val todayItems: List<DisplayableItem> = emptyList(),
    val isLoading: Boolean = true,
    val needsPermission: Boolean = false,
    val error: String? = null,
    val showBatteryProtectionDialog: Boolean = false,
    val nextAlarmDuration: Duration? = null,
    val currentTime: LocalDateTime = LocalDateTime.now()
)

@HiltViewModel
class AgendaViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskRepository: TaskRepository,
    private val preferencesRepository: PreferencesRepository,
    private val calendarSyncUseCase: CalendarSyncUseCase,
    private val reminderEngine: ReminderEngine,
    private val alarmScheduler: AlarmSchedulerUseCase,
    private val timeManager: TimeManager,
    private val appStatusManager: AppStatusManager,
    private val notificationRepository: NotificationRepository,
    private val personalityProvider: PersonalityProvider,
    private val eventBus: EventBus,
    private val debugLog: DebugLog
) : ViewModel() {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val _uiState = MutableStateFlow(AgendaUiState())
    val uiState: StateFlow<AgendaUiState> = _uiState.asStateFlow()

    private val _showNewDayDialog = MutableStateFlow(false)
    val showNewDayDialog: StateFlow<Boolean> = _showNewDayDialog.asStateFlow()

    private val _currentDate = MutableStateFlow(LocalDate.now())

    init {
        debugLog.add("VIEWMODEL: ==> AgendaViewModel inicializado.")
        subscribeToEvents()
        checkPermissionsAndInitialSync()
        checkNextAlarm()
        checkIfBatteryProtectionIsNeeded()
        startMidnightDetector()
        loadAgendaData()
    }

    private fun loadAgendaData() {
        viewModelScope.launch {
            val startDate = _currentDate.value
            val endDate = startDate.plusDays(6)

            combine(
                taskRepository.observeTasksForDateRange(startDate, endDate),
                preferencesRepository.observeAppSettings(),
                timeManager.currentTimeFlow
            ) { tasks, settings, currentTime ->
                val weekDays = (0..6).map { startDate.plusDays(it.toLong()) }
                val daysInfoList = weekDays.map { day ->
                    val allTasksForDay = tasks.filter { it.scheduledTime.toLocalDate() == day }
                    val (allDayTasks, timedTasks) = allTasksForDay.partition { it.isAllDay }
                    val schedule = calculateDailyScheduleForDay(day, timedTasks, settings)
                    DayInfo(
                        date = day,
                        timedTasks = timedTasks.sortedBy { it.scheduledTime },
                        allDayTasks = allDayTasks,
                        schedule = schedule
                    )
                }

                val tasksToday = tasks.filter { it.scheduledTime.toLocalDate() == _currentDate.value }
                val todayItems = buildDisplayableItems(tasksToday, currentTime)

                _uiState.update {
                    it.copy(
                        daysInfo = daysInfoList,
                        todayItems = todayItems,
                        isLoading = false,
                        currentTime = currentTime
                    )
                }
            }.collect()
        }
    }

    private fun buildDisplayableItems(tasks: List<Task>, currentTime: LocalDateTime): List<DisplayableItem> {
        val allTodos = tasks.filter { it.taskType == TaskType.TODO }
        val allEvents = tasks.filter { it.taskType == TaskType.EVENT }

        val pastEvents = allEvents
            .filter { (it.endTime ?: it.scheduledTime.plusHours(1)).isBefore(currentTime) }
            .sortedBy { it.scheduledTime }

        val upcomingEvents = allEvents
            .filter { !(it.endTime ?: it.scheduledTime.plusHours(1)).isBefore(currentTime) }
            .sortedBy { it.scheduledTime }

        return buildList {
            addAll(
                pastEvents.map { task ->
                    val formattedTime = task.scheduledTime.format(timeFormatter)
                    DisplayableItem.Event(task = task, timeRemaining = timeManager.getTimeUntil(task.scheduledTime), formattedTime = formattedTime)
                }
            )

            if (allTodos.isNotEmpty()) {
                add(DisplayableItem.TodoHeader)
                addAll(
                    allTodos.sortedWith(compareBy({ it.isFinished }, { !it.isStarted })).map { task ->
                        DisplayableItem.Todo(
                            task = task,
                            formattedTime = when {
                                task.isFinished -> "✓ Completado"
                                task.isStarted -> "En progreso"
                                else -> "Pendiente"
                            }
                        )
                    }
                )
            }

            addAll(
                upcomingEvents.map { task ->
                    val timeRemaining = if (!task.isFinished) timeManager.getTimeUntil(task.scheduledTime) else null
                    val formattedTime = when {
                        task.isFinished -> "✓ Finalizado"
                        timeRemaining != null -> {
                            val timeStr = timeManager.formatTimeRemaining(timeRemaining)
                            val scheduleStr = task.scheduledTime.format(timeFormatter)
                            "$scheduleStr ($timeStr)"
                        }
                        else -> task.scheduledTime.format(timeFormatter)
                    }
                    DisplayableItem.Event(task = task, timeRemaining = timeRemaining, formattedTime = formattedTime)
                }
            )
        }
    }

    // --- FUNCIÓN RESTAURADA Y COMPLETADA ---
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
    }

    // --- FUNCIÓN RESTAURADA ---
    private fun startMidnightDetector() {
        viewModelScope.launch {
            while (true) {
                val now = LocalDateTime.now()
                val tomorrow = now.toLocalDate().plusDays(1).atStartOfDay()
                val millisUntilMidnight = Duration.between(now, tomorrow).toMillis()

                // Esperar un poco más de medianoche para estar seguros
                val delayTime = if (millisUntilMidnight > 0) millisUntilMidnight + 1000 else 24 * 60 * 60 * 1000

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

    // --- FUNCIÓN RESTAURADA ---
    fun onNewDayRefreshAccepted() {
        viewModelScope.launch {
            debugLog.add("🌙 MIDNIGHT: Usuario aceptó refresh de nuevo día")
            _showNewDayDialog.value = false
            _currentDate.value = LocalDate.now()
            eventBus.emit(SystemEvent.DayChanged)

            _uiState.update { it.copy(isLoading = true) }
            calendarSyncUseCase.syncDateRange(_currentDate.value, _currentDate.value.plusDays(30), triggerReminders = true)
            alarmScheduler.scheduleNextAlarm()
        }
    }

    // --- FUNCIÓN RESTAURADA ---
    fun onNewDayRefreshDismissed() {
        debugLog.add("🌙 MIDNIGHT: Usuario canceló refresh de nuevo día")
        _showNewDayDialog.value = false
    }

    // --- FUNCIÓN RESTAURADA ---
    private fun checkIfBatteryProtectionIsNeeded() {
        viewModelScope.launch {
            val optimizacionDesactivada = appStatusManager.isBatteryOptimizationDisabled() == PermissionStatus.GRANTED
            // Solo mostrar si el setup ya se completó una vez
            if (!optimizacionDesactivada && !preferencesRepository.isFirstTimeUser()) {
                _uiState.update { it.copy(showBatteryProtectionDialog = true) }
            }
        }
    }

    // --- FUNCIÓN RESTAURADA ---
    fun onBatteryProtectionDialogDismissed() {
        viewModelScope.launch {
            // Marcar como configurado para no volver a preguntar en esta sesión
            _uiState.update { it.copy(showBatteryProtectionDialog = false) }
        }
    }

    fun onPermissionGranted() {
        viewModelScope.launch {
            _uiState.update { it.copy(needsPermission = false) }
            startMonitoringAndSync()
        }
    }

    fun acknowledgeTask(taskId: String) = performActionAndRespond(taskId, "Entendido", "acknowledge") {
        taskRepository.acknowledgeTask(it)
        eventBus.emit(TaskEvent.TaskAcknowledged(taskId = it))
    }

    fun toggleFinishStatus(task: Task) {
        if (task.isFinished) {
            performActionAndRespond(task.id, "Reabrir Tarea", "reset") {
                taskRepository.resetTaskStatus(it)
                reminderEngine.initializeOrUpdateAllReminders()
                eventBus.emit(TaskEvent.TaskStatusChanged(it, "REOPENED", false))
            }
        } else {
            performActionAndRespond(task.id, "Finalizar Tarea", "finish") {
                if (task.isTodo && !task.isStarted) taskRepository.startTask(it)
                taskRepository.finishTask(it)
                notificationRepository.cancelTaskNotification(it)
                reminderEngine.initializeOrUpdateAllReminders()
                eventBus.emit(TaskEvent.TaskStatusChanged(it, "FINISHED", true))
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
            eventBus.emit(TaskEvent.TaskDeleted(taskId = taskId))
        }
    }

    private fun checkPermissionsAndInitialSync() {
        viewModelScope.launch { if (appStatusManager.hasCalendarPermission() == PermissionStatus.DENIED) { _uiState.update { it.copy(needsPermission = true) } } else { _uiState.update { it.copy(needsPermission = false) }; startMonitoringAndSync() } }
    }

    private fun startMonitoringAndSync() {
        viewModelScope.launch {
            val intent = Intent(context, ChapelotasNotificationService::class.java).apply {
                action = Constants.ACTION_START_MONITORING
            }
            context.startService(intent)
            _uiState.update { it.copy(isLoading = true) }
            calendarSyncUseCase.syncDateRange(_currentDate.value, _currentDate.value.plusDays(30), triggerReminders = true)
        }
    }

    private fun checkNextAlarm() {
        viewModelScope.launch {
            val finalHuaweiTime = preferencesRepository.getFinalHuaweiAlarmTime().first()
            if (finalHuaweiTime != null) {
                val now = LocalDateTime.now()
                if (finalHuaweiTime.isAfter(now)) {
                    _uiState.update { it.copy(nextAlarmDuration = Duration.between(now, finalHuaweiTime)) }
                    return@launch
                } else {
                    preferencesRepository.clearFinalHuaweiAlarmTime()
                }
            }
            try {
                val alarmManager = context.getSystemService<AlarmManager>()
                val nextAlarmClock = alarmManager?.nextAlarmClock
                if (nextAlarmClock != null && nextAlarmClock.showIntent?.creatorPackage == context.packageName) {
                    val duration = Duration.between(Instant.now(), Instant.ofEpochMilli(nextAlarmClock.triggerTime))
                    _uiState.update { it.copy(nextAlarmDuration = if (!duration.isNegative) duration else null) }
                } else {
                    _uiState.update { it.copy(nextAlarmDuration = null) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(nextAlarmDuration = null) }
            }
        }
    }

    // --- Lógica de cálculo (sin cambios) ---
    private fun getLocalTime(timeString: String, defaultTime: LocalTime): LocalTime {
        return try { LocalTime.parse(timeString) } catch (e: Exception) { defaultTime }
    }
    private fun calculateDailyScheduleForDay(currentDate: LocalDate, tasksForDay: List<Task>, settings: AppSettings): DailySchedule {
        val workStart = getLocalTime(settings.workStartTime, LocalTime.of(8, 0)); val workEnd = getLocalTime(settings.workEndTime, LocalTime.of(23, 0)); val lunchStart = getLocalTime(settings.lunchStartTime, LocalTime.of(13, 0)); val lunchEnd = getLocalTime(settings.lunchEndTime, LocalTime.of(14, 0)); val dinnerStart = getLocalTime(settings.dinnerStartTime, LocalTime.of(20, 0)); val dinnerEnd = getLocalTime(settings.dinnerEndTime, LocalTime.of(21, 0)); val allScheduleItems = buildCombinedScheduleForDay(workStart, workEnd, tasksForDay)
        return DailySchedule(morning = getTimeBlockForPeriod(currentDate, workStart, lunchStart, allScheduleItems), lunch = getTimeBlockForPeriod(currentDate, lunchStart, lunchEnd, allScheduleItems), afternoon = getTimeBlockForPeriod(currentDate, lunchEnd, dinnerStart, allScheduleItems), dinner = getTimeBlockForPeriod(currentDate, dinnerStart, dinnerEnd, allScheduleItems), night = getTimeBlockForPeriod(currentDate, dinnerEnd, workEnd, allScheduleItems))
    }
    private fun buildCombinedScheduleForDay(workStart: LocalTime, workEnd: LocalTime, tasks: List<Task>): List<ScheduleItem> {
        if (!workStart.isBefore(workEnd)) return emptyList(); val busyBlocks = tasks.flatMap { task -> val taskStart = task.scheduledTime.toLocalTime(); val taskEnd = task.endTime?.toLocalTime() ?: taskStart.plusHours(1); val travel = task.travelTimeMinutes.toLong(); val travelStartTime = taskStart.minusMinutes(travel); val travelEndTime = taskEnd.plusMinutes(travel); listOfNotNull(if (travel > 0) ScheduleItem.Busy(task.title, travelStartTime, taskStart, "Viaje ida", task.calendarEventId, task.isFromCalendar) else null, ScheduleItem.Busy(task.title, taskStart, taskEnd, task.title.split(" ").take(3).joinToString(" "), task.calendarEventId, task.isFromCalendar), if (travel > 0) ScheduleItem.Busy(task.title, taskEnd, travelEndTime, "Viaje vuelta", task.calendarEventId, task.isFromCalendar) else null) }.sortedBy { it.start }; val mergedBusyBlocks = mutableListOf<ScheduleItem.Busy>(); for (block in busyBlocks) { if (mergedBusyBlocks.isEmpty() || block.start >= mergedBusyBlocks.last().end) { mergedBusyBlocks.add(block) } else { val last = mergedBusyBlocks.removeAt(mergedBusyBlocks.lastIndex); val newEnd = if (block.end.isAfter(last.end)) block.end else last.end; val newDescription = if (last.description.startsWith("Viaje") && !block.description.startsWith("Viaje")) block.description else last.description; mergedBusyBlocks.add(last.copy(end = newEnd, description = newDescription, calendarEventId = block.calendarEventId, isFromCalendar = block.isFromCalendar)) } }; val combinedSchedule = mutableListOf<ScheduleItem>(); var cursor = workStart; for (busyBlock in mergedBusyBlocks) { if (busyBlock.start > cursor) { val freeSlot = TimeSlot(cursor, busyBlock.start, "", 0); combinedSchedule.addAll(smartSplitSlot(freeSlot, 60, 30).map { ScheduleItem.Free(it) }) }; combinedSchedule.add(busyBlock); cursor = busyBlock.end }; if (cursor < workEnd) { val finalFreeSlot = TimeSlot(cursor, workEnd, "", 0); combinedSchedule.addAll(smartSplitSlot(finalFreeSlot, 60, 30).map { ScheduleItem.Free(it) }) }; return combinedSchedule
    }
    private fun getTimeBlockForPeriod(currentDate: LocalDate, periodStart: LocalTime, periodEnd: LocalTime, allScheduleItems: List<ScheduleItem>): TimeBlock {
        val now = LocalTime.now(); val today = LocalDate.now(); if (currentDate.isEqual(today) && periodEnd.isBefore(now)) return TimeBlock(status = BlockStatus.IN_THE_PAST); if(!periodStart.isBefore(periodEnd)) return TimeBlock(status = BlockStatus.OUTSIDE_WORK_HOURS); var itemsInPeriod = allScheduleItems.filter { item -> val itemStart = when(item) { is ScheduleItem.Busy -> item.start; is ScheduleItem.Free -> item.slot.start }; val itemEnd = when(item) { is ScheduleItem.Busy -> item.end; is ScheduleItem.Free -> item.slot.end }; itemStart < periodEnd && itemEnd > periodStart }; if (currentDate.isEqual(today)) { itemsInPeriod = itemsInPeriod.filter { val itemEnd = when(it) { is ScheduleItem.Busy -> it.end; is ScheduleItem.Free -> it.slot.end }; itemEnd.isAfter(now) } }; val hasFreeSlots = itemsInPeriod.any { it is ScheduleItem.Free }; return if (itemsInPeriod.isNotEmpty()) { TimeBlock(status = if (hasFreeSlots) BlockStatus.HAS_SLOTS else BlockStatus.FULLY_BOOKED, items = itemsInPeriod) } else { TimeBlock(status = BlockStatus.FULLY_BOOKED) }
    }
    private fun smartSplitSlot(slot: TimeSlot, chunkDurationMinutes: Long, minFragmentDurationMinutes: Long): List<TimeSlot> {
        val chunks = mutableListOf<TimeSlot>(); var currentStartTime = slot.start; if (currentStartTime.minute % 15 != 0) { currentStartTime = currentStartTime.plusMinutes(15 - (currentStartTime.minute % 15).toLong()) }; val nextFullHour = currentStartTime.truncatedTo(java.time.temporal.ChronoUnit.HOURS).plusHours(1); if ((nextFullHour.isBefore(slot.end) || nextFullHour == slot.end) && currentStartTime.isBefore(nextFullHour) ) { val fragmentDuration = Duration.between(currentStartTime, nextFullHour).toMinutes(); if (fragmentDuration >= minFragmentDurationMinutes) { val description = "Bloque de $fragmentDuration min"; chunks.add(TimeSlot(currentStartTime, nextFullHour, description, fragmentDuration)) }; currentStartTime = nextFullHour }; while (Duration.between(currentStartTime, slot.end).toMinutes() >= chunkDurationMinutes) { val nextEndTime = currentStartTime.plusMinutes(chunkDurationMinutes); if (!nextEndTime.isAfter(slot.end)) { val description = "Bloque de $chunkDurationMinutes min"; chunks.add(TimeSlot(currentStartTime, nextEndTime, description, chunkDurationMinutes)) }; currentStartTime = nextEndTime }; val remainingMinutes = Duration.between(currentStartTime, slot.end).toMinutes(); if (remainingMinutes >= minFragmentDurationMinutes) { val description = "Bloque de $remainingMinutes min"; chunks.add(TimeSlot(currentStartTime, slot.end, description, remainingMinutes)) }; return chunks
    }
    private fun performActionAndRespond(taskId: String, userActionText: String, actionType: String, taskFunction: suspend (String) -> Unit) {
        viewModelScope.launch { taskRepository.addMessageToLog(taskId, userActionText, Sender.USUARIO, incrementCounter = false); taskFunction(taskId); val task = taskRepository.getTask(taskId) ?: return@launch; val settings = preferencesRepository.observeAppSettings().first(); val responseMessage = personalityProvider.get(personalityKey = settings.personalityProfile, contextKey = "action_confirmation.$actionType", placeholders = mapOf("{USER_NAME}" to settings.userName, "{TASK_NAME}" to task.title)); taskRepository.addMessageToLog(taskId, responseMessage, Sender.CHAPELOTAS, incrementCounter = true); eventBus.emit(TaskEvent.MessageAdded(taskId, responseMessage, Sender.CHAPELOTAS.name, true)) }
    }
}