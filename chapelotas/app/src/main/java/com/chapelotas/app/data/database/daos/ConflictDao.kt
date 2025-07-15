package com.chapelotas.app.data.database.daos

import androidx.room.*
import com.chapelotas.app.data.database.entities.EventConflict
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface ConflictDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conflict: EventConflict): Long

    @Query("DELETE FROM event_conflicts WHERE event1Id = :eventId OR event2Id = :eventId")
    suspend fun deleteByEventId(eventId: String)

    @Query("DELETE FROM event_conflicts WHERE date = :date")
    suspend fun deleteByDate(date: LocalDateTime)

    @Query("SELECT * FROM event_conflicts WHERE resolved = 0 AND date >= :now ORDER BY severity DESC, date ASC")
    fun observeActiveConflicts(now: LocalDateTime = LocalDateTime.now(ZoneId.systemDefault())): Flow<List<EventConflict>>

    @Query("SELECT * FROM event_conflicts WHERE resolved = 0 AND userNotified = 0 AND date >= :now ORDER BY severity DESC, date ASC")
    suspend fun getUnnotifiedConflicts(now: LocalDateTime = LocalDateTime.now(ZoneId.systemDefault())): List<EventConflict>

    @Query("UPDATE event_conflicts SET userNotified = 1 WHERE conflictId = :conflictId")
    suspend fun markAsNotified(conflictId: Long)
}