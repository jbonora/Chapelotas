package com.chapelotas.app.domain.usecases

import com.chapelotas.app.data.database.ChapelotasDatabase
import com.chapelotas.app.data.database.entities.ChatThread
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Inicializa los chat threads para eventos existentes
 * Se ejecuta al inicio para asegurar que cada evento tenga su thread
 */
@Singleton
class InitializeChatThreadsUseCase @Inject constructor(
    private val database: ChapelotasDatabase
) {
    suspend operator fun invoke() {
        // 1. Crear thread general si no existe
        val generalThread = database.chatThreadDao().getThread("general")
        if (generalThread == null) {
            database.chatThreadDao().insertOrUpdate(
                ChatThread(
                    threadId = "general",
                    title = "🐵 Chapelotas",
                    threadType = "GENERAL",
                    lastMessage = "¡Hola! Soy tu secretaria personal."
                )
            )
        }

        // 2. Crear threads para eventos de hoy sin thread
        val todayEvents = database.eventPlanDao().getEventsByDate(LocalDate.now())

        todayEvents.forEach { event ->
            val threadId = "event_${event.eventId}"
            val existingThread = database.chatThreadDao().getThread(threadId)

            if (existingThread == null) {
                // Crear nuevo thread para el evento
                database.chatThreadDao().insertOrUpdate(
                    ChatThread(
                        threadId = threadId,
                        eventId = event.eventId,
                        title = event.title,
                        threadType = "EVENT",
                        eventTime = event.startTime,
                        status = when {
                            event.resolutionStatus.name == "COMPLETED" -> "COMPLETED"
                            event.startTime.isBefore(java.time.LocalDateTime.now()) -> "MISSED"
                            else -> "ACTIVE"
                        }
                    )
                )

                // Migrar mensajes existentes al thread
                database.conversationLogDao().updateThreadIdForEvent(event.eventId, threadId)

                // Actualizar último mensaje del thread
                val lastMessage = database.conversationLogDao().getLastMessageForThread(threadId)
                if (lastMessage != null) {
                    database.chatThreadDao().updateLastMessage(
                        threadId,
                        lastMessage.content.take(100), // Preview de 100 chars
                        lastMessage.timestamp
                    )
                }
            }
        }

        // 3. Asignar mensajes huérfanos al thread general
        database.conversationLogDao().getRecentHistory(100).forEach { log ->
            if (log.threadId == null) {
                val newThreadId = if (log.eventId != null) {
                    "event_${log.eventId}"
                } else {
                    "general"
                }

                // Actualizar el mensaje con su threadId
                // Nota: necesitarías agregar este método al DAO o hacerlo de otra forma
            }
        }
    }
}