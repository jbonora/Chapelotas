package com.chapelotas.app.data.ai

import retrofit2.http.*

interface OpenAIApi {
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse
}

data class ChatCompletionRequest(
    val model: String = "gpt-3.5-turbo", // Usando 3.5 que es más barato
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

// Data classes para parsear respuestas JSON de la IA
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