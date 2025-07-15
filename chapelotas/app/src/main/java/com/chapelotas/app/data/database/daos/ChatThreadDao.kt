package com.chapelotas.app.data.database.daos

import androidx.room.*
import com.chapelotas.app.data.database.entities.ChatThread
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface ChatThreadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(thread: ChatThread)

    @Query("SELECT * FROM chat_threads WHERE threadId = :threadId")
    suspend fun getThread(threadId: String): ChatThread?

    @Query("SELECT * FROM chat_threads WHERE status != 'ARCHIVED' ORDER BY lastMessageTime DESC")
    fun observeActiveThreads(): Flow<List<ChatThread>>

    @Query("UPDATE chat_threads SET lastMessage = :message, lastMessageTime = :time, unreadCount = unreadCount + 1 WHERE threadId = :threadId")
    suspend fun updateLastMessage(threadId: String, message: String, time: LocalDateTime = LocalDateTime.now(ZoneId.systemDefault()))

    @Query("UPDATE chat_threads SET unreadCount = 0 WHERE threadId = :threadId")
    suspend fun markAsRead(threadId: String)

    @Query("UPDATE chat_threads SET status = :status WHERE threadId = :threadId")
    suspend fun updateStatus(threadId: String, status: String)

    @Query("SELECT * FROM chat_threads WHERE eventId = :eventId LIMIT 1")
    suspend fun getThreadForEvent(eventId: String): ChatThread?

    @Query("SELECT COUNT(*) FROM chat_threads WHERE unreadCount > 0")
    fun observeUnreadCount(): Flow<Int>

    @Query("UPDATE chat_threads SET status = 'ARCHIVED' WHERE status = 'COMPLETED' AND lastMessageTime < :beforeDate")
    suspend fun archiveOldCompleted(beforeDate: LocalDateTime)

    // Para obtener threads por tipo
    @Query("SELECT * FROM chat_threads WHERE threadType = :type AND status != 'ARCHIVED' ORDER BY lastMessageTime DESC")
    suspend fun getThreadsByType(type: String): List<ChatThread>
}