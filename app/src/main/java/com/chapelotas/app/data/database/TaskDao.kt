package com.chapelotas.app.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: String)

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTask(taskId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun observeTask(taskId: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks ORDER BY scheduledTime ASC")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    // --- INICIO DE LA CORRECCIÓN (1/2) ---
    // Las tareas pendientes ahora son todas aquellas que no están marcadas como finalizadas.
    @Query("SELECT * FROM tasks WHERE isFinished = 0 ORDER BY scheduledTime ASC")
    fun observePendingTasks(): Flow<List<TaskEntity>>
    // --- FIN DE LA CORRECCIÓN (1/2) ---

    @Query("""
        SELECT * FROM tasks
        WHERE scheduledTime >= :startOfDay
        AND scheduledTime < :endOfDay
        ORDER BY scheduledTime ASC
    """)
    fun observeTasksForDate(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks
        WHERE (isFinished = 0)
        AND (nextReminderAt IS NULL OR nextReminderAt <= :now)
        ORDER BY scheduledTime ASC
    """)
    suspend fun getTasksNeedingReminder(now: Long): List<TaskEntity>

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

    // --- INICIO DE LA CORRECCIÓN (2/2) ---
    // Nuevas acciones que manipulan las banderas booleanas
    @Query("UPDATE tasks SET isAcknowledged = 1, updatedAt = :timestamp WHERE id = :taskId")
    suspend fun acknowledgeTask(taskId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET isFinished = 1, updatedAt = :timestamp WHERE id = :taskId")
    suspend fun finishTask(taskId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET isFinished = 0, isAcknowledged = 0, reminderCount = 0, nextReminderAt = NULL, updatedAt = :timestamp WHERE id = :taskId")
    suspend fun resetTaskStatus(taskId: String, timestamp: Long = System.currentTimeMillis())
    // --- FIN DE LA CORRECCIÓN (2/2) ---


    // --- MÉTODOS RELACIONADOS CON EL CALENDARIO Y LIMPIEZA ---
    @Query("SELECT * FROM tasks WHERE calendarEventId = :calendarEventId")
    suspend fun getTaskByCalendarId(calendarEventId: Long): TaskEntity?

    @Query("DELETE FROM tasks WHERE isFromCalendar = 1 AND calendarEventId NOT IN (:activeCalendarIds)")
    suspend fun deleteRemovedCalendarEvents(activeCalendarIds: List<Long>)

    @Query("DELETE FROM tasks WHERE isFinished = 1 AND updatedAt < :beforeDate")
    suspend fun deleteOldCompletedTasks(beforeDate: Long)


    // --- COMPANION OBJECT (sin cambios) ---
    companion object {
        fun startOfDay(date: LocalDate): Long {
            return date.atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }

        fun endOfDay(date: LocalDate): Long {
            return date.atTime(LocalTime.MAX)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }
    }
}