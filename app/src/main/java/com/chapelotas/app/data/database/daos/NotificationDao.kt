package com.chapelotas.app.data.database.daos

import androidx.room.*
import com.chapelotas.app.data.database.entities.ScheduledNotification
import com.chapelotas.app.data.database.entities.NotificationType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * DAO para operaciones con notificaciones programadas
 * El mono principalmente usa este DAO para sus checks
 */
@Dao
interface NotificationDao {

    // ===== INSERT =====
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: ScheduledNotification): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<ScheduledNotification>): List<Long>

    // ===== UPDATE =====
    @Update
    suspend fun update(notification: ScheduledNotification)

    @Query("""
        UPDATE scheduled_notifications 
        SET executed = 1, executedAt = :executedAt 
        WHERE notificationId = :notificationId
    """)
    suspend fun markAsExecuted(notificationId: Long, executedAt: LocalDateTime = LocalDateTime.now())

    @Query("""
        UPDATE scheduled_notifications 
        SET snoozedUntil = :snoozeUntil, snoozeCount = snoozeCount + 1 
        WHERE notificationId = :notificationId
    """)
    suspend fun updateSnoozedTime(notificationId: Long, snoozeUntil: LocalDateTime)

    @Query("""
        UPDATE scheduled_notifications 
        SET dismissed = 1 
        WHERE notificationId = :notificationId
    """)
    suspend fun markAsDismissed(notificationId: Long)

    @Query("""
        UPDATE scheduled_notifications 
        SET message = :message 
        WHERE notificationId = :notificationId
    """)
    suspend fun updateMessage(notificationId: Long, message: String)

    // ===== DELETE =====
    @Delete
    suspend fun delete(notification: ScheduledNotification)

    @Query("DELETE FROM scheduled_notifications WHERE eventId = :eventId")
    suspend fun deleteByEventId(eventId: String)

    @Query("DELETE FROM scheduled_notifications WHERE executed = 1 AND executedAt < :beforeDate")
    suspend fun deleteOldExecuted(beforeDate: LocalDateTime)

    // ===== QUERIES - Single =====
    @Query("SELECT * FROM scheduled_notifications WHERE notificationId = :id LIMIT 1")
    suspend fun getNotification(id: Long): ScheduledNotification?

    @Query("SELECT * FROM scheduled_notifications WHERE notificationId = :id LIMIT 1")
    fun observeNotification(id: Long): Flow<ScheduledNotification?>

    // ===== QUERIES - Pending (Para el Mono!) =====
    @Query("""
        SELECT * FROM scheduled_notifications 
        WHERE executed = 0 
        AND dismissed = 0
        AND (
            (snoozedUntil IS NULL AND scheduledTime <= :now) OR
            (snoozedUntil IS NOT NULL AND snoozedUntil <= :now)
        )
        ORDER BY 
            CASE 
                WHEN snoozedUntil IS NOT NULL THEN snoozedUntil 
                ELSE scheduledTime 
            END ASC
    """)
    suspend fun getPendingNotifications(now: LocalDateTime = LocalDateTime.now()): List<ScheduledNotification>

    @Query("""
        SELECT * FROM scheduled_notifications 
        WHERE executed = 0 
        AND dismissed = 0
        AND (
            (snoozedUntil IS NULL AND scheduledTime <= :now) OR
            (snoozedUntil IS NOT NULL AND snoozedUntil <= :now)
        )
        ORDER BY scheduledTime ASC
    """)
    fun observePendingNotifications(now: LocalDateTime = LocalDateTime.now()): Flow<List<ScheduledNotification>>

    // ===== QUERIES - By Event =====
    @Query("""
        SELECT * FROM scheduled_notifications 
        WHERE eventId = :eventId 
        ORDER BY scheduledTime ASC
    """)
    suspend fun getNotificationsByEvent(eventId: String): List<ScheduledNotification>

    @Query("""
        SELECT * FROM scheduled_notifications 
        WHERE eventId = :eventId 
        AND executed = 0 
        AND dismissed = 0
        ORDER BY scheduledTime ASC
    """)
    suspend fun getActiveNotificationsByEvent(eventId: String): List<ScheduledNotification>

    // ===== QUERIES - Next Check Time (Para el Mono!) =====
    @Query("""
        SELECT MIN(
            CASE 
                WHEN snoozedUntil IS NOT NULL THEN snoozedUntil 
                ELSE scheduledTime 
            END
        ) as nextTime
        FROM scheduled_notifications 
        WHERE executed = 0 
        AND dismissed = 0
        AND (
            (snoozedUntil IS NULL AND scheduledTime > :after) OR
            (snoozedUntil IS NOT NULL AND snoozedUntil > :after)
        )
    """)
    suspend fun getNextNotificationTime(after: LocalDateTime = LocalDateTime.now()): LocalDateTime?

    // ===== QUERIES - Analytics =====
    @Query("""
        SELECT COUNT(*) FROM scheduled_notifications 
        WHERE eventId = :eventId AND executed = 1
    """)
    suspend fun getExecutedCountForEvent(eventId: String): Int

    @Query("""
        SELECT COUNT(*) FROM scheduled_notifications 
        WHERE type = :type 
        AND scheduledTime >= :startDate 
        AND scheduledTime <= :endDate
    """)
    suspend fun getCountByTypeInRange(
        type: NotificationType,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Int

    @Query("""
        SELECT AVG(snoozeCount) FROM scheduled_notifications 
        WHERE executed = 1 AND snoozeCount > 0
    """)
    suspend fun getAverageSnoozeCount(): Float?

    // ===== UTILITY =====
    @Transaction
    suspend fun snoozeNotification(notificationId: Long, minutes: Int = 5): Boolean {
        val notification = getNotification(notificationId) ?: return false

        if (!notification.executed && !notification.dismissed) {
            val snoozeUntil = LocalDateTime.now().plusMinutes(minutes.toLong())
            updateSnoozedTime(notificationId, snoozeUntil)
            return true
        }
        return false
    }
}