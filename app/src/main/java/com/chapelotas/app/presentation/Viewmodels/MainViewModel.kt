package com.chapelotas.app.presentation.viewmodels

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chapelotas.app.data.notifications.ChapelotasNotificationService
import com.chapelotas.app.domain.debug.DebugLog
import com.chapelotas.app.domain.models.Task
import com.chapelotas.app.domain.permissions.AppStatusManager
import com.chapelotas.app.domain.permissions.PermissionStatus
import com.chapelotas.app.domain.repositories.PreferencesRepository
import com.chapelotas.app.domain.repositories.TaskRepository
import com.chapelotas.app.domain.usecases.CalendarSyncUseCase
import com.chapelotas.app.domain.usecases.ReminderEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val calendarSyncUseCase: CalendarSyncUseCase,
    private val taskRepository: TaskRepository,
    private val preferencesRepository: PreferencesRepository,
    private val debugLog: DebugLog,
    private val appStatusManager: AppStatusManager,
    // Inyectamos el ReminderEngine para poder darle órdenes
    private val reminderEngine: ReminderEngine
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
        checkPermissionsAndInitialSync()
        checkIfBatteryProtectionIsNeeded()
    }

    private fun checkPermissionsAndInitialSync() {
        viewModelScope.launch {
            debugLog.add("VIEWMODEL: Verificando permisos...")
            _uiState.update { it.copy(isLoading = true) }

            if (appStatusManager.hasCalendarPermission() == PermissionStatus.DENIED) {
                debugLog.add("VIEWMODEL: 🔴 Permiso DENEGADO. Mostrando pantalla de solicitud.")
                _uiState.update { it.copy(isLoading = false, needsPermission = true) }
                return@launch
            }

            debugLog.add("VIEWMODEL: ✅ Permiso YA CONCEDIDO. Sincronizando e iniciando monitoreo...")
            startMonitoringService()
            syncInternal()
        }
    }

    fun onPermissionGranted() {
        viewModelScope.launch {
            debugLog.add("VIEWMODEL: ✅ Permiso CONCEDIDO por el usuario.")
            _uiState.update { it.copy(needsPermission = false, isLoading = true) }
            startMonitoringService()
            syncInternal()
        }
    }

    private fun startMonitoringService() {
        val intent = Intent(context, ChapelotasNotificationService::class.java).apply {
            action = ChapelotasNotificationService.ACTION_START_MONITORING
        }
        context.startService(intent)
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

    fun acknowledgeTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.acknowledgeTask(taskId)
        }
    }

    fun toggleFinishStatus(task: Task) {
        viewModelScope.launch {
            if (task.isFinished) {
                // El usuario quiere revivir una tarea terminada
                taskRepository.resetTaskStatus(task.id)
                // --- LA LÍNEA CLAVE ---
                // Inmediatamente después de revivirla, le damos la orden al motor
                // para que recalcule todo.
                reminderEngine.initializeOrUpdateAllReminders()
                // -------------------------
            } else {
                // El usuario quiere terminar una tarea pendiente
                taskRepository.finishTask(task.id)
                // También le pedimos al motor que actualice, para que cancele
                // cualquier alarma pendiente para esta tarea.
                reminderEngine.initializeOrUpdateAllReminders()
            }
        }
    }
}

data class MainUiState(
    val isLoading: Boolean = false,
    val needsPermission: Boolean = false,
    val error: String? = null,
    val showBatteryProtectionDialog: Boolean = false
)