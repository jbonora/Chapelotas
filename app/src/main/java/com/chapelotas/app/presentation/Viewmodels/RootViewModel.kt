package com.chapelotas.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chapelotas.app.domain.repositories.PreferencesRepository
import com.chapelotas.app.domain.usecases.CalendarSyncUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class RootUiState(
    val isLoading: Boolean = true,
    val isFirstTimeUser: Boolean = true
)

sealed class RootViewEvent {
    data class NavigateTo(val route: String) : RootViewEvent()
    data class HighlightTask(val taskId: String) : RootViewEvent()
}

@HiltViewModel
class RootViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val calendarSyncUseCase: CalendarSyncUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RootUiState())
    val uiState: StateFlow<RootUiState> = _uiState.asStateFlow()

    private val _eventChannel = Channel<RootViewEvent>()
    val eventFlow = _eventChannel.receiveAsFlow()

    init {
        checkIfFirstTimeUser()
    }

    private fun checkIfFirstTimeUser() {
        viewModelScope.launch {
            val isFirstTime = preferencesRepository.isFirstTimeUser()
            _uiState.update { it.copy(isFirstTimeUser = isFirstTime) }

            if (!isFirstTime) {
                // La sincronización inicial se dispara desde aquí, una sola vez.
                val today = LocalDate.now()
                calendarSyncUseCase.syncDateRange(today, today.plusDays(30), triggerReminders = true)
            }
            // Marcamos la carga como finalizada para mostrar la UI.
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onNavigateTo(route: String) {
        viewModelScope.launch {
            _eventChannel.send(RootViewEvent.NavigateTo(route))
        }
    }

    fun onHighlightTask(taskId: String) {
        viewModelScope.launch {
            _eventChannel.send(RootViewEvent.HighlightTask(taskId))
        }
    }
}