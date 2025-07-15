package com.chapelotas.app.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Entity(
    tableName = "event_plans",
    foreignKeys = [
        ForeignKey(
            entity = DayPlan::class,
            parentColumns = ["date"],
            childColumns = ["dayDate"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["dayDate"]), Index(value = ["calendarEventId"], unique = true)]
)
data class EventPlan(
    @PrimaryKey val eventId: String,
    val dayDate: LocalDate,
    val calendarId: Long,
    val calendarEventId: Long,
    val title: String,
    val description: String?,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val location: String?,
    val isAllDay: Boolean,
    var isCritical: Boolean = false,
    var distance: EventDistance = EventDistance.CERCA,
    var hasConflict: Boolean = false,
    @ColumnInfo(defaultValue = "PENDING")
    var resolutionStatus: EventResolutionStatus = EventResolutionStatus.PENDING,

    // CAMBIO: Ahora por defecto es AUTO_APPROVED
    @ColumnInfo(defaultValue = "AUTO_APPROVED")
    var aiPlanStatus: String = "AUTO_APPROVED", // Estados: PENDING_REVIEW, AUTO_APPROVED, USER_APPROVED

    // NUEVO: Campo para saber si el usuario modificó manualmente
    @ColumnInfo(defaultValue = "0")
    var userModified: Boolean = false,

    val notificationsSent: Int = 0,
    val lastNotificationTime: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneId.systemDefault()),
    var updatedAt: LocalDateTime? = LocalDateTime.now(ZoneId.systemDefault())
) {
    val durationInMinutes: Long
        get() = Duration.between(startTime, endTime).toMinutes()

    fun getNotificationMinutesList(): List<Int> {
        return when (distance) {
            EventDistance.EN_OFI -> listOf(15, 5)
            EventDistance.CERCA -> listOf(30, 15, 5)
            EventDistance.LEJOS -> listOf(90, 60, 30)
        }
    }
}