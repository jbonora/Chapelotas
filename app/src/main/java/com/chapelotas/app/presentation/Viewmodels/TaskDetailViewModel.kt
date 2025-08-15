package com.chapelotas.app.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chapelotas.app.domain.events.EventBus
import com.chapelotas.app.domain.events.TaskEvent
import com.chapelotas.app.domain.models.AppSettings
import com.chapelotas.app.domain.models.LocationContext
import com.chapelotas.app.domain.models.Sender
import com.chapelotas.app.domain.models.Task
import com.chapelotas.app.domain.personality.PersonalityProvider
import com.chapelotas.app.domain.repositories.NotificationRepository
import com.chapelotas.app.domain.repositories.PreferencesRepository
import com.chapelotas.app.domain.repositories.TaskRepository
import com.chapelotas.app.domain.usecases.ReminderEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskDetailUiState(
    val task: Task? = null,
    val settings: AppSettings = AppSettings()
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val preferencesRepository: PreferencesRepository,
    private val personalityProvider: PersonalityProvider,
    private val notificationRepository: NotificationRepository,
    private val reminderEngine: ReminderEngine,
    private val eventBus: EventBus,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId: String = savedStateHandle.get<String>("taskId")!!

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            taskRepository.resetUnreadMessageCount(taskId)
            combine(
                taskRepository.observeTask(taskId),
                preferencesRepository.observeAppSettings()
            ) { task, settings ->
                TaskDetailUiState(task = task, settings = settings)
            }.collect {
                _uiState.value = it
            }
        }
    }

    fun updateTaskLocation(context: String, customTravelTime: Int? = null) {
        viewModelScope.launch {
            val currentTask = uiState.value.task ?: return@launch
            val oldLocationContext = currentTask.locationContext
            val oldTravelTime = currentTask.travelTimeMinutes

            val travelTime = customTravelTime ?: when (context) {
                LocationContext.NEARBY -> uiState.value.settings.travelTimeNearbyMinutes
                LocationContext.FAR -> uiState.value.settings.travelTimeFarMinutes
                else -> 0
            }

            // Actualizar en la base de datos
            taskRepository.updateTaskLocation(taskId, context, travelTime)

            // CAMBIO IMPORTANTE: Emitir eventos al bus para que ReminderEngine lo procese
            if (oldLocationContext != context) {
                eventBus.emit(TaskEvent.TaskUpdated(
                    taskId = taskId,
                    field = "locationContext",
                    oldValue = oldLocationContext,
                    newValue = context
                ))
            }

            if (oldTravelTime != travelTime) {
                eventBus.emit(TaskEvent.TaskUpdated(
                    taskId = taskId,
                    field = "travelTime",
                    oldValue = oldTravelTime,
                    newValue = travelTime
                ))
            }
        }
    }

    private fun performActionAndRespond(
        userActionText: String,
        actionType: String,
        taskFunction: suspend (String) -> Unit
    ) {
        viewModelScope.launch {
            val task = uiState.value.task ?: return@launch

            // 1. Se añade la acción del usuario al log
            taskRepository.addMessageToLog(taskId, userActionText, Sender.USUARIO, incrementCounter = false)

            // 2. Se ejecuta la lógica principal
            taskFunction(taskId)

            // 3. Se genera y añade la respuesta de la app
            val settings = uiState.value.settings
            val responseMessage = personalityProvider.get(
                personalityKey = settings.personalityProfile,
                contextKey = "action_confirmation.$actionType",
                placeholders = mapOf("{USER_NAME}" to settings.userName, "{TASK_NAME}" to task.title)
            )
            taskRepository.addMessageToLog(taskId, responseMessage, Sender.CHAPELOTAS, incrementCounter = true)
            eventBus.emit(
                TaskEvent.MessageAdded(taskId, responseMessage, Sender.CHAPELOTAS.name, true)
            )

            // 4. Reset contador ya que el usuario está viendo la pantalla
            taskRepository.resetUnreadMessageCount(taskId)
        }
    }

    fun acknowledgeTask() {
        performActionAndRespond("Entendido", "acknowledge") {
            taskRepository.acknowledgeTask(it)
            viewModelScope.launch { eventBus.emit(TaskEvent.TaskAcknowledged(taskId = it)) }
        }
    }

    fun toggleFinishStatus() {
        val task = uiState.value.task ?: return
        if (task.isFinished) {
            performActionAndRespond("Reabrir Tarea", "reset") {
                taskRepository.resetTaskStatus(it)
                reminderEngine.initializeOrUpdateAllReminders()
                viewModelScope.launch { eventBus.emit(TaskEvent.TaskStatusChanged(it, "REOPENED", false)) }
            }
        } else {
            performActionAndRespond("Finalizar Tarea", "finish") {
                if (task.isTodo && !task.isStarted) {
                    taskRepository.startTask(it)
                }
                taskRepository.finishTask(it)
                notificationRepository.cancelTaskNotification(it)
                reminderEngine.initializeOrUpdateAllReminders()
                viewModelScope.launch { eventBus.emit(TaskEvent.TaskStatusChanged(it, "FINISHED", true)) }
            }
        }
    }
}