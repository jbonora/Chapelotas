package com.chapelotas.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chapelotas.app.domain.models.Task
import com.chapelotas.app.domain.repositories.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel para la pantalla de Debug.
 * Expone la "fuente de verdad" sin filtros.
 */
@HiltViewModel
class DebugViewModel @Inject constructor(
    taskRepository: TaskRepository
) : ViewModel() {

    /**
     * Un Flow que observa TODAS las tareas en la base de datos, ordenadas por fecha.
     * Se actualiza automáticamente si algo cambia en la BD.
     */
    val allTasks: StateFlow<List<Task>> = taskRepository.observeAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}