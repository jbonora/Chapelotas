package com.chapelotas.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chapelotas.app.domain.models.ConversationEntry
import com.chapelotas.app.domain.models.LocationContext
import com.chapelotas.app.domain.models.Task
import com.chapelotas.app.domain.models.TaskType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.collections.immutable.toImmutableList
import java.time.ZoneId

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val taskType: String = TaskType.EVENT.name,
    val description: String? = null,
    val scheduledTime: Long,
    val endTime: Long? = null,
    val lastReminderAt: Long? = null,
    val nextReminderAt: Long? = null,
    val reminderCount: Int = 0,
    val isFromCalendar: Boolean = true,
    val calendarEventId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isAcknowledged: Boolean = false,
    val isFinished: Boolean = false,
    val isStarted: Boolean = false,
    val isRecurring: Boolean = false,
    val locationContext: String = LocationContext.DEFAULT,
    val travelTimeMinutes: Int = 0,
    val conversationLog: String = "[]",
    val unreadMessageCount: Int = 0
) {
    fun toDomainModel(): Task {
        val gson = Gson()
        val type = object : TypeToken<List<ConversationEntry>>() {}.type
        val log: List<ConversationEntry> = gson.fromJson(conversationLog, type)

        return Task(
            id = id,
            title = title,
            taskType = TaskType.valueOf(taskType),
            scheduledTime = java.time.Instant.ofEpochMilli(scheduledTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime(),
            endTime = endTime?.let {
                java.time.Instant.ofEpochMilli(it)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
            },
            lastReminderAt = lastReminderAt?.let {
                java.time.Instant.ofEpochMilli(it)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
            },
            nextReminderAt = nextReminderAt?.let {
                java.time.Instant.ofEpochMilli(it)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
            },
            reminderCount = reminderCount,
            isAcknowledged = isAcknowledged,
            isFinished = isFinished,
            calendarEventId = calendarEventId,
            isFromCalendar = isFromCalendar,
            isStarted = isStarted,
            isRecurring = isRecurring,
            locationContext = locationContext,
            travelTimeMinutes = travelTimeMinutes,
            // --- CAMBIO CLAVE: Convertimos la lista a inmutable ---
            conversationLog = log.toImmutableList(),
            unreadMessageCount = unreadMessageCount
        )
    }


    companion object {
        fun fromDomainModel(task: Task): TaskEntity {
            val gson = Gson()
            val logJson = gson.toJson(task.conversationLog)

            return TaskEntity(
                id = task.id,
                title = task.title,
                taskType = task.taskType.name,
                scheduledTime = task.scheduledTime
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli(),
                endTime = task.endTime?.atZone(ZoneId.systemDefault())
                    ?.toInstant()
                    ?.toEpochMilli(),
                lastReminderAt = task.lastReminderAt?.atZone(ZoneId.systemDefault())
                    ?.toInstant()
                    ?.toEpochMilli(),
                nextReminderAt = task.nextReminderAt?.atZone(ZoneId.systemDefault())
                    ?.toInstant()
                    ?.toEpochMilli(),
                reminderCount = task.reminderCount,
                isAcknowledged = task.isAcknowledged,
                isFinished = task.isFinished,
                calendarEventId = task.calendarEventId,
                isFromCalendar = task.isFromCalendar,
                isStarted = task.isStarted,
                isRecurring = task.isRecurring,
                locationContext = task.locationContext,
                travelTimeMinutes = task.travelTimeMinutes,
                conversationLog = logJson,
                unreadMessageCount = task.unreadMessageCount
            )
        }


        fun fromCalendarEvent(event: com.chapelotas.app.domain.entities.CalendarEvent): TaskEntity = TaskEntity(
            id = "cal_${event.id}_${event.getStartMillis()}",
            title = event.title,
            description = event.description,
            taskType = event.taskType.name,
            scheduledTime = event.getStartMillis(),
            endTime = event.getEndMillis(),
            isFromCalendar = true,
            calendarEventId = event.id,
            isRecurring = event.isRecurring,
            locationContext = LocationContext.DEFAULT,
            travelTimeMinutes = 0,
            unreadMessageCount = 0
        )
    }
}