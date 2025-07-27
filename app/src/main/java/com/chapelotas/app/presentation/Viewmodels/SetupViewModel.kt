package com.chapelotas.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chapelotas.app.domain.models.AppSettings
import com.chapelotas.app.domain.repositories.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first // <-- ¡EL IMPORT QUE FALTABA!
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Representa el paso actual en el flujo de configuración.
 */
enum class SetupStep {
    Welcome,
    CalendarPermission,
    NotificationPermission,
    BatteryOptimization,
    NameInput,
    FinalSettings,
    CustomizingSettings,
    Finished
}

/**
 * Contiene todos los datos relevantes para el estado de la UI durante la configuración.
 */
data class SetupState(
    val currentStep: SetupStep = SetupStep.Welcome,
    val userName: String = ""
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupState())
    val uiState = _uiState.asStateFlow()

    /**
     * Avanza al siguiente paso en el flujo de configuración.
     */
    fun nextStep() {
        val nextStepOrdinal = _uiState.value.currentStep.ordinal + 1
        val nextStep = SetupStep.values().getOrNull(nextStepOrdinal) ?: SetupStep.Finished
        _uiState.update { it.copy(currentStep = nextStep) }
    }

    /**
     * Pone el flujo en el modo de personalización.
     */
    fun startCustomization() {
        _uiState.update { it.copy(currentStep = SetupStep.CustomizingSettings) }
    }

    /**
     * Actualiza el nombre del usuario en el estado.
     */
    fun onNameChange(name: String) {
        _uiState.update { it.copy(userName = name) }
    }

    /**
     * Guarda la configuración final y marca el proceso de setup como completado.
     */
    fun finishSetup() {
        viewModelScope.launch {
            // Se asegura de que el nombre de usuario se guarde, incluso si está vacío.
            val currentSettings = preferencesRepository.observeAppSettings().first()
            val finalSettings = currentSettings.copy(
                userName = _uiState.value.userName.ifBlank { "Usuario" }
            )
            preferencesRepository.saveAppSettings(finalSettings)

            // Marca que el setup de optimización de batería se ha mostrado.
            preferencesRepository.setAlarmsConfigured(true)

            // Marca que el flujo de primera vez ha sido completado.
            preferencesRepository.setLastSuccessfulRun(System.currentTimeMillis())

            _uiState.update { it.copy(currentStep = SetupStep.Finished) }
        }
    }
}