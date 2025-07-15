package com.chapelotas.app.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

/**
 * Notificaciones programadas - el mono checkea esta tabla
 */
@Entity(
    tableName = "scheduled_notifications",
    foreignKeys = [
        ForeignKey(
            entity = EventPlan::class,
            parentColumns = ["eventId"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("eventId"),
        Index("scheduledTime"),
        Index("executed"),
        Index("snoozedUntil")
    ]
)
data class ScheduledNotification(
    @PrimaryKey(autoGenerate = true)
    val notificationId: Long = 0,
    val eventId: String,
    val scheduledTime: LocalDateTime,
    val minutesBefore: Int,

    val type: NotificationType = NotificationType.EVENT_REMINDER,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val executed: Boolean = false,
    val executedAt: LocalDateTime? = null,
    val snoozedUntil: LocalDateTime? = null,
    val snoozeCount: Int = 0,
    val dismissed: Boolean = false,
    val message: String? = null,
    val aiReason: String? = null,
    val workManagerId: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneId.systemDefault()),

    // --- CAMBIO CLAVE: El nuevo campo para contar la insistencia ---
    val timesShown: Int = 0
) {
    fun shouldShowNow(): Boolean {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val targetTime = snoozedUntil ?: scheduledTime
        return !executed && !dismissed && now.isAfter(targetTime)
    }

    fun isActive(): Boolean {
        return !executed && !dismissed
    }

    fun snooze(minutes: Int): ScheduledNotification {
        return copy(
            snoozedUntil = LocalDateTime.now(ZoneId.systemDefault()).plusMinutes(minutes.toLong()),
            snoozeCount = snoozeCount + 1
        )
    }

    fun getAndroidNotificationId(): Int {
        return notificationId.toInt() and Int.MAX_VALUE
    }
}