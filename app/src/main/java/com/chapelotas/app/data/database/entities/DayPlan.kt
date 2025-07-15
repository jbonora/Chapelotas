package com.chapelotas.app.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime // ---> AÑADE ESTE IMPORT <---

@Entity(tableName = "day_plans")
data class DayPlan(
    @PrimaryKey val date: LocalDate,
    var lastUpdated: Long = System.currentTimeMillis(),
    var sarcasticMode: Boolean = true,

    @ColumnInfo(defaultValue = "09:00")
    var workStartTime: LocalTime = LocalTime.of(9, 0),

    @ColumnInfo(defaultValue = "18:00")
    var workEndTime: LocalTime = LocalTime.of(18, 0),

    @ColumnInfo(defaultValue = "1")  // CAMBIADO: "1" = true para que coincida con el valor inicial
    var is24hMode: Boolean = true,

    // --- CAMBIO CLAVE AQUÍ ---
    // El nuevo campo para recordar la próxima alarma.
    var nextAlarmTime: LocalDateTime? = null
)