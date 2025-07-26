package com.chapelotas.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chapelotas.app.domain.models.Task
import java.time.LocalDateTime
import java.time.ZoneId

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
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

    // --- CAMPOS SIMPLIFICADOS ---
    val isAcknowledged: Boolean = false,
    val isFinished: Boolean = false
) {
    fun toDomainModel(): Task = Task(
        id = id,
        title = title,
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
        isFinished = isFinished
    )

    companion object {
        fun fromDomainModel(task: Task): TaskEntity = TaskEntity(
            id = task.id,
            title = task.title,
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
            isFinished = task.isFinished
        )

        fun fromCalendarEvent(event: com.chapelotas.app.domain.entities.CalendarEvent): TaskEntity = TaskEntity(
            id = "cal_${event.id}",
            title = event.title,
            description = event.description,
            scheduledTime = event.getStartMillis(),
            endTime = event.getEndMillis(),
            isFromCalendar = true,
            calendarEventId = event.id
        )
    }
}