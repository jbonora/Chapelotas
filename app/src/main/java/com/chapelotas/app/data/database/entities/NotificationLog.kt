package com.chapelotas.app.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "notification_logs",
    indices = [Index("eventId"), Index("shownAt"), Index("userAction")]
)
data class NotificationLog(
    @PrimaryKey(autoGenerate = true) val logId: Long = 0,
    val notificationId: Long,
    val eventId: String,
    val eventTitle: String,
    val shownAt: LocalDateTime,
    val minutesBeforeEvent: Int,
    val type: NotificationType,
    val priority: NotificationPriority,
    val message: String,
    val wasAiGenerated: Boolean = true,
    val userAction: UserAction? = null,
    val actionTimestamp: LocalDateTime? = null,
    val responseTimeSeconds: Long? = null,
    val wasSnoozed: Boolean = false,
    val snoozeCount: Int = 0,
    val deviceWasLocked: Boolean = false,
    val appWasInForeground: Boolean = false,
    val eventWasAttended: Boolean? = null,
    val wasEffective: Boolean? = null
) {
    fun calculateEffectiveness(): Boolean {
        return when (userAction) {
            UserAction.OPENED, UserAction.MARKED_DONE -> true
            UserAction.DISMISSED, UserAction.IGNORED -> false
            UserAction.SNOOZED -> wasSnoozed && snoozeCount <= 2
            null -> false
        }
    }
}