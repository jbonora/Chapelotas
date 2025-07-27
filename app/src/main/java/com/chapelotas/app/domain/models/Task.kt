package com.chapelotas.app.domain.models

import java.time.LocalDateTime

data class Task(
    val id: String,
    val title: String,
    val scheduledTime: LocalDateTime,
    val endTime: LocalDateTime? = null,
    val lastReminderAt: LocalDateTime? = null,
    val nextReminderAt: LocalDateTime? = null,
    val reminderCount: Int = 0,
    val isAcknowledged: Boolean = false,
    val isFinished: Boolean = false
) {
    val status: TaskStatus
        get() {
            val now = LocalDateTime.now()
            val effectiveEndTime = endTime ?: scheduledTime.plusHours(1)
            return when {
                isFinished -> TaskStatus.FINISHED
                // Si el tiempo ya pasó y no está terminada, SIEMPRE es DELAYED.
                now.isAfter(effectiveEndTime) -> TaskStatus.DELAYED
                now.isAfter(scheduledTime) -> TaskStatus.ONGOING
                else -> TaskStatus.UPCOMING
            }
        }
}

/**
 * Representa los posibles estados de una tarea.
 * No existe el estado "Omitido" (MISSED), ya que Chapelotas siempre insiste.
 */
enum class TaskStatus {
    UPCOMING,
    ONGOING,
    DELAYED,
    FINISHED
}