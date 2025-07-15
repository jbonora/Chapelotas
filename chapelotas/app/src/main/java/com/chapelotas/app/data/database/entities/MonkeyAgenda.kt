package com.chapelotas.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Tabla principal del mono - Una sola fuente de verdad para todas las acciones
 * Cada entrada representa UNA acción que el mono debe ejecutar
 */
@Entity(tableName = "monkey_agenda")
data class MonkeyAgenda(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Cuándo el mono debe actuar
    val scheduledTime: LocalDateTime,

    // Si es sobre un evento específico (puede ser null para mensajes generales)
    val eventId: String? = null,

    // Tipo de acción: "NOTIFY_EVENT", "DAILY_SUMMARY", "IDLE_CHECK", "CLEANUP"
    val actionType: String,

    // Estado: "PENDING", "PROCESSING", "COMPLETED", "CANCELLED"
    val status: String = "PENDING",

    // Cuándo se creó esta entrada
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneId.systemDefault()),

    // Cuándo se procesó (null si aún no se procesó)
    val processedAt: LocalDateTime? = null
)