package com.chapelotas.app.domain.repositories

import com.chapelotas.app.domain.entities.AIPlan
import com.chapelotas.app.domain.entities.CalendarEvent

/**
 * Repositorio para interactuar con el servicio de IA
 */
interface AIRepository {

    /**
     * Genera un plan de comunicación basado en los eventos del día
     * @param events Lista de eventos a analizar
     * @param userContext Contexto adicional del usuario (preferencias, historial, etc.)
     * @return Plan de notificaciones generado por la IA
     */
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


    /**
     * Genera un mensaje personalizado para una notificación
     * @param event Evento sobre el cual notificar
     * @param messageType Tipo de mensaje a generar
     * @param isSarcastic Si debe usar tono sarcástico
     * @param additionalContext Contexto adicional para personalizar el mensaje
     */
    suspend fun generateNotificationMessage(
        event: CalendarEvent,
        messageType: String,
        isSarcastic: Boolean = false,
        additionalContext: String? = null
    ): String

    /**
     * Genera el resumen del día
     * @param todayEvents Eventos de hoy
     * @param isSarcastic Si debe usar tono sarcástico
     */
    suspend fun generateDailySummary(
        todayEvents: List<CalendarEvent>,
        isSarcastic: Boolean = false
    ): String

    /**
     * Genera el resumen de mañana
     * @param tomorrowEvents Eventos de mañana
     * @param todayContext Contexto de lo que pasó hoy
     * @param isSarcastic Si debe usar tono sarcástico
     */
    suspend fun generateTomorrowSummary(
        tomorrowEvents: List<CalendarEvent>,
        todayContext: String? = null,
        isSarcastic: Boolean = false
    ): String

    /**
     * Analiza si un evento debería ser crítico basado en su contenido
     * @param event Evento a analizar
     * @param userHistory Historial de eventos críticos previos
     */
    suspend fun suggestCriticalEvents(
        event: CalendarEvent,
        userHistory: List<CalendarEvent>? = null
    ): Boolean

    /**
     * Genera un mensaje de preparación inteligente
     * Por ejemplo: "Acordate de llevar paraguas, hay 80% de lluvia"
     */
    suspend fun generatePreparationTips(
        event: CalendarEvent,
        weatherInfo: String? = null,
        trafficInfo: String? = null
    ): String?

    /**
     * Verifica la conectividad con el servicio de IA
     */
    suspend fun testConnection(): Boolean

    /**
     * Obtiene el modelo de IA actual en uso
     */
    suspend fun getCurrentModel(): String
}