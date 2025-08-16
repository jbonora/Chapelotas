package com.chapelotas.app.domain.models

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.time.LocalDateTime

object LocationContext {
    const val DEFAULT = "default"
    const val OFFICE = "office"
    const val NEARBY = "nearby"
    const val FAR = "far"
}

enum class TaskType {
    EVENT,
    TODO
}

enum class Sender {
    CHAPELOTAS,
    USUARIO
}

data class ConversationEntry(
    val sender: Sender,
    val message: String
)

data class Task(
    val id: String,
    val title: String,
    val scheduledTime: LocalDateTime,
    val endTime: LocalDateTime? = null,
    val taskType: TaskType = TaskType.EVENT,
    val lastReminderAt: LocalDateTime? = null,
    val nextReminderAt: LocalDateTime? = null,
    val reminderCount: Int = 0,
    val isAcknowledged: Boolean = false,
    val isFinished: Boolean = false,
    val calendarEventId: Long? = null,
    val isFromCalendar: Boolean = false,
    val isStarted: Boolean = false,
    val isRecurring: Boolean = false,
    val locationContext: String = LocationContext.DEFAULT,
    val travelTimeMinutes: Int = 0,
    val conversationLog: ImmutableList<ConversationEntry> = persistentListOf(),
    val unreadMessageCount: Int = 0,
    val isAllDay: Boolean = false // Campo para eventos de todo el día
) {
    val isTodo: Boolean
        get() = taskType == TaskType.TODO

    val status: TaskStatus
        get() {
            val now = LocalDateTime.now()
            val effectiveEndTime = endTime ?: scheduledTime.plusHours(1)
            return when {
                isFinished -> TaskStatus.FINISHED
                isTodo && isStarted -> TaskStatus.IN_PROGRESS
                isTodo -> TaskStatus.TODO
                now.isAfter(effectiveEndTime) -> TaskStatus.DELAYED
                now.isAfter(scheduledTime) -> TaskStatus.ONGOING
                else -> TaskStatus.UPCOMING
            }
        }
}

enum class TaskStatus {
    UPCOMING,
    ONGOING,
    DELAYED,
    FINISHED,
    TODO,
    IN_PROGRESS
}