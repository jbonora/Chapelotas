package com.chapelotas.app.data.repositories

import com.chapelotas.app.data.database.TaskDao
import com.chapelotas.app.data.database.TaskEntity
import com.chapelotas.app.domain.entities.CalendarEvent
import com.chapelotas.app.domain.models.ConversationEntry
import com.chapelotas.app.domain.models.Sender
import com.chapelotas.app.domain.models.Task
import com.chapelotas.app.domain.repositories.TaskRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

    private val gson = Gson()

    override suspend fun addMessageToLog(taskId: String, message: String, sender: Sender, incrementCounter: Boolean) {
        val taskEntity = taskDao.getTask(taskId) ?: return

        val type = object : TypeToken<MutableList<ConversationEntry>>() {}.type
        val updatedLog: MutableList<ConversationEntry> = try {
            gson.fromJson(taskEntity.conversationLog, type)
        } catch (e: Exception) {
            mutableListOf()
        }

        updatedLog.add(ConversationEntry(sender = sender, message = message))
        val logJson = gson.toJson(updatedLog)

        if (incrementCounter) {
            taskDao.updateConversationLogAndIncrementUnread(taskId, logJson, System.currentTimeMillis())
        } else {
            taskDao.updateConversationLog(taskId, logJson, System.currentTimeMillis())
        }
    }

    override suspend fun resetUnreadMessageCount(taskId: String) {
        taskDao.resetUnreadMessageCount(taskId, System.currentTimeMillis())
    }

    override suspend fun syncWithCalendarEvents(events: List<CalendarEvent>): List<Task> {
        deleteOutdatedUnfinishedTasks()

        val newTasks = mutableListOf<Task>()
        if (events.isEmpty()) {
            return newTasks
        }

        val minDate = events.minOf { it.startTime }.toLocalDate()
        val maxDate = events.maxOf { it.startTime }.toLocalDate()

        val startOfDay = minDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = maxDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val dbTasksInRange = taskDao.getCalendarTasksForDateRange(startOfDay, endOfDay)

        val activeCompositeIds = events.map { "cal_${it.id}_${it.getStartMillis()}" }.toSet()

        val tasksToDelete = dbTasksInRange.filter { it.id !in activeCompositeIds }
        if (tasksToDelete.isNotEmpty()) {
            tasksToDelete.forEach { taskToDelete ->
                taskDao.deleteById(taskToDelete.id)
            }
        }

        events.forEach { event ->
            val taskId = "cal_${event.id}_${event.getStartMillis()}"
            val existingTaskEntity = dbTasksInRange.find { it.id == taskId }

            if (existingTaskEntity == null) {
                val newTaskEntity = TaskEntity.fromCalendarEvent(event)
                taskDao.insert(newTaskEntity)
                newTasks.add(newTaskEntity.toDomainModel())
            } else {
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
        return newTasks
    }

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

    override suspend fun deleteOutdatedUnfinishedTasks() {
        val todayStartMillis = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        taskDao.deleteUnfinishedTasksBefore(todayStartMillis)
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

    override fun observeTasksForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Task>> {
        val start = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return taskDao.observeTasksForDateRange(start, end).map { entities ->
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

    override suspend fun startTask(taskId: String) {
        taskDao.startTask(taskId)
    }

    override suspend fun finishTask(taskId: String) {
        taskDao.finishTask(taskId)
    }

    override suspend fun resetTaskStatus(taskId: String) {
        taskDao.resetTaskStatus(taskId)
    }

    override suspend fun updateTaskLocation(taskId: String, locationContext: String, travelTime: Int) {
        taskDao.updateTaskLocation(taskId, locationContext, travelTime)
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

    override suspend fun cleanOldCompletedTasks(daysToKeep: Int) {
        val beforeDate = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        taskDao.deleteOldCompletedTasks(beforeDate)
    }
}