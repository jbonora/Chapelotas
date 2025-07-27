package com.chapelotas.app.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

/**
 * Data Access Object (DAO) para las tareas.
 * Define todas las operaciones de base de datos (consultas SQL) para la entidad TaskEntity.
 * Es la única clase que habla directamente con la tabla "tasks".
 */
@Dao
interface TaskDao {

    // --- OPERACIONES BÁSICAS DE ESCRITURA ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: String)


    // --- OPERACIONES DE LECTURA (QUERIES) ---

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTask(taskId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE calendarEventId = :calendarEventId")
    suspend fun getTaskByCalendarId(calendarEventId: Long): TaskEntity?

    /**
     * Obtiene todas las tareas que no están finalizadas y cuyo próximo recordatorio ya pasó o nunca fue programado.
     * Esta es la consulta clave para el ReminderEngine.
     */
    @Query("""
        SELECT * FROM tasks
        WHERE (isFinished = 0)
        AND (nextReminderAt IS NULL OR nextReminderAt <= :now)
        ORDER BY scheduledTime ASC
    """)
    suspend fun getTasksNeedingReminder(now: Long): List<TaskEntity>


    // --- FLUJOS OBSERVABLES (Para que la UI se actualice sola) ---

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun observeTask(taskId: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks ORDER BY scheduledTime ASC")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    /**
     * Observa las tareas para una fecha específica.
     */
    @Query("""
        SELECT * FROM tasks
        WHERE scheduledTime >= :startOfDay
        AND scheduledTime < :endOfDay
        ORDER BY scheduledTime ASC
    """)
    fun observeTasksForDate(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>>


    // --- ACTUALIZACIONES DE ESTADO (ACCIONES) ---

    @Query("UPDATE tasks SET isAcknowledged = 1, updatedAt = :timestamp WHERE id = :taskId")
    suspend fun acknowledgeTask(taskId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET isFinished = 1, updatedAt = :timestamp WHERE id = :taskId")
    suspend fun finishTask(taskId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET isFinished = 0, isAcknowledged = 0, reminderCount = 0, nextReminderAt = NULL, updatedAt = :timestamp WHERE id = :taskId")
    suspend fun resetTaskStatus(taskId: String, timestamp: Long = System.currentTimeMillis())

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


    // --- OPERACIONES DE SINCRONIZACIÓN Y LIMPIEZA ---

    @Query("DELETE FROM tasks WHERE isFromCalendar = 1 AND calendarEventId NOT IN (:activeCalendarIds)")
    suspend fun deleteRemovedCalendarEvents(activeCalendarIds: List<Long>)

    @Query("DELETE FROM tasks WHERE isFinished = 1 AND updatedAt < :beforeDate")
    suspend fun deleteOldCompletedTasks(beforeDate: Long)
}