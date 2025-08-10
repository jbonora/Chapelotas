package com.chapelotas.app.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // --- OPERACIONES DE INSERCIÓN Y ACTUALIZACIÓN ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    // --- OPERACIONES DE ELIMINACIÓN ---

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: String)

    @Query("DELETE FROM tasks WHERE isFinished = 1 AND updatedAt < :beforeDate")
    suspend fun deleteOldCompletedTasks(beforeDate: Long)

    @Query("DELETE FROM tasks WHERE isFinished = 0 AND scheduledTime < :todayStartMillis")
    suspend fun deleteUnfinishedTasksBefore(todayStartMillis: Long)

    // --- OPERACIONES DE LECTURA (QUERIES) ---

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTask(taskId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE calendarEventId = :calendarEventId")
    suspend fun getTaskByCalendarId(calendarEventId: Long): TaskEntity?

    @Query("""
        SELECT * FROM tasks
        WHERE (isFinished = 0)
        AND (nextReminderAt IS NULL OR nextReminderAt <= :now)
        ORDER BY scheduledTime ASC
    """)
    suspend fun getTasksNeedingReminder(now: Long): List<TaskEntity>

    @Query("""
        SELECT * FROM tasks
        WHERE isFromCalendar = 1
        AND scheduledTime >= :startOfDay
        AND scheduledTime < :endOfDay
    """)
    suspend fun getCalendarTasksForDateRange(startOfDay: Long, endOfDay: Long): List<TaskEntity>


    // --- FLUJOS OBSERVABLES (Para que la UI se actualice sola) ---

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun observeTask(taskId: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks ORDER BY scheduledTime ASC")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks
        WHERE scheduledTime >= :startOfDay
        AND scheduledTime < :endOfDay
        ORDER BY scheduledTime ASC
    """)
    fun observeTasksForDate(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks
        WHERE scheduledTime >= :startOfDay
        AND scheduledTime < :endOfDay
        ORDER BY scheduledTime ASC
    """)
    fun observeTasksForDateRange(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>>


    // --- ACTUALIZACIONES DE ESTADO (ACCIONES) ---

    @Query("UPDATE tasks SET isAcknowledged = 1, updatedAt = :timestamp WHERE id = :taskId")
    suspend fun acknowledgeTask(taskId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET isFinished = 1, updatedAt = :timestamp WHERE id = :taskId")
    suspend fun finishTask(taskId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET isFinished = 0, isAcknowledged = 0, reminderCount = 0, nextReminderAt = NULL, isStarted = 0, updatedAt = :timestamp WHERE id = :taskId")
    suspend fun resetTaskStatus(taskId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET isStarted = 1, isAcknowledged = 1, updatedAt = :timestamp WHERE id = :taskId")
    suspend fun startTask(taskId: String, timestamp: Long = System.currentTimeMillis())


    // --- ACTUALIZACIONES DE RECORDATORIOS Y LOGS ---

    @Query("""
        UPDATE tasks
        SET lastReminderAt = :timestamp,
            reminderCount = reminderCount + 1,
            nextReminderAt = :nextReminder,
            updatedAt = :timestamp
        WHERE id = :taskId
    """)
    suspend fun updateReminderSent(taskId: String, timestamp: Long, nextReminder: Long?)

    @Query("""
        UPDATE tasks SET
        reminderCount = :reminderCount,
        nextReminderAt = :nextReminderAt,
        updatedAt = :timestamp
        WHERE id = :taskId
    """)
    suspend fun updateInitialReminderState(taskId: String, reminderCount: Int, nextReminderAt: Long?, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET locationContext = :locationContext, travelTimeMinutes = :travelTime, updatedAt = :timestamp WHERE id = :taskId")
    suspend fun updateTaskLocation(taskId: String, locationContext: String, travelTime: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET conversationLog = :logJson, updatedAt = :timestamp WHERE id = :taskId")
    suspend fun updateConversationLog(taskId: String, logJson: String, timestamp: Long)

    @Query("UPDATE tasks SET conversationLog = :logJson, unreadMessageCount = unreadMessageCount + 1, updatedAt = :timestamp WHERE id = :taskId")
    suspend fun updateConversationLogAndIncrementUnread(taskId: String, logJson: String, timestamp: Long)

    @Query("UPDATE tasks SET unreadMessageCount = 0, updatedAt = :timestamp WHERE id = :taskId")
    suspend fun resetUnreadMessageCount(taskId: String, timestamp: Long = System.currentTimeMillis())
}