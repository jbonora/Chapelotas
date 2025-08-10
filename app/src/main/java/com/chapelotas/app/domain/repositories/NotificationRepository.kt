package com.chapelotas.app.domain.repositories

import com.chapelotas.app.domain.entities.ChapelotasNotification
import java.time.LocalDateTime

interface NotificationRepository {
    suspend fun showImmediateNotification(notification: ChapelotasNotification)
    fun scheduleExactReminder(taskId: String, triggerAt: LocalDateTime)
    fun cancelReminder(taskId: String)
    // --- LÍNEA AÑADIDA ---
    fun cancelTaskNotification(taskId: String)

}