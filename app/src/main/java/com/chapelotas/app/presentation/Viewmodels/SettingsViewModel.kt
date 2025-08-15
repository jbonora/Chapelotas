package com.chapelotas.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chapelotas.app.domain.events.EventBus
import com.chapelotas.app.domain.events.SystemEvent
import com.chapelotas.app.domain.models.AppSettings
import com.chapelotas.app.domain.personality.PersonalityProvider
import com.chapelotas.app.domain.repositories.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val availablePersonalities: Map<String, String> = emptyMap()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val eventBus: EventBus,
    private val personalityProvider: PersonalityProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        val personalities = personalityProvider.getAvailablePersonalities()
        viewModelScope.launch {
            preferencesRepository.observeAppSettings().collect { settings ->
                _uiState.value = SettingsUiState(
                    settings = settings,
                    availablePersonalities = personalities
                )
            }
        }
    }

    // --- ✅ CAMBIO PRINCIPAL AQUÍ ---
    fun onSettingsChanged(newSettings: AppSettings) {
        viewModelScope.launch {
            val oldSettings = uiState.value.settings

            // Guardar la nueva configuración
            preferencesRepository.saveAppSettings(newSettings)

            // Comprobar qué ha cambiado y emitir el evento adecuado
            if (didAlarmSettingsChange(oldSettings, newSettings)) {
                eventBus.emit(SystemEvent.AlarmSettingsChanged)
            }

            if (oldSettings.personalityProfile != newSettings.personalityProfile) {
                eventBus.emit(SystemEvent.PersonalitySettingsChanged)
            }
        }
    }

    /**
     * Compara los campos de configuración que afectan directamente a la alarma.
     */
    private fun didAlarmSettingsChange(old: AppSettings, new: AppSettings): Boolean {
        return old.autoCreateAlarm != new.autoCreateAlarm ||
                old.alarmOffsetMinutes != new.alarmOffsetMinutes ||
                old.workStartTime != new.workStartTime
    }
    // --- FIN DEL CAMBIO ---
}