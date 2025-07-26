package com.chapelotas.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chapelotas.app.domain.models.AppSettings
import com.chapelotas.app.domain.repositories.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    // --- INICIO DE LA CORRECCIÓN ---
    // 1. El ViewModel ahora "observa" los cambios desde el repositorio.
    val settings: StateFlow<AppSettings> = preferencesRepository.observeAppSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    // 2. Cuando la UI reporta un cambio, simplemente le pedimos al repositorio que lo guarde.
    fun onSettingsChanged(newSettings: AppSettings) {
        viewModelScope.launch {
            preferencesRepository.saveAppSettings(newSettings)
        }
    }
    // --- FIN DE LA CORRECCIÓN ---
}