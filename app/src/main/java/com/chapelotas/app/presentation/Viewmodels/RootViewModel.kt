package com.chapelotas.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chapelotas.app.domain.repositories.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Estado que representa la pantalla de carga inicial
data class RootUiState(
    val isLoading: Boolean = true,
    val isFirstTimeUser: Boolean = true
)

@HiltViewModel
class RootViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RootUiState())
    val uiState: StateFlow<RootUiState> = _uiState.asStateFlow()

    init {
        // Al iniciar, comprueba si es un nuevo usuario
        checkIfFirstTimeUser()
    }

    private fun checkIfFirstTimeUser() {
        viewModelScope.launch {
            val isFirstTime = preferencesRepository.isFirstTimeUser()
            // Actualiza el estado para que la UI reaccione
            _uiState.value = RootUiState(isLoading = false, isFirstTimeUser = isFirstTime)
        }
    }
}