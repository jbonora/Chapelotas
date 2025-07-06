package com.chapelotas.app.data.database.daos

import androidx.room.*
import com.chapelotas.app.data.database.entities.EventPlan
import com.chapelotas.app.data.database.entities.EventDistance
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * DAO para operaciones con planes de eventos
 */
@Dao
interface EventPlanDao {

    // ===== INSERT =====
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(eventPlan: EventPlan)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(eventPlans: List<EventPlan>)

    // ===== UPDATE =====
    @Update
    suspend fun update(eventPlan: EventPlan)

    @Query("""
        UPDATE event_plans 
        SET isCritical = :critical, updatedAt = datetime('now') 
        WHERE eventId = :eventId
    """)
    suspend fun updateCritical(eventId: String, critical: Boolean)

    @Query("""
        UPDATE event_plans 
        SET distance = :distance, updatedAt = datetime('now') 
        WHERE eventId = :eventId
    """)
    suspend fun updateDistance(eventId: String, distance: EventDistance)

    @Query("""
        UPDATE event_plans 
        SET suggestedNotificationMinutes = :minutes, updatedAt = datetime('now') 
        WHERE eventId = :eventId
    """)
    suspend fun updateNotificationMinutes(eventId: String, minutes: String)

    @Query("""
        UPDATE event_plans 
        SET hasConflict = :hasConflict, updatedAt = datetime('now') 
        WHERE eventId = :eventId
    """)
    suspend fun updateConflictStatus(eventId: String, hasConflict: Boolean)

    @Query("""
        UPDATE event_plans 
        SET notificationsSent = notificationsSent + 1,
            lastNotificationTime = :time
        WHERE eventId = :eventId
    """)
    suspend fun incrementNotificationCount(eventId: String, time: LocalDateTime)

    // ===== DELETE =====
    @Delete
    suspend fun delete(eventPlan: EventPlan)

    @Query("DELETE FROM event_plans WHERE eventId = :eventId")
    suspend fun deleteById(eventId: String)

    @Query("DELETE FROM event_plans WHERE dayDate < :beforeDate")
    suspend fun deleteOldEvents(beforeDate: LocalDate)

    // ===== QUERIES - Single =====
    @Query("SELECT * FROM event_plans WHERE eventId = :eventId LIMIT 1")
    suspend fun getEvent(eventId: String): EventPlan?

    @Query("SELECT * FROM event_plans WHERE eventId = :eventId LIMIT 1")
    fun observeEvent(eventId: String): Flow<EventPlan?>

    // ===== QUERIES - Today =====
    @Query("""
        SELECT * FROM event_plans 
        WHERE dayDate = date('now', 'localtime') 
        ORDER BY startTime ASC
    """)
    suspend fun getTodayEvents(): List<EventPlan>

    @Query("""
        SELECT * FROM event_plans 
        WHERE dayDate = date('now', 'localtime') 
        ORDER BY startTime ASC
    """)
    fun observeTodayEvents(): Flow<List<EventPlan>>

    @Query("""
        SELECT * FROM event_plans 
        WHERE dayDate = date('now', 'localtime') 
        AND isCritical = 1
        ORDER BY startTime ASC
    """)
    suspend fun getTodayCriticalEvents(): List<EventPlan>

    @Query("""
        SELECT * FROM event_plans 
        WHERE dayDate = date('now', 'localtime')
        AND datetime(startTime) > datetime('now')
        ORDER BY startTime ASC
        LIMIT 1
    """)
    suspend fun getNextEvent(): EventPlan?

    // ===== QUERIES - Tomorrow =====
    @Query("""
        SELECT * FROM event_plans 
        WHERE dayDate = date('now', '+1 day', 'localtime') 
        ORDER BY startTime ASC
    """)
    suspend fun getTomorrowEvents(): List<EventPlan>

    @Query("""
        SELECT * FROM event_plans 
        WHERE dayDate = date('now', '+1 day', 'localtime') 
        ORDER BY startTime ASC
    """)
    fun observeTomorrowEvents(): Flow<List<EventPlan>>

    // ===== QUERIES - By Date =====
    @Query("""
        SELECT * FROM event_plans 
        WHERE dayDate = :date 
        ORDER BY startTime ASC
    """)
    suspend fun getEventsByDate(date: LocalDate): List<EventPlan>

    @Query("""
        SELECT * FROM event_plans 
        WHERE dayDate = :date 
        ORDER BY startTime ASC
    """)
    fun observeEventsByDate(date: LocalDate): Flow<List<EventPlan>>

    // ===== QUERIES - Conflicts =====
    @Query("""
        SELECT * FROM event_plans 
        WHERE hasConflict = 1 
        AND dayDate >= date('now', 'localtime')
        ORDER BY startTime ASC
    """)
    suspend fun getEventsWithConflicts(): List<EventPlan>

    // ===== QUERIES - Analytics =====
    @Query("""
        SELECT COUNT(*) FROM event_plans 
        WHERE dayDate = :date AND isCritical = 1
    """)
    suspend fun getCriticalCountForDate(date: LocalDate): Int

    @Query("""
        SELECT COUNT(*) FROM event_plans 
        WHERE dayDate = :date
    """)
    suspend fun getEventCountForDate(date: LocalDate): Int

    // ===== UTILITY =====
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM event_plans 
            WHERE calendarEventId = :calendarId 
            AND dayDate = :date
        )
    """)
    suspend fun existsByCalendarId(calendarId: Long, date: LocalDate): Boolean
}