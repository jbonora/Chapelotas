package com.chapelotas.app.domain.usecases

import android.util.Log
import com.chapelotas.app.data.database.ChapelotasDatabase
import com.chapelotas.app.data.database.entities.ChatThread
import java.time.LocalDate
import java.time.ZoneId
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
    companion object {
        private const val TAG = "InitChatThreads"
    }

    suspend operator fun invoke() {
        try {
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
                Log.d(TAG, "Thread general creado")
            }

            // 2. Crear threads para eventos de hoy sin thread
            val todayEvents = database.eventPlanDao().getEventsByDate(LocalDate.now())
            var threadsCreated = 0

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
                                event.startTime.isBefore(java.time.LocalDateTime.now(ZoneId.systemDefault())) -> "MISSED"
                                else -> "ACTIVE"
                            }
                        )
                    )
                    threadsCreated++

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

            if (threadsCreated > 0) {
                Log.d(TAG, "Creados $threadsCreated threads para eventos")
            }

            // 3. Asignar mensajes huérfanos al thread general
            database.conversationLogDao().assignOrphanMessagesToGeneral()

            // 4. Actualizar último mensaje del thread general si es necesario
            val lastGeneralMessage = database.conversationLogDao().getLastMessageForThread("general")
            if (lastGeneralMessage != null) {
                database.chatThreadDao().updateLastMessage(
                    "general",
                    lastGeneralMessage.content.take(100),
                    lastGeneralMessage.timestamp
                )
            }

            Log.d(TAG, "Inicialización de threads completada")

        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando threads", e)
        }
    }
}