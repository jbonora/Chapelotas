package com.chapelotas.app.domain.entities

import com.chapelotas.app.di.Constants

enum class NotificationAction {
    ACKNOWLEDGE,
    FINISH_DONE
}

data class ChapelotasNotification(
    val id: String,
    val eventId: String,
    val message: String,
    val actions: List<NotificationAction>,
    val channelId: String = Constants.CHANNEL_ID_GENERAL,
    val type: NotificationType = NotificationType.EVENT_REMINDER,
    val priority: NotificationPriority = NotificationPriority.HIGH
)

enum class NotificationPriority { LOW, NORMAL, HIGH, CRITICAL }
enum class NotificationType { EVENT_REMINDER, CRITICAL_ALERT }