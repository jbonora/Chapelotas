package com.chapelotas.app.data.database.daos

import androidx.room.*
import com.chapelotas.app.data.database.entities.NotificationLog
import com.chapelotas.app.data.database.entities.UserAction
import java.time.LocalDateTime

@Dao
interface NotificationLogDao {
    @Insert
    suspend fun insert(log: NotificationLog): Long

    @Query("UPDATE notification_logs SET userAction = :action, actionTimestamp = :timestamp, responseTimeSeconds = :responseTime WHERE logId = :logId")
    suspend fun updateUserAction(logId: Long, action: UserAction, timestamp: LocalDateTime = LocalDateTime.now(ZoneId.systemDefault()), responseTime: Long)

    @Query("SELECT * FROM notification_logs WHERE eventId = :eventId ORDER BY shownAt DESC")
    suspend fun getLogsByEvent(eventId: String): List<NotificationLog>

    // --- CORRECCIÓN ---
    @Transaction
    suspend fun logNotificationShown(
        notification: com.chapelotas.app.data.database.entities.ScheduledNotification,
        event: com.chapelotas.app.data.database.entities.EventPlan,
        message: String,
        deviceLocked: Boolean,
        appInForeground: Boolean
    ): Long {
        val log = NotificationLog(
            notificationId = notification.notificationId,
            eventId = event.eventId,
            eventTitle = event.title,
            shownAt = LocalDateTime.now(ZoneId.systemDefault()),
            minutesBeforeEvent = notification.minutesBefore,
            type = notification.type,
            priority = notification.priority,
            message = message,
            wasSnoozed = notification.snoozeCount > 0,
            snoozeCount = notification.snoozeCount,
            deviceWasLocked = deviceLocked,
            appWasInForeground = appInForeground
        )
        return insert(log)
    }
    // --- FIN DE LA CORRECCIÓN ---
}