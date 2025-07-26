package com.chapelotas.app.domain.repositories

import com.chapelotas.app.domain.entities.ChapelotasNotification

interface NotificationRepository {
    suspend fun showImmediateNotification(notification: ChapelotasNotification)
    suspend fun scheduleNotification(notification: ChapelotasNotification)
    suspend fun cancelNotification(notificationId: String)
    suspend fun isNotificationServiceRunning(): Boolean
    suspend fun startNotificationService()
}