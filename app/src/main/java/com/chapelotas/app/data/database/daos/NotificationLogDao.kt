package com.chapelotas.app.data.database.daos

import androidx.room.*
import com.chapelotas.app.data.database.entities.NotificationLog
import com.chapelotas.app.data.database.entities.UserAction
import com.chapelotas.app.data.database.entities.NotificationType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * DAO para el log de notificaciones - Analytics y mejora continua
 */
@Dao
interface NotificationLogDao {

    // ===== INSERT =====
    @Insert
    suspend fun insert(log: NotificationLog): Long

    @Insert
    suspend fun insertAll(logs: List<NotificationLog>)

    // ===== UPDATE =====
    @Update
    suspend fun update(log: NotificationLog)

    @Query("""
        UPDATE notification_logs 
        SET userAction = :action, 
            actionTimestamp = :timestamp,
            responseTimeSeconds = :responseTime
        WHERE logId = :logId
    """)
    suspend fun updateUserAction(
        logId: Long,
        action: UserAction,
        timestamp: LocalDateTime = LocalDateTime.now(),
        responseTime: Long
    )

    @Query("""
        UPDATE notification_logs 
        SET eventWasAttended = :attended, wasEffective = :effective 
        WHERE eventId = :eventId
    """)
    suspend fun updateEventOutcome(eventId: String, attended: Boolean, effective: Boolean)

    // ===== DELETE =====
    @Query("DELETE FROM notification_logs WHERE shownAt < :beforeDate")
    suspend fun deleteOldLogs(beforeDate: LocalDateTime)

    // ===== QUERIES - Analytics =====

    // Efectividad general
    @Query("""
        SELECT 
            COUNT(CASE WHEN wasEffective = 1 THEN 1 END) * 100.0 / COUNT(*) as effectiveness
        FROM notification_logs 
        WHERE shownAt >= :since
    """)
    suspend fun getEffectivenessPercentage(since: LocalDateTime): Float?

    // Efectividad por tipo
    @Query("""
        SELECT 
            type,
            COUNT(*) as total,
            COUNT(CASE WHEN wasEffective = 1 THEN 1 END) as effective,
            AVG(responseTimeSeconds) as avgResponseTime
        FROM notification_logs 
        WHERE shownAt >= :since
        GROUP BY type
    """)
    suspend fun getEffectivenessByType(since: LocalDateTime): List<TypeEffectiveness>

    // Tiempo de respuesta promedio
    @Query("""
        SELECT AVG(responseTimeSeconds) 
        FROM notification_logs 
        WHERE userAction IS NOT NULL 
        AND responseTimeSeconds IS NOT NULL
        AND shownAt >= :since
    """)
    suspend fun getAverageResponseTime(since: LocalDateTime): Float?

    // Tasa de snooze
    @Query("""
        SELECT 
            COUNT(CASE WHEN wasSnoozed = 1 THEN 1 END) * 100.0 / COUNT(*) as snoozeRate
        FROM notification_logs 
        WHERE shownAt >= :since
    """)
    suspend fun getSnoozeRate(since: LocalDateTime): Float?

    // Mejores horarios (cuando el usuario responde más rápido)
    @Query("""
        SELECT 
            strftime('%H', shownAt) as hour,
            AVG(responseTimeSeconds) as avgResponseTime,
            COUNT(*) as notificationCount
        FROM notification_logs 
        WHERE userAction IS NOT NULL 
        AND responseTimeSeconds IS NOT NULL
        GROUP BY hour
        ORDER BY avgResponseTime ASC
    """)
    suspend fun getBestResponseTimesByHour(): List<HourlyResponse>

    // ===== QUERIES - Por evento =====
    @Query("""
        SELECT * FROM notification_logs 
        WHERE eventId = :eventId 
        ORDER BY shownAt DESC
    """)
    suspend fun getLogsByEvent(eventId: String): List<NotificationLog>

    @Query("""
        SELECT COUNT(*) FROM notification_logs 
        WHERE eventId = :eventId
    """)
    suspend fun getNotificationCountForEvent(eventId: String): Int

    // ===== QUERIES - Patrones de usuario =====
    @Query("""
        SELECT 
            eventTitle,
            COUNT(*) as notificationCount,
            AVG(CASE WHEN userAction = 'IGNORED' THEN 1 ELSE 0 END) as ignoreRate
        FROM notification_logs
        GROUP BY eventTitle
        HAVING notificationCount > 3
        ORDER BY ignoreRate DESC
        LIMIT 10
    """)
    suspend fun getMostIgnoredEventTypes(): List<IgnoredEventPattern>

    // ===== UTILITY =====
    @Transaction
    suspend fun logNotificationShown(
        notification: com.chapelotas.app.data.database.entities.ScheduledNotification,
        event: com.chapelotas.app.data.database.entities.EventPlan,
        message: String,
        deviceLocked: Boolean = false,
        appInForeground: Boolean = false
    ): Long {
        val log = NotificationLog(
            notificationId = notification.notificationId,
            eventId = event.eventId,
            eventTitle = event.title,
            shownAt = LocalDateTime.now(),
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
}

// Data classes para queries complejas
data class TypeEffectiveness(
    val type: NotificationType,
    val total: Int,
    val effective: Int,
    val avgResponseTime: Float?
)

data class HourlyResponse(
    val hour: String,
    val avgResponseTime: Float,
    val notificationCount: Int
)

data class IgnoredEventPattern(
    val eventTitle: String,
    val notificationCount: Int,
    val ignoreRate: Float
)