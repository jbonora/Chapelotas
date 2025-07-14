package com.chapelotas.app.data.ai

import com.google.gson.annotations.SerializedName
import retrofit2.http.*

interface OpenAIApi {
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse
}

data class ChatCompletionRequest(
    val model: String = "gpt-3.5-turbo",
    val messages: List<Message>,
    val temperature: Double = 0.7,
    val response_format: ResponseFormat? = null
)

data class Message(
    val role: String, // "system", "user", "assistant"
    val content: String
)

data class ResponseFormat(
    val type: String = "json_object"
)

data class ChatCompletionResponse(
    val id: String,
    val choices: List<Choice>,
    val usage: Usage
)

data class Choice(
    val index: Int,
    val message: Message,
    val finish_reason: String
)

data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

// --- ESTRUCTURA DE RESPUESTA PARA EL NUEVO MODO BATCH ---

// El objeto principal que esperamos recibir de la IA
data class AIPlanBatchResponse(
    val plans: List<AIEventPlan>
)

// Representa el plan para un único evento dentro del batch
data class AIEventPlan(
    @SerializedName("eventId")
    val eventId: Long,

    @SerializedName("isCritical")
    val isCritical: Boolean,

    @SerializedName("conflict")
    val conflict: AIConflictInfo?,

    @SerializedName("notifications")
    val notifications: List<AINotification>
)

// Representa la información de un conflicto detectado por la IA
data class AIConflictInfo(
    @SerializedName("hasConflict")
    val hasConflict: Boolean,

    @SerializedName("conflictingEventId")
    val conflictingEventId: Long?,

    @SerializedName("conflictType")
    val conflictType: String? // "OVERLAP" | "TOO_CLOSE"
)

// Representa una notificación individual dentro del plan de un evento
data class AINotification(
    @SerializedName("minutesBefore")
    val minutesBefore: Int,

    @SerializedName("message")
    val message: String,

    @SerializedName("priority")
    val priority: String, // "LOW", "NORMAL", "HIGH", "CRITICAL"

    @SerializedName("type")
    val type: String // "EVENT_REMINDER", "PREPARATION_TIP"
)


// --- ESTRUCTURAS ANTIGUAS (Se mantienen por ahora por si hay que revertir) ---
data class AIPlanResponse(
    val insights: String,
    val suggestedFocus: String,
    val notifications: List<NotificationPlan>
)

data class NotificationPlan(
    val eventId: Long,
    val minutesBeforeEvent: Int,
    val message: String,
    val priority: String,
    val type: String,
    val rationale: String
)