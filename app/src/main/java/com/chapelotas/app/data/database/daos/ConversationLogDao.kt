package com.chapelotas.app.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.chapelotas.app.data.database.entities.ConversationLog
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface ConversationLogDao {

    @Insert
    suspend fun insert(log: ConversationLog)

    @Query("SELECT * FROM conversation_logs ORDER BY timestamp ASC LIMIT 50")
    fun observeConversationHistory(): Flow<List<ConversationLog>>

    @Query("SELECT * FROM conversation_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentHistory(limit: Int): List<ConversationLog>

    @Query("DELETE FROM conversation_logs WHERE timestamp < :beforeDate")
    suspend fun deleteOldLogs(beforeDate: LocalDateTime)

    @Query("SELECT COUNT(*) FROM conversation_logs")
    suspend fun getLogCount(): Int

    // ===== MÉTODOS PARA THREADS =====

    // Obtener mensajes de un thread específico
    @Query("SELECT * FROM conversation_logs WHERE threadId = :threadId ORDER BY timestamp ASC")
    fun observeThreadMessages(threadId: String): Flow<List<ConversationLog>>

    // Obtener mensajes de un thread con límite
    @Query("SELECT * FROM conversation_logs WHERE threadId = :threadId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getThreadMessages(threadId: String, limit: Int = 50): List<ConversationLog>

    // Marcar mensajes como leídos
    @Query("UPDATE conversation_logs SET messageStatus = 'READ' WHERE threadId = :threadId AND messageStatus = 'UNREAD'")
    suspend fun markThreadMessagesAsRead(threadId: String)

    // Contar mensajes no leídos por thread
    @Query("SELECT COUNT(*) FROM conversation_logs WHERE threadId = :threadId AND messageStatus = 'UNREAD'")
    suspend fun getUnreadCountForThread(threadId: String): Int

    // Obtener último mensaje de un thread
    @Query("SELECT * FROM conversation_logs WHERE threadId = :threadId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessageForThread(threadId: String): ConversationLog?

    // Para migración: actualizar mensajes viejos con threadId
    @Query("UPDATE conversation_logs SET threadId = :threadId WHERE eventId = :eventId AND threadId IS NULL")
    suspend fun updateThreadIdForEvent(eventId: String, threadId: String)

    // ===== MÉTODOS ADICIONALES PARA MIGRACIÓN COMPLETA =====

    // Actualizar threadId de un mensaje específico
    @Query("UPDATE conversation_logs SET threadId = :threadId WHERE logId = :logId")
    suspend fun updateThreadId(logId: Long, threadId: String)

    // Obtener mensajes sin thread asignado
    @Query("SELECT * FROM conversation_logs WHERE threadId IS NULL OR threadId = ''")
    suspend fun getMessagesWithoutThread(): List<ConversationLog>

    // Actualizar mensajes sin thread al thread general
    @Query("UPDATE conversation_logs SET threadId = 'general' WHERE threadId IS NULL OR threadId = ''")
    suspend fun assignOrphanMessagesToGeneral()
}