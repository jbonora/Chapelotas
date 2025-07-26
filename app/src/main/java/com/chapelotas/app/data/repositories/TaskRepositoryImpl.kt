package com.chapelotas.app.data.repositories

import com.chapelotas.app.data.database.TaskDao
import com.chapelotas.app.data.database.TaskEntity
import com.chapelotas.app.domain.entities.CalendarEvent
import com.chapelotas.app.domain.models.Task
import com.chapelotas.app.domain.repositories.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {

    override suspend fun createTask(task: Task): String {
        val entity = TaskEntity.fromDomainModel(task)
        taskDao.insert(entity)
        return entity.id
    }

    override suspend fun getTask(taskId: String): Task? {
        return taskDao.getTask(taskId)?.toDomainModel()
    }

    override suspend fun deleteTask(taskId: String) {
        taskDao.deleteById(taskId)
    }

    override fun observeTask(taskId: String): Flow<Task?> {
        return taskDao.observeTask(taskId).map { it?.toDomainModel() }
    }

    override fun observeTasksForDate(date: LocalDate): Flow<List<Task>> {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return taskDao.observeTasksForDate(startOfDay, endOfDay).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun observeAllTasks(): Flow<List<Task>> {
        return taskDao.observeAllTasks().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getTasksNeedingReminder(): List<Task> {
        val now = System.currentTimeMillis()
        return taskDao.getTasksNeedingReminder(now).map { it.toDomainModel() }
    }

    override suspend fun getTaskByCalendarId(calendarEventId: Long): Task? {
        return taskDao.getTaskByCalendarId(calendarEventId)?.toDomainModel()
    }

    override suspend fun acknowledgeTask(taskId: String) {
        taskDao.acknowledgeTask(taskId)
    }

    override suspend fun finishTask(taskId: String) {
        taskDao.finishTask(taskId)
    }

    override suspend fun resetTaskStatus(taskId: String) {
        taskDao.resetTaskStatus(taskId)
    }

    override suspend fun recordReminderSent(taskId: String, nextReminderTime: LocalDateTime?) {
        val now = System.currentTimeMillis()
        val nextReminder = nextReminderTime?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        taskDao.updateReminderSent(taskId, now, nextReminder)
    }

    override suspend fun updateInitialReminderState(taskId: String, reminderCount: Int, nextReminderAt: LocalDateTime?) {
        val nextReminderMillis = nextReminderAt?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        taskDao.updateInitialReminderState(taskId, reminderCount, nextReminderMillis)
    }

    override suspend fun syncWithCalendarEvents(events: List<CalendarEvent>) {
        val activeCalendarIds = events.map { it.id }
        taskDao.deleteRemovedCalendarEvents(activeCalendarIds)

        events.forEach { event ->
            val taskId = "cal_${event.id}"
            val existingTaskEntity = taskDao.getTask(taskId)
            if (existingTaskEntity == null) {
                val newTask = TaskEntity.fromCalendarEvent(event)
                taskDao.insert(newTask)
            } else {
                // Solo actualiza si los datos del calendario son diferentes
                val updatedTask = existingTaskEntity.copy(
                    title = event.title,
                    description = event.description,
                    scheduledTime = event.getStartMillis(),
                    endTime = event.getEndMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                if (updatedTask != existingTaskEntity) {
                    taskDao.update(updatedTask)
                }
            }
        }
    }

    override suspend fun cleanOldCompletedTasks(daysToKeep: Int) {
        val beforeDate = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        taskDao.deleteOldCompletedTasks(beforeDate)
    }
}