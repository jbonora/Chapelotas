package com.chapelotas.app.domain.repositories

import com.chapelotas.app.domain.entities.CalendarEvent
import com.chapelotas.app.domain.models.Task
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

interface TaskRepository {

    // ========== OPERACIONES BÁSICAS ==========
    suspend fun createTask(task: Task): String
    suspend fun getTask(taskId: String): Task?
    suspend fun deleteTask(taskId: String)

    // ========== OBSERVABLES ==========
    fun observeTask(taskId: String): Flow<Task?>
    fun observeTasksForDate(date: LocalDate): Flow<List<Task>>
    fun observeAllTasks(): Flow<List<Task>> // <--- FUNCIÓN RESTAURADA

    // ========== QUERIES ESPECÍFICAS ==========
    suspend fun getTasksNeedingReminder(): List<Task>
    suspend fun getTaskByCalendarId(calendarEventId: Long): Task?

    // ========== LÓGICA DE ESTADOS (ACCIONES DEL USUARIO) ==========
    suspend fun acknowledgeTask(taskId: String)
    suspend fun finishTask(taskId: String)
    suspend fun resetTaskStatus(taskId: String)

    // ========== OPERACIONES INTERNAS DEL SISTEMA DE RECORDATORIOS ==========
    suspend fun recordReminderSent(taskId: String, nextReminderTime: LocalDateTime?)
    suspend fun updateInitialReminderState(taskId: String, reminderCount: Int, nextReminderAt: LocalDateTime?)

    // ========== SINCRONIZACIÓN Y LIMPIEZA ==========
    suspend fun syncWithCalendarEvents(events: List<CalendarEvent>)
    suspend fun cleanOldCompletedTasks(daysToKeep: Int = 7)
}