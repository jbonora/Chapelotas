package com.chapelotas.app.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "event_conflicts",
    indices = [Index("event1Id"), Index("event2Id"), Index("resolved"), Index("date")]
)
data class EventConflict(
    @PrimaryKey(autoGenerate = true) val conflictId: Long = 0,
    val event1Id: String,
    val event1Title: String,
    val event1Time: String,
    val event2Id: String,
    val event2Title: String,
    val event2Time: String,
    val type: ConflictType,
    val date: LocalDateTime,
    val overlapMinutes: Int? = null,
    val distanceMinutes: Int? = null,
    val aiMessage: String,
    val severity: ConflictSeverity = ConflictSeverity.MEDIUM, // Ahora esta línea es válida
    val resolved: Boolean = false,
    val resolvedAt: LocalDateTime? = null,
    val resolutionNote: String? = null,
    val userNotified: Boolean = false,
    val detectedAt: LocalDateTime
) {
    fun getUserMessage(): String {
        return when (type) {
            ConflictType.OVERLAPPING -> "⚠️ '$event1Title' y '$event2Title' se superponen por $overlapMinutes minutos"
            ConflictType.TOO_CLOSE -> "⏰ Solo tenés $distanceMinutes minutos entre '$event1Title' y '$event2Title'"
            ConflictType.SAME_LOCATION -> "📍 Mismo lugar al mismo tiempo: '$event1Title' y '$event2Title'"
        }
    }
}