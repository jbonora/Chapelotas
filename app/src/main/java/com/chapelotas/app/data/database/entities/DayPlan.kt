package com.chapelotas.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Plan maestro del día - equivalente al JSON maestro
 * Un registro por día
 */
@Entity(tableName = "day_plans")
data class DayPlan(
    @PrimaryKey
    val date: LocalDate,

    val userId: String = "",
    val userName: String = "",
    val sarcasticMode: Boolean = false,
    val lastAiAnalysis: LocalDateTime = LocalDateTime.now(),
    val isActive: Boolean = true,
    val nextCheckTime: String = "", // HH:mm format

    // Metadata
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Verifica si es momento de hacer un nuevo análisis con IA
     * (cada 4 horas o si nunca se hizo)
     */
    fun needsAiReanalysis(): Boolean {
        val hoursSinceLastAnalysis = java.time.Duration
            .between(lastAiAnalysis, LocalDateTime.now())
            .toHours()
        return hoursSinceLastAnalysis >= 4
    }
}