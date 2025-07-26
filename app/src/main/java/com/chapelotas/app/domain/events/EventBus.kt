package com.chapelotas.app.domain.events

import com.chapelotas.app.domain.models.Task
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class AppEvent {
    // Se dispara cuando se detecta una tarea que antes no existía
    data class TaskCreated(val task: Task) : AppEvent()

    // --- INICIO DE LA CORRECCIÓN ---
    // El evento TaskStatusChanged se elimina, ya que el concepto de "status" único ya no existe.
    // La UI ahora reacciona a los cambios en las banderas booleanas del objeto Task.
    // --- FIN DE LA CORRECCIÓN ---

    // Se dispara después de cualquier sincronización para refrescar la UI
    object SyncCompleted : AppEvent()
}

@Singleton
class EventBus @Inject constructor() {
    private val _events = MutableSharedFlow<AppEvent>()
    val events = _events.asSharedFlow()

    fun emit(event: AppEvent) {
        _events.tryEmit(event)
    }
}