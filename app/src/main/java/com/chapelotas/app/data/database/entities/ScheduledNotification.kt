package com.chapelotas.app.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

/**
 * Notificaciones programadas - el mono checkea esta tabla
 */
@Entity(
    tableName = "scheduled_notifications",
    foreignKeys = [
        ForeignKey(
            entity = EventPlan::class,
            parentColumns = ["eventId"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("eventId"),
        Index("scheduledTime"),
        Index("executed"),
        Index("snoozedUntil")
    ]
)
data class ScheduledNotification(
    @PrimaryKey(autoGenerate = true)
    val notificationId: Long = 0, // Auto-generado, consistente!

    // Relación con evento
    val eventId: String,

    // Timing
    val scheduledTime: LocalDateTime,
    val minutesBefore: Int,

    // Tipo y prioridad
    val type: NotificationType = NotificationType.REMINDER,
    val priority: NotificationPriority = NotificationPriority.NORMAL,

    // Estado
    val executed: Boolean = false,
    val executedAt: LocalDateTime? = null,
    val snoozedUntil: LocalDateTime? = null,
    val snoozeCount: Int = 0,
    val dismissed: Boolean = false,

    // Contenido
    val message: String? = null, // Mensaje pre-generado por IA
    val aiReason: String? = null, // Por qué la IA sugirió este tiempo

    // WorkManager (si se usa)
    val workManagerId: String? = null,

    // Metadata
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Verifica si debe mostrarse ahora
     */
    fun shouldShowNow(): Boolean {
        val now = LocalDateTime.now()

        // Si está snoozeada, usar ese tiempo
        val targetTime = snoozedUntil ?: scheduledTime

        return !executed &&
                !dismissed &&
                now.isAfter(targetTime)
    }

    /**
     * Verifica si está activa (no ejecutada ni descartada)
     */
    fun isActive(): Boolean {
        return !executed && !dismissed
    }

    /**
     * Crea una copia para snooze
     */
    fun snooze(minutes: Int): ScheduledNotification {
        return copy(
            snoozedUntil = LocalDateTime.now().plusMinutes(minutes.toLong()),
            snoozeCount = snoozeCount + 1
        )
    }

    /**
     * Genera ID único para Android NotificationManager
     */
    fun getAndroidNotificationId(): Int {
        // Usa los primeros 31 bits del hash del notificationId
        // Esto garantiza IDs únicos y consistentes
        return notificationId.toInt() and Int.MAX_VALUE
    }
}