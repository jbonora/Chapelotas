package com.chapelotas.app.data.database.daos

import androidx.room.*
import com.chapelotas.app.data.database.entities.EventDistance
import com.chapelotas.app.data.database.entities.EventPlan
import com.chapelotas.app.data.database.entities.EventResolutionStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

@Dao
interface EventPlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(eventPlan: EventPlan)

    @Update
    suspend fun update(eventPlan: EventPlan)

    @Query("SELECT * FROM event_plans WHERE eventId = :id LIMIT 1")
    suspend fun getEvent(id: String): EventPlan?

    @Query("SELECT * FROM event_plans WHERE dayDate = :date ORDER BY startTime ASC")
    suspend fun getEventsByDate(date: LocalDate): List<EventPlan>

    @Query("SELECT * FROM event_plans WHERE dayDate = :today ORDER BY startTime ASC")
    fun observeTodayEvents(today: LocalDate = LocalDate.now()): Flow<List<EventPlan>>

    @Query("SELECT * FROM event_plans WHERE dayDate = :today AND isCritical = 1 ORDER BY startTime ASC")
    suspend fun getTodayCriticalEvents(today: LocalDate = LocalDate.now()): List<EventPlan>

    @Query("DELETE FROM event_plans WHERE dayDate < :beforeDate")
    suspend fun deleteOldEvents(beforeDate: LocalDate)

    @Query("DELETE FROM event_plans WHERE dayDate = :date")
    suspend fun deleteAllByDate(date: LocalDate)

    @Query("DELETE FROM event_plans WHERE eventId = :eventId")
    suspend fun deleteById(eventId: String)

    // ACTUALIZADO: Ahora marca userModified = 1 cuando el usuario cambia la criticidad
    @Query("UPDATE event_plans SET isCritical = :isCritical, userModified = 1, updatedAt = :now WHERE eventId = :eventId")
    suspend fun updateCritical(eventId: String, isCritical: Boolean, now: LocalDateTime = LocalDateTime.now(ZoneId.systemDefault()))

    // ACTUALIZADO: Ahora marca userModified = 1 cuando el usuario cambia la distancia
    @Query("UPDATE event_plans SET distance = :distance, userModified = 1, updatedAt = :now WHERE eventId = :eventId")
    suspend fun updateDistance(eventId: String, distance: EventDistance, now: LocalDateTime = LocalDateTime.now(ZoneId.systemDefault()))

    @Query("SELECT * FROM event_plans WHERE endTime < :currentTime AND resolutionStatus = 'PENDING' ORDER BY startTime DESC")
    suspend fun getPastUnresolvedEvents(currentTime: LocalDateTime = LocalDateTime.now(ZoneId.systemDefault())): List<EventPlan>

    @Query("UPDATE event_plans SET resolutionStatus = :status, updatedAt = :now WHERE eventId = :eventId")
    suspend fun updateResolutionStatus(eventId: String, status: EventResolutionStatus, now: LocalDateTime = LocalDateTime.now(ZoneId.systemDefault()))

    @Query("UPDATE event_plans SET notificationsSent = notificationsSent + 1, lastNotificationTime = :time WHERE eventId = :eventId")
    suspend fun incrementNotificationCount(eventId: String, time: LocalDateTime)

    // NUEVO: Actualizar estado del plan de AI
    @Query("UPDATE event_plans SET aiPlanStatus = :status WHERE eventId = :eventId")
    suspend fun updateAIPlanStatus(eventId: String, status: String)

    // NUEVO: Obtener eventos no modificados por el usuario
    @Query("SELECT * FROM event_plans WHERE userModified = 0 AND dayDate = :date")
    suspend fun getUnmodifiedEventsByDate(date: LocalDate): List<EventPlan>
}