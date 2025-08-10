package com.chapelotas.app.domain.usecases

import com.chapelotas.app.domain.debug.DebugLog
import com.chapelotas.app.domain.models.Task
import com.chapelotas.app.domain.models.TaskType
import com.chapelotas.app.domain.repositories.CalendarRepository
import com.chapelotas.app.domain.repositories.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * Caso de Uso dedicado exclusivamente a la lógica de "rollover".
 * Su única responsabilidad es mover los ToDos no finalizados de un día para el otro.
 */
class RolloverTodosUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val calendarRepository: CalendarRepository,
    private val debugLog: DebugLog
) {
    // La función ahora es 'suspend' para poder llamar a otras funciones de base de datos de forma segura.
    suspend fun execute() {
        debugLog.add("🔄 TODO-ROLLOVER: Iniciando el traspaso de To-Dos no finalizados.")

        // 1. Obtenemos solo los ToDos de AYER que no se completaron.
        val yesterday = LocalDate.now().minusDays(1)
        val unfinishedTodos = taskRepository.observeTasksForDate(yesterday)
            .first() // Usamos .first() para obtener la lista actual del Flow
            .filter { it.taskType == TaskType.TODO && !it.isFinished }

        if (unfinishedTodos.isEmpty()) {
            debugLog.add("🔄 TODO-ROLLOVER: No hay To-Dos pendientes de ayer para mover. ¡Buen trabajo!")
            return
        }

        debugLog.add("🔄 TODO-ROLLOVER: Encontrados ${unfinishedTodos.size} To-Dos para mover.")

        // 2. Iteramos sobre la lista de ToDos pendientes.
        unfinishedTodos.forEach { todo ->
            // 3. Si es una tarea del calendario (no recurrente), la movemos a hoy.
            if (todo.isFromCalendar && !todo.isRecurring && todo.calendarEventId != null) {
                val newStartTime = todo.scheduledTime.plusDays(1)
                // Para los ToDo, la hora de fin es la misma que la de inicio.
                val newEndTime = newStartTime

                val success = calendarRepository.updateEventTime(todo.calendarEventId, newStartTime, newEndTime)
                if (success) {
                    debugLog.add("🔄 TODO-ROLLOVER: Tarea '${todo.title}' movida a hoy en el calendario.")
                } else {
                    debugLog.add("🔄 TODO-ROLLOVER: ❌ Falló al mover la tarea '${todo.title}' en el calendario.")
                }

            } else if (todo.isRecurring) {
                // 4. Si es recurrente, simplemente la marcamos como finalizada para que el calendario genere la instancia de hoy.
                taskRepository.finishTask(todo.id)
                debugLog.add("🔄 TODO-ROLLOVER: Tarea recurrente '${todo.title}' marcada como finalizada para ayer.")
            } else {
                // 5. Si es una tarea manual (no del calendario), actualizamos su fecha en nuestra base de datos.
                debugLog.add("🔄 TODO-ROLLOVER: Tarea interna '${todo.title}' ignorada por ahora.")
            }
        }
    }
}