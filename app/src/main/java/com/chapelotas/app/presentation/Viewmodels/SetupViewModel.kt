package com.chapelotas.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chapelotas.app.domain.repositories.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SetupStep {
    Welcome,
    CalendarPermission,
    NotificationPermission,
    BatteryOptimization,
    NameInput,
    FinalSettings,
    CustomizingSettings // Este paso sigue siendo útil para mostrar la pantalla de settings
}

data class SetupState(
    val currentStep: SetupStep = SetupStep.Welcome,
    val userName: String = "",
    val isFinished: Boolean = false // Nuevo estado para controlar la navegación
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupState())
    val uiState = _uiState.asStateFlow()

    fun nextStep() {
        if (_uiState.value.currentStep == SetupStep.FinalSettings) return

        val nextStepOrdinal = _uiState.value.currentStep.ordinal + 1
        val nextStep = SetupStep.values().getOrNull(nextStepOrdinal) ?: SetupStep.FinalSettings
        _uiState.update { it.copy(currentStep = nextStep) }
    }

    fun startCustomization() {
        _uiState.update { it.copy(currentStep = SetupStep.CustomizingSettings) }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(userName = name) }
    }

    fun finishSetup() {
        viewModelScope.launch {
            val currentSettings = preferencesRepository.observeAppSettings().first()
            val finalSettings = currentSettings.copy(
                userName = _uiState.value.userName.ifBlank { "Usuario" }
            )
            preferencesRepository.saveAppSettings(finalSettings)
            preferencesRepository.setAlarmsConfigured(true)
            preferencesRepository.setLastSuccessfulRun(System.currentTimeMillis())

            // En lugar de cambiar el paso, activamos la bandera de finalización
            _uiState.update { it.copy(isFinished = true) }
        }
    }
}