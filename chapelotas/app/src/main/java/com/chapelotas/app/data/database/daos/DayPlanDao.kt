package com.chapelotas.app.data.database.daos

import androidx.room.*
import com.chapelotas.app.data.database.entities.DayPlan
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime // Asegúrate de que este import esté presente

@Dao
interface DayPlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dayPlan: DayPlan)

    @Update
    suspend fun update(dayPlan: DayPlan)

    @Query("UPDATE day_plans SET sarcasticMode = :sarcastic WHERE date = :date")
    suspend fun updateSarcasticMode(date: LocalDate, sarcastic: Boolean)

    @Query("DELETE FROM day_plans WHERE date < :beforeDate")
    suspend fun deleteOldPlans(beforeDate: LocalDate)

    @Query("SELECT * FROM day_plans WHERE date = :date LIMIT 1")
    suspend fun getPlan(date: LocalDate): DayPlan?

    @Query("SELECT * FROM day_plans WHERE date = :date LIMIT 1")
    fun observePlan(date: LocalDate): Flow<DayPlan?>

    @Query("SELECT * FROM day_plans WHERE date = :today LIMIT 1")
    suspend fun getTodayPlan(today: LocalDate = LocalDate.now()): DayPlan?

    @Query("SELECT * FROM day_plans WHERE date = :today LIMIT 1")
    fun observeTodayPlan(today: LocalDate = LocalDate.now()): Flow<DayPlan?>

    // --- NUEVA FUNCIÓN AÑADIDA ---
    // Esta es la única función nueva que se necesitaba.
    @Query("UPDATE day_plans SET nextAlarmTime = :time WHERE date = :date")
    suspend fun updateNextAlarmTime(date: LocalDate, time: LocalDateTime?)


    @Transaction
    suspend fun getOrCreateTodayPlan(): DayPlan {
        val today = LocalDate.now()
        val existing = getPlan(today)
        return if (existing != null) {
            existing
        } else {
            val newPlan = DayPlan(date = today)
            insert(newPlan)
            newPlan
        }
    }
}