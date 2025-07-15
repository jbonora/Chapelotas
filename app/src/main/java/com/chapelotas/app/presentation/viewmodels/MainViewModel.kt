package com.chapelotas.app.presentation.viewmodels

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.chapelotas.app.data.database.ChapelotasDatabase
import com.chapelotas.app.data.database.entities.ConversationLog
import com.chapelotas.app.data.database.entities.DayPlan
import com.chapelotas.app.data.database.entities.EventResolutionStatus
import com.chapelotas.app.data.notifications.TestCallWorker
import com.chapelotas.app.domain.entities.ChapelotasNotification
import com.chapelotas.app.domain.events.ChapelotasEvent
import com.chapelotas.app.domain.events.ChapelotasEventBus
import com.chapelotas.app.domain.repositories.CalendarRepository
import com.chapelotas.app.domain.repositories.NotificationRepository
import com.chapelotas.app.domain.usecases.NotificationActionHandler
import com.chapelotas.app.domain.usecases.SetupInitialConfigurationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

data class MainUiState(
    val isLoading: Boolean = true,
    val isFirstTimeUser: Boolean = false,
    val requiresPermissions: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: ChapelotasDatabase,
    private val notificationActionHandler: NotificationActionHandler,
    private val eventBus: ChapelotasEventBus,
    private val setupInitialConfigurationUseCase: SetupInitialConfigurationUseCase,
    private val calendarRepository: CalendarRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _notificationToShow = MutableStateFlow<ChapelotasNotification?>(null)
    val notificationToShow: StateFlow<ChapelotasNotification?> = _notificationToShow.asStateFlow()

    val conversationHistory: StateFlow<List<ConversationLog>> = database.conversationLogDao()
        .observeConversationHistory()
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    val dayPlan: StateFlow<DayPlan?> = database.dayPlanDao().observeTodayPlan()
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = null)

    // Flow para eventos de navegación
    val navigationEvents = eventBus.events.filter { event ->
        event is ChapelotasEvent.NavigateToPlan
    }

    init {
        checkInitialSetup()
    }

    private fun checkInitialSetup() {
        viewModelScope.launch {
            _uiState.value = uiState.value.copy(isLoading = true)
            database.dayPlanDao().getOrCreateTodayPlan()
            setupInitialConfigurationUseCase().fold(
                onSuccess = { result ->
                    val needsPermissionsNow = !calendarRepository.hasCalendarReadPermission()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isFirstTimeUser = result.isFirstTimeSetup,
                        requiresPermissions = needsPermissionsNow
                    )
                    if (result.isFirstTimeSetup && !needsPermissionsNow) {
                        eventBus.tryEmit(ChapelotasEvent.ServiceStarted("InitialSetup"))
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = error.message)
                }
            )
        }
    }

    fun onPermissionsGranted() {
        _uiState.value = _uiState.value.copy(requiresPermissions = false)
    }

    fun testCriticalCall() {
        val testCallWorkRequest = OneTimeWorkRequestBuilder<TestCallWorker>().build()
        WorkManager.getInstance(context).enqueue(testCallWorkRequest)
    }

    fun setWorkStartTime(time: LocalTime) {
        viewModelScope.launch {
            dayPlan.value?.let {
                database.dayPlanDao().update(it.copy(workStartTime = time))
            }
        }
    }

    fun setWorkEndTime(time: LocalTime) {
        viewModelScope.launch {
            dayPlan.value?.let {
                database.dayPlanDao().update(it.copy(workEndTime = time))
            }
        }
    }

    fun set24hMode(enabled: Boolean) {
        viewModelScope.launch {
            dayPlan.value?.let {
                database.dayPlanDao().update(it.copy(is24hMode = enabled))
            }
        }
    }

    fun handleIntent(intent: Intent?) {
        val message = intent?.getStringExtra("notification_message")
        val notificationId = intent?.getLongExtra("notification_id", -1L)

        if (!message.isNullOrEmpty() && notificationId != null && notificationId != -1L) {
            viewModelScope.launch {
                val scheduledNotification = database.notificationDao().getNotification(notificationId)
                if (scheduledNotification != null && scheduledNotification.isActive()) {
                    _notificationToShow.value = ChapelotasNotification(
                        id = scheduledNotification.notificationId.toString(),
                        eventId = scheduledNotification.eventId.split("_").first().toLongOrNull() ?: 0L,
                        scheduledTime = scheduledNotification.scheduledTime,
                        message = message,
                        priority = com.chapelotas.app.domain.entities.NotificationPriority.valueOf(scheduledNotification.priority.name),
                        type = com.chapelotas.app.domain.entities.NotificationType.valueOf(scheduledNotification.type.name)
                    )
                }
            }
        }
    }

    fun dismissNotificationDialog() {
        _notificationToShow.value = null
    }

    fun approveAiPlan(eventId: String) {
        viewModelScope.launch {
            try {
                database.eventPlanDao().getEvent(eventId)?.let {
                    database.eventPlanDao().update(it.copy(aiPlanStatus = "APPROVED"))
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Error aprobando plan: ${e.message}")
            }
        }
    }

    fun setSarcasticMode(enabled: Boolean) {
        viewModelScope.launch {
            try {
                database.dayPlanDao().getTodayPlan()?.let {
                    database.dayPlanDao().updateSarcasticMode(LocalDate.now(), enabled)
                    eventBus.tryEmit(ChapelotasEvent.SarcasticModeToggled(enabled))
                    Log.d("MainViewModel", "Sarcastic mode set to $enabled")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Error actualizando modo sarcástico: ${e.message}")
            }
        }
    }

    fun markNotificationAsDone(notificationId: Long) {
        viewModelScope.launch {
            val notification = database.notificationDao().getNotification(notificationId) ?: return@launch
            notificationActionHandler.handleDismiss(notificationId, notification.eventId)
            dismissNotificationDialog()
        }
    }

    fun snoozeNotification(notificationId: Long, minutes: Int) {
        viewModelScope.launch {
            val notification = database.notificationDao().getNotification(notificationId) ?: return@launch
            notificationActionHandler.handleSnooze(notificationId, notification.eventId, minutes)
            dismissNotificationDialog()
        }
    }

    /**
     * Maneja las acciones de los botones en los mensajes del chat
     */
    fun handleEventAction(eventId: String, action: String) {
        if (eventId.isEmpty()) return

        viewModelScope.launch {
            try {
                when (action) {
                    "done" -> {
                        // Buscar el evento y marcarlo como completado
                        val event = database.eventPlanDao().getEvent(eventId)
                        if (event != null) {
                            // Actualizar estado del evento
                            database.eventPlanDao().updateResolutionStatus(
                                eventId,
                                EventResolutionStatus.COMPLETED
                            )

                            // Registrar en el log
                            database.conversationLogDao().insert(
                                ConversationLog(
                                    timestamp = LocalDateTime.now(ZoneId.systemDefault()),
                                    role = "user",
                                    content = "✅ Marqué como completado: ${event.title}",
                                    eventId = eventId
                                )
                            )

                            // Generar respuesta de Chapelotas
                            val dayPlan = database.dayPlanDao().getTodayPlan()
                            val response = if (dayPlan?.sarcasticMode == true) {
                                "¡Mirá vos! Al fin hiciste algo. ${event.title} completado. 👏"
                            } else {
                                "Excelente, ${event.title} ha sido marcado como completado. ✅"
                            }

                            database.conversationLogDao().insert(
                                ConversationLog(
                                    timestamp = LocalDateTime.now(ZoneId.systemDefault()).plusSeconds(1),
                                    role = "assistant",
                                    content = response
                                )
                            )

                            // Cancelar notificaciones pendientes para este evento
                            database.notificationDao().deleteNotificationsForEvent(eventId)
                        }
                    }

                    "snooze" -> {
                        // Posponer todas las notificaciones del evento 15 minutos
                        val event = database.eventPlanDao().getEvent(eventId)
                        if (event != null) {
                            val notifications = database.notificationDao().getActiveNotificationsForEvent(eventId)
                            val snoozeTime = LocalDateTime.now(ZoneId.systemDefault()).plusMinutes(15)

                            notifications.forEach { notification ->
                                database.notificationDao().snooze(
                                    notification.notificationId,
                                    snoozeTime
                                )
                            }

                            // Registrar en el log
                            database.conversationLogDao().insert(
                                ConversationLog(
                                    timestamp = LocalDateTime.now(ZoneId.systemDefault()),
                                    role = "user",
                                    content = "⏰ Pospuse 15 minutos: ${event.title}",
                                    eventId = eventId
                                )
                            )

                            // Respuesta de Chapelotas
                            val dayPlan = database.dayPlanDao().getTodayPlan()
                            val response = if (dayPlan?.sarcasticMode == true) {
                                "Ahí vamos de nuevo... postponiendo ${event.title}. Te aviso en 15, pero no te quejes después. 🙄"
                            } else {
                                "De acuerdo, te recordaré sobre ${event.title} en 15 minutos. ⏰"
                            }

                            database.conversationLogDao().insert(
                                ConversationLog(
                                    timestamp = LocalDateTime.now(ZoneId.systemDefault()).plusSeconds(1),
                                    role = "assistant",
                                    content = response
                                )
                            )
                        }
                    }

                    "view" -> {
                        // Navegar a la pantalla del plan del día
                        eventBus.tryEmit(ChapelotasEvent.NavigateToPlan(eventId))
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error handling event action", e)
                _uiState.value = _uiState.value.copy(
                    error = "Error al procesar la acción: ${e.message}"
                )
            }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            try {
                // Obtener todos los logs antes de borrar
                val logsCount = database.conversationLogDao().getLogCount()

                // Borrar todos los logs
                database.conversationLogDao().deleteOldLogs(
                    LocalDateTime.now(ZoneId.systemDefault()).plusDays(1) // Fecha futura para borrar todo
                )

                // Agregar un mensaje de confirmación
                database.conversationLogDao().insert(
                    ConversationLog(
                        timestamp = LocalDateTime.now(ZoneId.systemDefault()),
                        role = "assistant",
                        content = "💨 Historial borrado. ¡Empecemos de nuevo! Como si no hubiera pasado nada... 😉",
                        eventId = null
                    )
                )

                Log.d("MainViewModel", "Se borraron $logsCount mensajes del historial")

            } catch (e: Exception) {
                Log.e("MainViewModel", "Error borrando historial", e)
                _uiState.value = _uiState.value.copy(
                    error = "Error al borrar el historial: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}