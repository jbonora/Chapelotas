package com.chapelotas.app.data.database.daos

import androidx.room.*
import com.chapelotas.app.data.database.entities.MonkeyAgenda
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface MonkeyAgendaDao {

    @Insert
    suspend fun insert(item: MonkeyAgenda): Long

    @Update
    suspend fun update(item: MonkeyAgenda)

    @Query("SELECT * FROM monkey_agenda WHERE status = 'PENDING' ORDER BY scheduledTime ASC LIMIT 1")
    suspend fun getNextPendingAction(): MonkeyAgenda?

    @Query("SELECT * FROM monkey_agenda WHERE status = 'PENDING' AND scheduledTime <= :now ORDER BY scheduledTime ASC")
    suspend fun getPendingActionsUntilNow(now: LocalDateTime): List<MonkeyAgenda>

    @Query("UPDATE monkey_agenda SET status = 'PROCESSING' WHERE id = :id")
    suspend fun markAsProcessing(id: Long)

    @Query("UPDATE monkey_agenda SET status = 'COMPLETED', processedAt = :processedAt WHERE id = :id")
    suspend fun markAsCompleted(id: Long, processedAt: LocalDateTime = LocalDateTime.now(ZoneId.systemDefault()))

    @Query("UPDATE monkey_agenda SET status = 'CANCELLED' WHERE id = :id")
    suspend fun markAsCancelled(id: Long)

    @Query("UPDATE monkey_agenda SET status = 'CANCELLED' WHERE eventId = :eventId AND status = 'PENDING'")
    suspend fun cancelPendingForEvent(eventId: String)

    @Query("SELECT * FROM monkey_agenda WHERE status = 'PENDING' ORDER BY scheduledTime ASC")
    fun observePendingActions(): Flow<List<MonkeyAgenda>>

    @Query("SELECT COUNT(*) FROM monkey_agenda WHERE status = 'PENDING'")
    suspend fun getPendingCount(): Int

    // Para debug
    @Query("SELECT * FROM monkey_agenda ORDER BY scheduledTime DESC LIMIT :limit")
    suspend fun getRecentActions(limit: Int = 10): List<MonkeyAgenda>

    // Limpieza - mover completados viejos a archivo
    @Query("DELETE FROM monkey_agenda WHERE status = 'COMPLETED' AND processedAt < :beforeDate")
    suspend fun deleteOldCompleted(beforeDate: LocalDateTime)
}