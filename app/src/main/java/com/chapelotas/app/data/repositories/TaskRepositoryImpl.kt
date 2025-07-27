package com.chapelotas.app.data.repositories

import com.chapelotas.app.data.database.TaskDao
import com.chapelotas.app.data.database.TaskEntity
import com.chapelotas.app.domain.entities.CalendarEvent
import com.chapelotas.app.domain.models.Task
import com.chapelotas.app.domain.repositories.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación del TaskRepository.
 * Conecta la lógica de negocio con las operaciones de la base de datos (DAO).
 */
@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {

    // Convierte un objeto de dominio (Task) a una entidad de base de datos (TaskEntity) y lo inserta.
    override suspend fun createTask(task: Task): String {
        val entity = TaskEntity.fromDomainModel(task)
        taskDao.insert(entity)
        return entity.id
    }

    // Busca una entidad en la BD y la convierte a un objeto de dominio.
    override suspend fun getTask(taskId: String): Task? {
        return taskDao.getTask(taskId)?.toDomainModel()
    }

    override suspend fun deleteTask(taskId: String) {
        taskDao.deleteById(taskId)
    }

    // Observa una entidad y la transforma en un flujo de objetos de dominio.
    override fun observeTask(taskId: String): Flow<Task?> {
        return taskDao.observeTask(taskId).map { it?.toDomainModel() }
    }

    // Observa las tareas de una fecha y las transforma a objetos de dominio.
    override fun observeTasksForDate(date: LocalDate): Flow<List<Task>> {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return taskDao.observeTasksForDate(startOfDay, endOfDay).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun observeAllTasks(): Flow<List<Task>> {
        return taskDao.observeAllTasks().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getTasksNeedingReminder(): List<Task> {
        val now = System.currentTimeMillis()
        return taskDao.getTasksNeedingReminder(now).map { it.toDomainModel() }
    }

    override suspend fun getTaskByCalendarId(calendarEventId: Long): Task? {
        return taskDao.getTaskByCalendarId(calendarEventId)?.toDomainModel()
    }

    // --- Simples delegaciones de acciones al DAO ---
    override suspend fun acknowledgeTask(taskId: String) {
        taskDao.acknowledgeTask(taskId)
    }

    override suspend fun finishTask(taskId: String) {
        taskDao.finishTask(taskId)
    }

    override suspend fun resetTaskStatus(taskId: String) {
        taskDao.resetTaskStatus(taskId)
    }

    override suspend fun recordReminderSent(taskId: String, nextReminderTime: LocalDateTime?) {
        val now = System.currentTimeMillis()
        val nextReminder = nextReminderTime?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        taskDao.updateReminderSent(taskId, now, nextReminder)
    }

    override suspend fun updateInitialReminderState(taskId: String, reminderCount: Int, nextReminderAt: LocalDateTime?) {
        val nextReminderMillis = nextReminderAt?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        taskDao.updateInitialReminderState(taskId, reminderCount, nextReminderMillis)
    }

    /**
     * Lógica de sincronización con eventos del calendario.
     * Añade nuevos, actualiza los existentes y elimina los que ya no están.
     */
    override suspend fun syncWithCalendarEvents(events: List<CalendarEvent>) {
        val activeCalendarIds = events.map { it.id }
        taskDao.deleteRemovedCalendarEvents(activeCalendarIds)

        events.forEach { event ->
            val taskId = "cal_${event.id}"
            val existingTaskEntity = taskDao.getTask(taskId)

            if (existingTaskEntity == null) {
                // Si la tarea no existe, se crea desde el evento del calendario.
                val newTask = TaskEntity.fromCalendarEvent(event)
                taskDao.insert(newTask)
            } else {
                // Si ya existe, se actualiza solo si algo cambió.
                val updatedTask = existingTaskEntity.copy(
                    title = event.title,
                    description = event.description,
                    scheduledTime = event.getStartMillis(),
                    endTime = event.getEndMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                if (updatedTask != existingTaskEntity) {
                    taskDao.update(updatedTask)
                }
            }
        }
    }

    override suspend fun cleanOldCompletedTasks(daysToKeep: Int) {
        val beforeDate = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        taskDao.deleteOldCompletedTasks(beforeDate)
    }
}