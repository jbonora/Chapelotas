package com.chapelotas.app.data.database.daos

import androidx.room.*
import com.chapelotas.app.data.database.entities.EventConflict
import com.chapelotas.app.data.database.entities.ConflictSeverity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * DAO para operaciones con conflictos entre eventos
 */
@Dao
interface ConflictDao {

    // ===== INSERT =====
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conflict: EventConflict): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(conflicts: List<EventConflict>): List<Long>

    // ===== UPDATE =====
    @Update
    suspend fun update(conflict: EventConflict)

    @Query("""
        UPDATE event_conflicts 
        SET resolved = 1, resolvedAt = :resolvedAt, resolutionNote = :note 
        WHERE conflictId = :conflictId
    """)
    suspend fun markAsResolved(
        conflictId: Long,
        resolvedAt: LocalDateTime = LocalDateTime.now(),
        note: String? = null
    )

    @Query("""
        UPDATE event_conflicts 
        SET userNotified = 1 
        WHERE conflictId = :conflictId
    """)
    suspend fun markAsNotified(conflictId: Long)

    // ===== DELETE =====
    @Delete
    suspend fun delete(conflict: EventConflict)

    @Query("DELETE FROM event_conflicts WHERE event1Id = :eventId OR event2Id = :eventId")
    suspend fun deleteByEventId(eventId: String)

    @Query("DELETE FROM event_conflicts WHERE resolved = 1 AND resolvedAt < :beforeDate")
    suspend fun deleteOldResolved(beforeDate: LocalDateTime)

    // ===== QUERIES - Single =====
    @Query("SELECT * FROM event_conflicts WHERE conflictId = :id LIMIT 1")
    suspend fun getConflict(id: Long): EventConflict?

    // ===== QUERIES - Active Conflicts =====
    @Query("""
        SELECT * FROM event_conflicts 
        WHERE resolved = 0 
        AND date >= datetime('now', 'localtime')
        ORDER BY severity DESC, date ASC
    """)
    suspend fun getActiveConflicts(): List<EventConflict>

    @Query("""
        SELECT * FROM event_conflicts 
        WHERE resolved = 0 
        AND date >= datetime('now', 'localtime')
        ORDER BY severity DESC, date ASC
    """)
    fun observeActiveConflicts(): Flow<List<EventConflict>>

    // ===== QUERIES - By Event =====
    @Query("""
        SELECT * FROM event_conflicts 
        WHERE (event1Id = :eventId OR event2Id = :eventId)
        AND resolved = 0
        ORDER BY date ASC
    """)
    suspend fun getActiveConflictsByEvent(eventId: String): List<EventConflict>

    @Query("""
        SELECT * FROM event_conflicts 
        WHERE (event1Id = :eventId OR event2Id = :eventId)
        ORDER BY date DESC
    """)
    suspend fun getAllConflictsByEvent(eventId: String): List<EventConflict>

    // ===== QUERIES - Unnotified =====
    @Query("""
        SELECT * FROM event_conflicts 
        WHERE resolved = 0 
        AND userNotified = 0
        AND date >= datetime('now', 'localtime')
        ORDER BY severity DESC, date ASC
    """)
    suspend fun getUnnotifiedConflicts(): List<EventConflict>

    // ===== QUERIES - By Date =====
    @Query("""
        SELECT * FROM event_conflicts 
        WHERE date(date) = date(:date)
        ORDER BY severity DESC, event1Time ASC
    """)
    suspend fun getConflictsByDate(date: LocalDateTime): List<EventConflict>

    // ===== QUERIES - Analytics =====
    @Query("""
        SELECT COUNT(*) FROM event_conflicts 
        WHERE resolved = 0 AND severity = :severity
    """)
    suspend fun getActiveCountBySeverity(severity: ConflictSeverity): Int

    @Query("""
        SELECT COUNT(*) FROM event_conflicts 
        WHERE date(date) = date('now', 'localtime') AND resolved = 0
    """)
    suspend fun getTodayActiveCount(): Int

    // ===== UTILITY =====
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM event_conflicts 
            WHERE ((event1Id = :event1 AND event2Id = :event2) OR 
                   (event1Id = :event2 AND event2Id = :event1))
            AND resolved = 0
        )
    """)
    suspend fun existsActiveConflict(event1: String, event2: String): Boolean

    @Transaction
    suspend fun resolveAllForEvent(eventId: String, reason: String = "Evento modificado") {
        val conflicts = getActiveConflictsByEvent(eventId)
        conflicts.forEach { conflict ->
            markAsResolved(conflict.conflictId, note = reason)
        }
    }
}