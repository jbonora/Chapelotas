package com.chapelotas.app.data.database.daos

import androidx.room.*
import com.chapelotas.app.data.database.entities.DayPlan
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * DAO para operaciones con planes diarios
 */
@Dao
interface DayPlanDao {

    // ===== INSERT =====
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dayPlan: DayPlan)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dayPlans: List<DayPlan>)

    // ===== UPDATE =====
    @Update
    suspend fun update(dayPlan: DayPlan)

    @Query("UPDATE day_plans SET sarcasticMode = :sarcastic WHERE date = :date")
    suspend fun updateSarcasticMode(date: LocalDate, sarcastic: Boolean)

    @Query("UPDATE day_plans SET lastAiAnalysis = datetime('now') WHERE date = :date")
    suspend fun updateLastAiAnalysis(date: LocalDate)

    @Query("UPDATE day_plans SET nextCheckTime = :time WHERE date = :date")
    suspend fun updateNextCheckTime(date: LocalDate, time: String)

    // ===== DELETE =====
    @Delete
    suspend fun delete(dayPlan: DayPlan)

    @Query("DELETE FROM day_plans WHERE date < :beforeDate")
    suspend fun deleteOldPlans(beforeDate: LocalDate)

    // ===== QUERIES =====
    @Query("SELECT * FROM day_plans WHERE date = :date LIMIT 1")
    suspend fun getPlan(date: LocalDate): DayPlan?

    @Query("SELECT * FROM day_plans WHERE date = :date LIMIT 1")
    fun observePlan(date: LocalDate): Flow<DayPlan?>

    @Query("SELECT * FROM day_plans WHERE date = date('now', 'localtime') LIMIT 1")
    suspend fun getTodayPlan(): DayPlan?

    @Query("SELECT * FROM day_plans WHERE date = date('now', 'localtime') LIMIT 1")
    fun observeTodayPlan(): Flow<DayPlan?>

    @Query("SELECT * FROM day_plans WHERE date = date('now', '+1 day', 'localtime') LIMIT 1")
    suspend fun getTomorrowPlan(): DayPlan?

    @Query("SELECT * FROM day_plans WHERE isActive = 1 ORDER BY date DESC")
    fun observeActivePlans(): Flow<List<DayPlan>>

    @Query("SELECT COUNT(*) FROM day_plans")
    suspend fun getCount(): Int

    // ===== UTILITY =====
    @Transaction
    suspend fun getOrCreateTodayPlan(
        userId: String = "",
        userName: String = ""
    ): DayPlan {
        val today = LocalDate.now()
        val existing = getPlan(today)

        return if (existing != null) {
            existing
        } else {
            val newPlan = DayPlan(
                date = today,
                userId = userId,
                userName = userName
            )
            insert(newPlan)
            newPlan
        }
    }
}