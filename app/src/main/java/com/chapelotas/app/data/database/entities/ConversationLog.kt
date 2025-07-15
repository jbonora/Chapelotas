package com.chapelotas.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import androidx.room.ColumnInfo

@Entity(tableName = "conversation_logs")
data class ConversationLog(
    @PrimaryKey(autoGenerate = true)
    val logId: Long = 0,

    val timestamp: LocalDateTime,

    val role: String,  // "user" o "assistant"

    val content: String,

    // ID del evento asociado (opcional)
    val eventId: String? = null,

// ID del thread/chat al que pertenece este mensaje
    @ColumnInfo(defaultValue = "general")
    val threadId: String = "general",

    // Estado del mensaje: "UNREAD", "READ", "ACTED_UPON"
    val messageStatus: String = "UNREAD"

)
