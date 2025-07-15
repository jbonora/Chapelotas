package com.chapelotas.app.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chapelotas.app.data.database.ChapelotasDatabase
import com.chapelotas.app.data.database.entities.ChatThread
import com.chapelotas.app.data.database.entities.ConversationLog
import com.chapelotas.app.data.database.entities.EventResolutionStatus
import com.chapelotas.app.domain.repositories.AIRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val database: ChapelotasDatabase,
    private val aiRepository: AIRepository
) : ViewModel() {

    private val threadId: String = savedStateHandle.get<String>("threadId") ?: "general"

    // Thread actual
    val thread: StateFlow<ChatThread?> = database.chatThreadDao()
        .observeThread(threadId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Mensajes del thread
    val messages: StateFlow<List<ConversationLog>> = database.conversationLogDao()
        .observeThreadMessages(threadId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Estado del input del usuario
    private val _userInput = MutableStateFlow("")
    val userInput: StateFlow<String> = _userInput.asStateFlow()

    // Estado de "escribiendo..."
    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    init {
        // Marcar mensajes como leídos al abrir el chat
        markMessagesAsRead()
    }

    private fun markMessagesAsRead() {
        viewModelScope.launch {
            database.conversationLogDao().markThreadMessagesAsRead(threadId)
            database.chatThreadDao().markAsRead(threadId)
        }
    }

    fun updateUserInput(input: String) {
        _userInput.value = input
    }

    fun sendMessage() {
        val message = _userInput.value.trim()
        if (message.isEmpty()) return

        viewModelScope.launch {
            // Limpiar input inmediatamente
            _userInput.value = ""

            // Guardar mensaje del usuario
            val userMessage = ConversationLog(
                timestamp = LocalDateTime.now(ZoneId.systemDefault()),
                role = "user",
                content = message,
                eventId = thread.value?.eventId,
                threadId = threadId
            )
            database.conversationLogDao().insert(userMessage)

            // Actualizar último mensaje del thread
            database.chatThreadDao().updateLastMessage(
                threadId = threadId,
                message = message,
                time = LocalDateTime.now(ZoneId.systemDefault())
            )

            // Mostrar indicador de "escribiendo..."
            _isTyping.value = true

            // Generar respuesta con AI
            generateAIResponse(message)
        }
    }

    private suspend fun generateAIResponse(userMessage: String) {
        try {
            val currentThread = thread.value
            val dayPlan = database.dayPlanDao().getTodayPlan()
            val isSarcastic = dayPlan?.sarcasticMode ?: true

            // Contexto para la AI
            val context = when (currentThread?.threadType) {
                "EVENT" -> {
                    val event = currentThread.eventId?.let {
                        database.eventPlanDao().getEvent(it)
                    }
                    """
                    Sos Chapelotas, secretaria ${if (isSarcastic) "sarcástica argentina" else "profesional"}.
                    Estás respondiendo sobre el evento: ${event?.title ?: "Evento"}
                    Hora: ${event?.startTime?.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}
                    ${event?.location?.let { "Lugar: $it" } ?: ""}
                    El usuario dice: "$userMessage"
                    
                    Responde de forma CORTA y directa (máximo 2-3 líneas).
                    ${if (isSarcastic) "Podés ser sarcástico pero útil." else "Sé profesional y amable."}
                    """
                }
                else -> {
                    """
                    Sos Chapelotas, secretaria ${if (isSarcastic) "sarcástica argentina" else "profesional"}.
                    El usuario dice: "$userMessage"
                    
                    Responde de forma CORTA y directa (máximo 2-3 líneas).
                    ${if (isSarcastic) "Podés ser sarcástico pero simpático." else "Sé profesional y amable."}
                    """
                }
            }

            val response = aiRepository.callOpenAIForText(context, temperature = 0.7)

            // Guardar respuesta de la AI
            val aiMessage = ConversationLog(
                timestamp = LocalDateTime.now(ZoneId.systemDefault()),
                role = "assistant",
                content = response,
                eventId = currentThread?.eventId,
                threadId = threadId
            )
            database.conversationLogDao().insert(aiMessage)

            // Actualizar último mensaje del thread
            database.chatThreadDao().updateLastMessage(
                threadId = threadId,
                message = response,
                time = LocalDateTime.now(ZoneId.systemDefault())
            )

            // Procesar comandos especiales si es un evento
            if (currentThread?.threadType == "EVENT" && currentThread.eventId != null) {
                processEventCommands(userMessage, currentThread.eventId)
            }

        } catch (e: Exception) {
            // En caso de error, mostrar mensaje genérico
            val errorMessage = ConversationLog(
                timestamp = LocalDateTime.now(ZoneId.systemDefault()),
                role = "assistant",
                content = "Ups, algo salió mal. ¿Podés repetir?",
                threadId = threadId
            )
            database.conversationLogDao().insert(errorMessage)
        } finally {
            _isTyping.value = false
        }
    }

    private suspend fun processEventCommands(message: String, eventId: String) {
        val lowerMessage = message.lowercase()

        when {
            lowerMessage.contains("listo") ||
                    lowerMessage.contains("hecho") ||
                    lowerMessage.contains("completado") -> {
                // Marcar evento como completado
                database.eventPlanDao().updateResolutionStatus(
                    eventId,
                    EventResolutionStatus.COMPLETED
                )
                database.chatThreadDao().updateStatus(threadId, "COMPLETED")
            }

            lowerMessage.contains("cancelar") ||
                    lowerMessage.contains("no voy") -> {
                // Marcar evento como cancelado
                database.eventPlanDao().updateResolutionStatus(
                    eventId,
                    EventResolutionStatus.CANCELLED
                )
                database.chatThreadDao().updateStatus(threadId, "CANCELLED")
            }
        }
    }

    fun markEventAsDone() {
        viewModelScope.launch {
            thread.value?.eventId?.let { eventId ->
                database.eventPlanDao().updateResolutionStatus(
                    eventId,
                    EventResolutionStatus.COMPLETED
                )
                database.chatThreadDao().updateStatus(threadId, "COMPLETED")

                // Agregar mensaje de confirmación
                val message = ConversationLog(
                    timestamp = LocalDateTime.now(ZoneId.systemDefault()),
                    role = "system",
                    content = "✅ Evento marcado como completado",
                    eventId = eventId,
                    threadId = threadId
                )
                database.conversationLogDao().insert(message)
            }
        }
    }

    fun snoozeEvent(minutes: Int = 15) {
        viewModelScope.launch {
            thread.value?.eventId?.let { eventId ->
                // Aquí deberías implementar la lógica de snooze
                // Por ahora solo agregamos un mensaje
                val message = ConversationLog(
                    timestamp = LocalDateTime.now(ZoneId.systemDefault()),
                    role = "system",
                    content = "⏰ Te recordaré en $minutes minutos",
                    eventId = eventId,
                    threadId = threadId
                )
                database.conversationLogDao().insert(message)
            }
        }
    }
}