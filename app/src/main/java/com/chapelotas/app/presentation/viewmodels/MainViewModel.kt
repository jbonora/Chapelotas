package com.chapelotas.app.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chapelotas.app.domain.entities.CalendarEvent
import com.chapelotas.app.domain.usecases.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getDailySummaryUseCase: GetDailySummaryUseCase,
    private val getTomorrowSummaryUseCase: GetTomorrowSummaryUseCase,
    private val scheduleNotificationsUseCase: ScheduleNotificationsUseCase,
    private val markEventAsCriticalUseCase: MarkEventAsCriticalUseCase,
    private val setupInitialConfigurationUseCase: SetupInitialConfigurationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _todayEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val todayEvents: StateFlow<List<CalendarEvent>> = _todayEvents.asStateFlow()

    private val _dailySummary = MutableStateFlow("")
    val dailySummary: StateFlow<String> = _dailySummary.asStateFlow()

    private val _tomorrowSummary = MutableStateFlow("")
    val tomorrowSummary: StateFlow<String> = _tomorrowSummary.asStateFlow()

    // Variable para controlar si ya se cargó el resumen automático
    private var hasLoadedTodaySummary = false

    init {
        checkInitialSetup()
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

                    // Si ya tenemos permisos y no hemos cargado el resumen, cargarlo automáticamente
                    if (!result.requiresPermissions && !hasLoadedTodaySummary) {
                        delay(500) // Pequeña espera para que todo se inicialice
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

    fun loadDailySummary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSummary = true)

            // Si es carga manual (botón), permitir recargar
            if (_dailySummary.value.isNotEmpty()) {
                hasLoadedTodaySummary = true // Ya no es la primera vez
            }

            getDailySummaryUseCase().fold(
                onSuccess = { result ->
                    _dailySummary.value = result.summary
                    _todayEvents.value = result.events
                    _uiState.value = _uiState.value.copy(
                        isLoadingSummary = false,
                        hasEventsToday = result.hasEvents,
                        criticalEventsCount = result.criticalEventsCount
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingSummary = false,
                        error = error.message
                    )
                }
            )
        }
    }

    fun loadTomorrowSummary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSummary = true)

            getTomorrowSummaryUseCase().fold(
                onSuccess = { result ->
                    _tomorrowSummary.value = result.summary
                    _uiState.value = _uiState.value.copy(
                        isLoadingSummary = false,
                        hasEventsTomorrow = result.hasEvents
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingSummary = false,
                        error = error.message
                    )
                }
            )
        }
    }

    fun scheduleNotifications(forTomorrow: Boolean = false) {
        viewModelScope.launch {
            Log.d("Chapelotas", "Programando notificaciones...")
            scheduleNotificationsUseCase(forTomorrow).fold(
                onSuccess = { result ->
                    Log.d("Chapelotas", "✓ Notificaciones programadas: ${result.notificationsScheduled}")
                    _uiState.value = _uiState.value.copy(
                        lastScheduledCount = result.notificationsScheduled
                    )
                },
                onFailure = { error ->
                    Log.e("Chapelotas", "✗ Error programando: ${error.message}")
                    _uiState.value = _uiState.value.copy(
                        error = error.message
                    )
                }
            )
        }
    }

    fun toggleEventCritical(eventId: Long, isCritical: Boolean) {
        viewModelScope.launch {
            markEventAsCriticalUseCase(eventId, isCritical).fold(
                onSuccess = {
                    loadDailySummary()
                    // Auto-programar notificaciones cuando se marca como crítico
                    scheduleNotifications(false)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun onPermissionsGranted() {
        _uiState.value = _uiState.value.copy(requiresPermissions = false)
        checkInitialSetup()
        // También cargar el resumen cuando se otorgan permisos
        viewModelScope.launch {
            delay(500)
            loadDailySummary()
        }
    }
}

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