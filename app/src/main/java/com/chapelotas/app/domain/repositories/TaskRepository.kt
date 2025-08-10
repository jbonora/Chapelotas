package com.chapelotas.app.domain.repositories

import com.chapelotas.app.domain.entities.CalendarEvent
import com.chapelotas.app.domain.models.Sender
import com.chapelotas.app.domain.models.Task
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

interface TaskRepository {
    suspend fun createTask(task: Task): String
    suspend fun getTask(taskId: String): Task?
    suspend fun deleteTask(taskId: String)

    fun observeTask(taskId: String): Flow<Task?>
    fun observeTasksForDate(date: LocalDate): Flow<List<Task>>
    fun observeAllTasks(): Flow<List<Task>>
    fun observeTasksForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Task>>

    suspend fun getTasksNeedingReminder(): List<Task>
    suspend fun getTaskByCalendarId(calendarEventId: Long): Task?

    suspend fun acknowledgeTask(taskId: String)
    suspend fun finishTask(taskId: String)
    suspend fun resetTaskStatus(taskId: String)
    suspend fun startTask(taskId: String)

    suspend fun updateTaskLocation(taskId: String, locationContext: String, travelTime: Int)

    suspend fun recordReminderSent(taskId: String, nextReminderTime: LocalDateTime?)
    suspend fun updateInitialReminderState(taskId: String, reminderCount: Int, nextReminderAt: LocalDateTime?)

    suspend fun syncWithCalendarEvents(events: List<CalendarEvent>): List<Task>
    suspend fun cleanOldCompletedTasks(daysToKeep: Int = 7)

    suspend fun deleteOutdatedUnfinishedTasks()

    suspend fun addMessageToLog(taskId: String, message: String, sender: Sender, incrementCounter: Boolean = true)
    suspend fun resetUnreadMessageCount(taskId: String)
}