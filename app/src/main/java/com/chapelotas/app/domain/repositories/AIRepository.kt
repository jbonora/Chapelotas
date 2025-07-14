package com.chapelotas.app.domain.repositories

import com.chapelotas.app.data.ai.AIEventPlan
import com.chapelotas.app.domain.entities.AIPlan
import com.chapelotas.app.domain.entities.CalendarEvent
import com.chapelotas.app.domain.entities.PlannedNotification

/**
 * Repositorio para interactuar con el servicio de IA
 */
interface AIRepository {

    // --- NUEVO MÉTODO PARA ANÁLISIS BATCH ---
    /**
     * Analiza una lista de eventos del día y devuelve un plan completo.
     * @param events La lista de eventos a analizar.
     * @return Un mapa donde la clave es el ID del evento y el valor es el plan de la IA.
     */
    suspend fun createDailyPlanBatch(events: List<CalendarEvent>): Map<Long, AIEventPlan>


    @Deprecated("Usar createDailyPlanBatch para un análisis contextual completo.")
    suspend fun createIntelligentEventPlan(event: CalendarEvent): List<PlannedNotification>


    suspend fun generateCommunicationPlan(
        events: List<CalendarEvent>,
        userContext: String? = null
    ): AIPlan

    suspend fun callOpenAIForJSON(
        prompt: String,
        temperature: Double = 0.3
    ): String

    suspend fun callOpenAIForText(
        prompt: String,
        temperature: Double = 0.7
    ): String

    suspend fun generateNotificationMessage(
        event: CalendarEvent,
        messageType: String,
        isSarcastic: Boolean = false,
        additionalContext: String? = null
    ): String

    suspend fun generateDailySummary(
        todayEvents: List<CalendarEvent>,
        isSarcastic: Boolean = false
    ): String

    suspend fun generateTomorrowSummary(
        tomorrowEvents: List<CalendarEvent>,
        todayContext: String? = null,
        isSarcastic: Boolean = false
    ): String

    suspend fun suggestCriticalEvents(
        event: CalendarEvent,
        userHistory: List<CalendarEvent>? = null
    ): Boolean

    suspend fun generatePreparationTips(
        event: CalendarEvent,
        weatherInfo: String? = null,
        trafficInfo: String? = null
    ): String?

    suspend fun testConnection(): Boolean

    suspend fun getCurrentModel(): String
}