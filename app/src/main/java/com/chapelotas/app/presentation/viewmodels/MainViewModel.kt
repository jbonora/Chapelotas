package com.chapelotas.app.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chapelotas.app.data.database.ChapelotasDatabase
import com.chapelotas.app.data.database.entities.*
import com.chapelotas.app.domain.entities.CalendarEvent
import com.chapelotas.app.domain.events.ChapelotasEvent
import com.chapelotas.app.domain.events.ChapelotasEventBus
import com.chapelotas.app.domain.repositories.AIRepository
import com.chapelotas.app.domain.repositories.CalendarRepository
import com.chapelotas.app.domain.repositories.PreferencesRepository
import com.chapelotas.app.domain.usecases.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * MainViewModel actualizado con Room
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val database: ChapelotasDatabase,
    private val calendarRepository: CalendarRepository,
    private val aiRepository: AIRepository,
    private val preferencesRepository: PreferencesRepository,
    private val eventBus: ChapelotasEventBus,
    private val getDailySummaryUseCase: GetDailySummaryUseCase,
    private val getTomorrowSummaryUseCase: GetTomorrowSummaryUseCase,
    private val setupInitialConfigurationUseCase: SetupInitialConfigurationUseCase
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Eventos de hoy desde Room (convertidos a CalendarEvent para compatibilidad)
    val todayEvents: StateFlow<List<CalendarEvent>> = database.eventPlanDao()
        .observeTodayEvents()
        .map { eventPlans ->
            eventPlans.map { plan -> plan.toCalendarEvent() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Plan del día
    val dayPlan: StateFlow<DayPlan?> = database.dayPlanDao()
        .observeTodayPlan()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Conflictos activos
    val activeConflicts: StateFlow<List<EventConflict>> = database.conflictDao()
        .observeActiveConflicts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Resúmenes
    private val _dailySummary = MutableStateFlow("")
    val dailySummary: StateFlow<String> = _dailySummary.asStateFlow()

    private val _tomorrowSummary = MutableStateFlow("")
    val tomorrowSummary: StateFlow<String> = _tomorrowSummary.asStateFlow()

    // Control de carga automática
    private var hasLoadedTodaySummary = false

    init {
        checkInitialSetup()
        observeSystemEvents()

        // Actualizar UI state basado en eventos
        viewModelScope.launch {
            todayEvents.collect { events ->
                _uiState.update { state ->
                    state.copy(
                        hasEventsToday = events.isNotEmpty(),
                        criticalEventsCount = events.count { it.isCritical }
                    )
                }
            }
        }
    }

    private fun checkInitialSetup() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            setupInitialConfigurationUseCase().fold(
                onSuccess = { result ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isFirstTimeUser = result.isFirstTimeSetup,
                        requiresPermissions = result.requiresPermissions,
                        availableCalendars = result.availableCalendars
                    )

                    // Si tenemos permisos, cargar resumen automáticamente
                    if (!result.requiresPermissions && !hasLoadedTodaySummary) {
                        delay(500)
                        loadDailySummary()
                        hasLoadedTodaySummary = true
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }

    /**
     * Cargar resumen del día con IA
     */
    fun loadDailySummary() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoadingSummary = true)

                // Emitir evento
                eventBus.emit(ChapelotasEvent.DailySummaryRequested(isAutomatic = !hasLoadedTodaySummary))

                // Obtener eventos y preferencias
                val events = todayEvents.value
                val preferences = preferencesRepository.getUserPreferences()
                val plan = dayPlan.value

                // Generar resumen con IA
                val summary = aiRepository.generateDailySummary(
                    todayEvents = events,
                    isSarcastic = plan?.sarcasticMode ?: preferences.isSarcasticModeEnabled
                )

                _dailySummary.value = summary

                // Emitir evento de completado
                eventBus.emit(ChapelotasEvent.DailySummaryGenerated(
                    summary = summary,
                    eventsCount = events.size,
                    criticalCount = events.count { it.isCritical }
                ))

                _uiState.value = _uiState.value.copy(isLoadingSummary = false)

            } catch (e: Exception) {
                Log.e("MainViewModel", "Error generando resumen diario", e)
                _uiState.value = _uiState.value.copy(
                    isLoadingSummary = false,
                    error = "Error generando resumen: ${e.message}"
                )

                // Fallback sin IA
                _dailySummary.value = generateFallbackDailySummary()
            }
        }
    }

    /**
     * Cargar resumen de mañana con IA
     */
    fun loadTomorrowSummary() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoadingSummary = true)

                // Emitir evento
                eventBus.emit(ChapelotasEvent.TomorrowSummaryRequested(isAutomatic = false))

                // Obtener eventos de mañana
                val tomorrowEvents = database.eventPlanDao()
                    .getTomorrowEvents()
                    .map { it.toCalendarEvent() }

                val preferences = preferencesRepository.getUserPreferences()
                val plan = dayPlan.value

                // Contexto de hoy
                val todayContext = if (todayEvents.value.isNotEmpty()) {
                    "Hoy tuviste ${todayEvents.value.size} eventos"
                } else {
                    "Hoy fue un día tranquilo"
                }

                // Generar resumen con IA
                val summary = aiRepository.generateTomorrowSummary(
                    tomorrowEvents = tomorrowEvents,
                    todayContext = todayContext,
                    isSarcastic = plan?.sarcasticMode ?: preferences.isSarcasticModeEnabled
                )

                _tomorrowSummary.value = summary
                _uiState.value = _uiState.value.copy(
                    isLoadingSummary = false,
                    hasEventsTomorrow = tomorrowEvents.isNotEmpty()
                )

                // Emitir evento
                val firstEvent = tomorrowEvents.minByOrNull { it.startTime }
                eventBus.emit(ChapelotasEvent.TomorrowSummaryGenerated(
                    summary = summary,
                    eventsCount = tomorrowEvents.size,
                    firstEventTime = firstEvent?.startTime
                ))

            } catch (e: Exception) {
                Log.e("MainViewModel", "Error generando resumen de mañana", e)
                _uiState.value = _uiState.value.copy(
                    isLoadingSummary = false,
                    error = "Error generando resumen: ${e.message}"
                )

                // Fallback
                _tomorrowSummary.value = "No se pudo generar el resumen de mañana"
            }
        }
    }

    /**
     * Programar notificaciones (legacy - ahora el mono las maneja)
     */
    fun scheduleNotifications(forTomorrow: Boolean = false) {
        viewModelScope.launch {
            Log.d("MainViewModel", "Notificaciones ahora son manejadas por el mono automáticamente")
            // El UnifiedMonkeyService maneja todo
        }
    }

    /**
     * Toggle crítico de un evento
     */
    fun toggleEventCritical(eventId: Long, isCritical: Boolean) {
        viewModelScope.launch {
            try {
                // Buscar el EventPlan correspondiente
                val eventPlan = database.eventPlanDao()
                    .getTodayEvents()
                    .find { it.calendarEventId == eventId }

                if (eventPlan != null) {
                    // Actualizar en Room
                    database.eventPlanDao().updateCritical(eventPlan.eventId, isCritical)

                    // Emitir evento
                    eventBus.emit(ChapelotasEvent.EventMarkedCritical(
                        eventId = eventPlan.eventId,
                        isCritical = isCritical,
                        reason = "Usuario marcó manualmente"
                    ))

                    // Actualizar notificaciones si es crítico
                    if (isCritical) {
                        addCriticalNotifications(eventPlan)
                    }
                }

            } catch (e: Exception) {
                Log.e("MainViewModel", "Error actualizando criticidad", e)
                _uiState.value = _uiState.value.copy(
                    error = "Error actualizando evento: ${e.message}"
                )
            }
        }
    }

    /**
     * Agregar notificaciones críticas adicionales
     */
    private suspend fun addCriticalNotifications(event: EventPlan) {
        // Verificar si ya tiene notificación crítica
        val existingCritical = database.notificationDao()
            .getActiveNotificationsByEvent(event.eventId)
            .any { it.type == NotificationType.CRITICAL_ALERT }

        if (!existingCritical && event.startTime.isAfter(LocalDateTime.now().plusMinutes(5))) {
            // Agregar notificación crítica 5 minutos antes
            val criticalNotification = ScheduledNotification(
                eventId = event.eventId,
                scheduledTime = event.startTime.minusMinutes(5),
                minutesBefore = 5,
                type = NotificationType.CRITICAL_ALERT,
                priority = NotificationPriority.CRITICAL,
                message = "🚨 ALERTA CRÍTICA: ${event.title} en 5 minutos!"
            )

            database.notificationDao().insert(criticalNotification)
        }
    }

    /**
     * Limpiar error
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Cuando se otorgan permisos
     */
    fun onPermissionsGranted() {
        _uiState.value = _uiState.value.copy(requiresPermissions = false)
        checkInitialSetup()

        viewModelScope.launch {
            delay(500)
            loadDailySummary()
        }
    }

    /**
     * Observar eventos del sistema
     */
    private fun observeSystemEvents() {
        viewModelScope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is ChapelotasEvent.ConflictDetected -> {
                        // Mostrar alerta de conflicto
                        _uiState.value = _uiState.value.copy(
                            error = "⚠️ Conflicto detectado entre eventos"
                        )
                    }

                    is ChapelotasEvent.MonkeyError -> {
                        if (!event.willRetry) {
                            _uiState.value = _uiState.value.copy(
                                error = "Error en el servicio: ${event.error}"
                            )
                        }
                    }

                    else -> {
                        // Otros eventos se manejan en CalendarMonitorViewModel
                    }
                }
            }
        }
    }

    /**
     * Generar resumen fallback sin IA
     */
    private fun generateFallbackDailySummary(): String {
        val events = todayEvents.value
        val currentHour = LocalDateTime.now().hour

        return when {
            events.isEmpty() -> {
                when (currentHour) {
                    in 0..11 -> "Buenos días! Hoy tenés el día libre 🌅"
                    in 12..17 -> "Buenas tardes! No hay eventos programados para hoy"
                    else -> "Buenas noches! Hoy no tuviste eventos programados"
                }
            }

            events.size == 1 -> {
                val event = events.first()
                "Hoy tenés solo un evento: ${event.title} a las ${event.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            }

            else -> {
                val nextEvent = events.find { it.startTime.isAfter(LocalDateTime.now()) }
                if (nextEvent != null) {
                    "Tenés ${events.size} eventos hoy. El próximo es ${nextEvent.title} a las ${nextEvent.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
                } else {
                    "Tuviste ${events.size} eventos hoy. ¡Ya terminaste con todo!"
                }
            }
        }
    }
}

// Extension function para convertir EventPlan a CalendarEvent
private fun EventPlan.toCalendarEvent(): CalendarEvent {
    return CalendarEvent(
        id = this.calendarEventId,
        title = this.title,
        description = this.description,
        startTime = this.startTime,
        endTime = this.endTime,
        location = this.location,
        isAllDay = this.isAllDay,
        calendarId = this.calendarId,
        calendarName = "Calendar", // Default
        isCritical = this.isCritical,
        hasBeenSummarized = false
    )
}

// MainUiState se mantiene igual
data class MainUiState(
    val isLoading: Boolean = false,
    val isLoadingSummary: Boolean = false,
    val isFirstTimeUser: Boolean = false,
    val requiresPermissions: Boolean = false,
    val hasEventsToday: Boolean = false,
    val hasEventsTomorrow: Boolean = false,
    val criticalEventsCount: Int = 0,
    val lastScheduledCount: Int = 0,
    val availableCalendars: Map<Long, String> = emptyMap(),
    val error: String? = null
)