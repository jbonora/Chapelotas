package com.chapelotas.app.domain.entities

enum class NotificationAction {
    ACKNOWLEDGE,       // "Entendido"
    FINISH_DONE        // "Done"
}

data class ChapelotasNotification(
    val id: String,
    val eventId: String, // <-- CAMBIO: De Long a String
    val message: String,
    val actions: List<NotificationAction>,
    val type: NotificationType = NotificationType.EVENT_REMINDER,
    val priority: NotificationPriority = NotificationPriority.HIGH
)

enum class NotificationPriority { LOW, NORMAL, HIGH, CRITICAL }
enum class NotificationType { EVENT_REMINDER, CRITICAL_ALERT }