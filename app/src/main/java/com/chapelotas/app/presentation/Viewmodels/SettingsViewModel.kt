package com.chapelotas.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chapelotas.app.domain.events.EventBus
import com.chapelotas.app.domain.events.SystemEvent
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
    private val preferencesRepository: PreferencesRepository,
    // --- INICIO DE LA MODIFICACIÓN ---
    private val eventBus: EventBus
    // --- FIN DE LA MODIFICACIÓN ---
) : ViewModel() {

    val settings: StateFlow<AppSettings> = preferencesRepository.observeAppSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    fun onSettingsChanged(newSettings: AppSettings) {
        viewModelScope.launch {
            preferencesRepository.saveAppSettings(newSettings)
            // --- INICIO DE LA MODIFICACIÓN ---
            // Ahora solo emitimos un evento. No sabemos (ni nos importa) quién lo escuchará.
            eventBus.emit(SystemEvent.SettingsChanged)
            // --- FIN DE LA MODIFICACIÓN ---
        }
    }
}