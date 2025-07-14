package com.chapelotas.app.data.database.daos

import androidx.room.*
import com.chapelotas.app.data.database.entities.ScheduledNotification
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: ScheduledNotification)

    @Update
    suspend fun update(notification: ScheduledNotification)

    @Query("SELECT * FROM scheduled_notifications WHERE notificationId = :id")
    suspend fun getNotification(id: Long): ScheduledNotification?

    @Query("SELECT * FROM scheduled_notifications WHERE scheduledTime <= :now AND executed = 0")
    suspend fun getPendingNotifications(now: LocalDateTime): List<ScheduledNotification>

    @Query("SELECT * FROM scheduled_notifications WHERE executed = 0 ORDER BY scheduledTime ASC")
    fun observeActiveNotifications(): Flow<List<ScheduledNotification>>

    @Query("SELECT scheduledTime FROM scheduled_notifications WHERE executed = 0 ORDER BY scheduledTime ASC LIMIT 1")
    suspend fun getNextNotificationTime(): LocalDateTime?

    @Query("UPDATE scheduled_notifications SET executed = 1, timesShown = timesShown + 1 WHERE notificationId = :id")
    suspend fun markAsExecuted(id: Long)

    @Query("UPDATE scheduled_notifications SET snoozeCount = snoozeCount + 1, scheduledTime = :newTime, executed = 0 WHERE notificationId = :id")
    suspend fun snooze(id: Long, newTime: LocalDateTime)

    @Query("DELETE FROM scheduled_notifications WHERE eventId = :eventId")
    suspend fun deleteNotificationsForEvent(eventId: String)

    // NUEVO MÉTODO AGREGADO:
    @Query("SELECT * FROM scheduled_notifications WHERE eventId = :eventId AND executed = 0 AND dismissed = 0 ORDER BY scheduledTime ASC")
    suspend fun getActiveNotificationsForEvent(eventId: String): List<ScheduledNotification>
}