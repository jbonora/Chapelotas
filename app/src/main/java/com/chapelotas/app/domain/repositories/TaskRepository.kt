package com.chapelotas.app.domain.repositories

import com.chapelotas.app.domain.entities.CalendarEvent
import com.chapelotas.app.domain.models.Task
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Define el contrato para gestionar las tareas.
 * Actúa como una capa de abstracción entre los casos de uso (la lógica) y la fuente de datos (la base de datos).
 */
interface TaskRepository {

    // --- OPERACIONES BÁSICAS (CRUD) ---
    suspend fun createTask(task: Task): String
    suspend fun getTask(taskId: String): Task?
    suspend fun deleteTask(taskId: String)

    // --- OBSERVABLES (Para que la UI reaccione a los cambios) ---
    fun observeTask(taskId: String): Flow<Task?>
    fun observeTasksForDate(date: LocalDate): Flow<List<Task>>
    fun observeAllTasks(): Flow<List<Task>>

    // --- QUERIES ESPECÍFICAS PARA LA LÓGICA DE NEGOCIO ---
    suspend fun getTasksNeedingReminder(): List<Task>
    suspend fun getTaskByCalendarId(calendarEventId: Long): Task?

    // --- ACCIONES DEL USUARIO Y DEL SISTEMA ---
    suspend fun acknowledgeTask(taskId: String)
    suspend fun finishTask(taskId: String)
    suspend fun resetTaskStatus(taskId: String)
    suspend fun recordReminderSent(taskId: String, nextReminderTime: LocalDateTime?)
    suspend fun updateInitialReminderState(taskId: String, reminderCount: Int, nextReminderAt: LocalDateTime?)

    // --- SINCRONIZACIÓN Y MANTENIMIENTO ---
    suspend fun syncWithCalendarEvents(events: List<CalendarEvent>)
    suspend fun cleanOldCompletedTasks(daysToKeep: Int = 7)
}