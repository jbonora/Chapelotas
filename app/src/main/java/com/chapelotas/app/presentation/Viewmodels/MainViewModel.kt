package com.chapelotas.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chapelotas.app.domain.debug.DebugLog
import com.chapelotas.app.domain.events.AppEvent
import com.chapelotas.app.domain.events.EventBus
import com.chapelotas.app.domain.models.Task
import com.chapelotas.app.domain.repositories.CalendarRepository
import com.chapelotas.app.domain.repositories.NotificationRepository
import com.chapelotas.app.domain.repositories.PreferencesRepository
import com.chapelotas.app.domain.repositories.TaskRepository
import com.chapelotas.app.domain.usecases.CalendarSyncUseCase
import com.chapelotas.app.domain.usecases.ReminderEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val calendarSyncUseCase: CalendarSyncUseCase,
    private val taskRepository: TaskRepository,
    private val notificationRepository: NotificationRepository,
    private val preferencesRepository: PreferencesRepository,
    private val reminderEngine: ReminderEngine,
    private val calendarRepository: CalendarRepository,
    private val eventBus: EventBus,
    private val debugLog: DebugLog
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val todayTasks: StateFlow<List<Task>> = taskRepository.observeTasksForDate(LocalDate.now())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        debugLog.add("VIEWMODEL: ==> ViewModel inicializado.")

        viewModelScope.launch {
            debugLog.add("VIEWMODEL: Iniciando servicio en segundo plano desde el arranque.")
            notificationRepository.startNotificationService()
        }

        checkPermissionsAndInitialSync()
        listenToEvents()
        checkIfBatteryProtectionIsNeeded()
    }

    private fun checkIfBatteryProtectionIsNeeded() {
        viewModelScope.launch {
            if (!preferencesRepository.areAlarmsConfigured()) {
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

    private fun listenToEvents() {
        viewModelScope.launch {
            eventBus.events.collect { event ->
                when(event) {
                    is AppEvent.SyncCompleted -> {
                        debugLog.add("VIEWMODEL: 📬 Evento 'SyncCompleted' RECIBIDO. Inicializando recordatorios.")
                        reminderEngine.initializeTaskReminders()
                    }
                    else -> { /* No-op */ }
                }
            }
        }
    }

    private fun checkPermissionsAndInitialSync() {
        viewModelScope.launch {
            debugLog.add("VIEWMODEL: Verificando permisos...")
            _uiState.update { it.copy(isLoading = true) }
            if (!calendarRepository.hasCalendarReadPermission()) {
                debugLog.add("VIEWMODEL: 🔴 Permiso DENEGADO. Mostrando pantalla de solicitud.")
                _uiState.update { it.copy(isLoading = false, needsPermission = true) }
                return@launch
            }
            debugLog.add("VIEWMODEL: ✅ Permiso YA CONCEDIDO. Sincronizando...")
            syncInternal()
        }
    }

    fun onPermissionGranted() {
        viewModelScope.launch {
            debugLog.add("VIEWMODEL: ✅ Permiso CONCEDIDO por el usuario.")
            _uiState.update { it.copy(needsPermission = false, isLoading = true) }
            syncInternal()
        }
    }

    private suspend fun syncInternal() {
        try {
            calendarSyncUseCase.syncToday()
        } catch (e: Exception) {
            debugLog.add("VIEWMODEL: 🔴 ERROR durante la sincronización: ${e.message}")
            _uiState.update { it.copy(error = "Error sincronizando: ${e.message}") }
        } finally {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun acknowledgeTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.acknowledgeTask(taskId)
            debugLog.add("UI ACTION: Tarea '$taskId' marcada como ACEPTADA.")
        }
    }

    fun toggleFinishStatus(task: Task) {
        viewModelScope.launch {
            if (task.isFinished) {
                taskRepository.resetTaskStatus(task.id)
                debugLog.add("UI ACTION: Tarea '${task.title}' ha sido REVIVIDA.")
            } else {
                taskRepository.finishTask(task.id)
                debugLog.add("UI ACTION: Tarea '${task.title}' marcada como TERMINADA desde la UI.")
            }
        }
    }
}

data class MainUiState(
    val isLoading: Boolean = false,
    val needsPermission: Boolean = false,
    val error: String? = null,
    val lastSyncTime: Long? = null,
    val showBatteryProtectionDialog: Boolean = false
)