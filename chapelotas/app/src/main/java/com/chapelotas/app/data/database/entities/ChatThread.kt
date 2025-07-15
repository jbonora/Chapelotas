package com.chapelotas.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Representa un "chat" o conversación individual
 * Puede ser de un evento específico o el chat general con Chapelotas
 */
@Entity(tableName = "chat_threads")
data class ChatThread(
    @PrimaryKey
    val threadId: String, // Formato: "event_{eventId}" o "general" o "daily_summary_{date}"

    // ID del evento asociado (null para chats generales)
    val eventId: String? = null,

    // Título que se muestra en la lista de chats
    val title: String,

    // Último mensaje para preview
    val lastMessage: String = "",

    // Hora del último mensaje (para ordenar)
    val lastMessageTime: LocalDateTime = LocalDateTime.now(ZoneId.systemDefault()),

    // Cantidad de mensajes sin leer
    val unreadCount: Int = 0,

    // Estado del thread: "ACTIVE", "COMPLETED", "MISSED", "ARCHIVED"
    val status: String = "ACTIVE",

    // Tipo: "EVENT", "GENERAL", "DAILY_SUMMARY"
    val threadType: String,

    // Para eventos: hora del evento (para código de colores)
    val eventTime: LocalDateTime? = null,

    // Fecha de creación del thread
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneId.systemDefault())
)