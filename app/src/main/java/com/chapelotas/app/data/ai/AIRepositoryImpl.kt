package com.chapelotas.app.data.ai

import android.util.Log
import com.chapelotas.app.BuildConfig
import com.chapelotas.app.domain.entities.*
import com.chapelotas.app.domain.repositories.AIRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación del repositorio de IA
 * Ahora con IA REAL! 🚀
 */
@Singleton
class AIRepositoryImpl @Inject constructor() : AIRepository {

    private val apiKey = BuildConfig.OPENAI_API_KEY
    private val gson = GsonBuilder().create()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openai.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(
            OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        )
        .build()

    private val openAIApi = retrofit.create(OpenAIApi::class.java)

    init {
        if (apiKey.isEmpty()) {
            Log.w("AIRepository", "API Key no configurada - usando modo simulado")
        } else {
            Log.d("AIRepository", "API Key configurada correctamente ✓")
        }
    }

    override suspend fun generateCommunicationPlan(
        events: List<CalendarEvent>,
        userContext: String?
    ): AIPlan {
        if (apiKey.isEmpty()) {
            return generateSimulatedPlan(events)
        }

        val systemPrompt = """
        Sos Chapelotas, una secretaria personal virtual argentina con personalidad insistente pero servicial.
        Tu trabajo es crear un plan de comunicación para recordarle al usuario sus eventos del día.
        
        Debes responder ÚNICAMENTE en formato JSON con la siguiente estructura exacta:
        {
            "insights": "Análisis general del día en 1-2 oraciones",
            "suggestedFocus": "Qué debería priorizar el usuario hoy",
            "notifications": [
                {
                    "eventId": 123,
                    "minutesBeforeEvent": 15,
                    "message": "Mensaje de recordatorio creativo y personalizado",
                    "priority": "HIGH",
                    "type": "REMINDER",
                    "rationale": "Por qué este timing"
                }
            ]
        }
        
        Reglas importantes:
        - Eventos críticos necesitan al menos 2 recordatorios (30 y 15 minutos antes)
        - Sé creativo con los mensajes, no genérico
        - Usá expresiones argentinas naturales
        - Si hay eventos seguidos, avisá sobre el poco tiempo entre ellos
        - Priority puede ser: HIGH, NORMAL, LOW
        - Type puede ser: REMINDER, CRITICAL, PREPARATION
        """.trimIndent()

        val userPrompt = buildString {
            append("Eventos del día de hoy:\n")
            events.forEach { event ->
                append("- ID: ${event.id}, ")
                append("Hora: ${event.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))}, ")
                append("Título: ${event.title}")
                event.location?.let { append(", Lugar: $it") }
                append(", Duración: ${event.durationInMinutes} minutos")
                if (event.isCritical) append(" [EVENTO CRÍTICO - MUY IMPORTANTE]")
                append("\n")
            }
            userContext?.let { append("\nContexto adicional del usuario: $it") }
        }

        try {
            Log.d("AIRepository", "Generando plan de comunicación...")

            val response = openAIApi.createChatCompletion(
                authorization = "Bearer $apiKey",
                request = ChatCompletionRequest(
                    model = "gpt-3.5-turbo",
                    messages = listOf(
                        Message("system", systemPrompt),
                        Message("user", userPrompt)
                    ),
                    temperature = 0.8
                )
            )

            val jsonResponse = response.choices.firstOrNull()?.message?.content
                ?: throw Exception("No response from AI")

            Log.d("AIRepository", "Respuesta IA: $jsonResponse")

            return parseAIPlanFromJson(jsonResponse, events)

        } catch (e: Exception) {
            Log.e("AIRepository", "Error al generar plan", e)
            return generateSimulatedPlan(events)
        }
    }

    override suspend fun generateNotificationMessage(
        event: CalendarEvent,
        messageType: String,
        isSarcastic: Boolean,
        additionalContext: String?
    ): String {
        if (apiKey.isEmpty()) {
            return generateSimulatedMessage(event, messageType, isSarcastic)
        }

        val personality = if (isSarcastic) {
            """Sos Chapelotas en modo sarcástico. Usá humor ácido argentino e ironía, 
            pero siempre asegurándote de que el mensaje sea claro sobre el evento.
            Ejemplos de tu estilo:
            - "Mirá, yo te aviso por tercera vez. Si llegás tarde, no digas que no tenías un asistente increíble."
            - "¿En serio necesitás que te recuerde esto? Bueno, acá estoy, tu memoria externa personal."
            - "Dale, seguí scrolleando Instagram. Total tu reunión es en 15 minutos nomás..."
            """
        } else {
            """Sos Chapelotas en modo amigable. Sé insistente pero cálido, servicial y profesional.
            Usá un tono argentino natural pero cordial.
            Ejemplos:
            - "Che, perdón por insistir, pero tu reunión empieza en 20 minutos"
            - "¡Ey! No te olvides que en 15 minutos tenés [evento]"
            - "Último aviso amistoso: [evento] está por empezar"
            """
        }

        val prompt = """
        $personality
        
        Generá UN SOLO mensaje corto (máximo 2 líneas) de tipo $messageType para:
        - Evento: ${event.title}
        - Hora: ${event.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))}
        - Lugar: ${event.location ?: "No especificado"}
        - Tiempo hasta el evento: ${event.timeUntilStartDescription()}
        ${if (event.isCritical) "- 🚨 ESTE ES UN EVENTO CRÍTICO/IMPORTANTE" else ""}
        
        El mensaje debe ser directo, memorable y mencionar el título del evento.
        NO uses comillas en tu respuesta. Solo el mensaje directo.
        """.trimIndent()

        return try {
            val response = openAIApi.createChatCompletion(
                authorization = "Bearer $apiKey",
                request = ChatCompletionRequest(
                    model = "gpt-3.5-turbo",
                    messages = listOf(Message("user", prompt)),
                    temperature = 0.9
                )
            )

            response.choices.firstOrNull()?.message?.content?.trim()
                ?: generateSimulatedMessage(event, messageType, isSarcastic)

        } catch (e: Exception) {
            Log.e("AIRepository", "Error generando mensaje", e)
            generateSimulatedMessage(event, messageType, isSarcastic)
        }
    }

    override suspend fun generateDailySummary(
        todayEvents: List<CalendarEvent>,
        isSarcastic: Boolean
    ): String {
        if (apiKey.isEmpty()) {
            return generateSimulatedDailySummary(todayEvents, isSarcastic)
        }

        val currentTime = LocalDateTime.now()
        val timeContext = when (currentTime.hour) {
            in 0..11 -> "Es la mañana"
            in 12..17 -> "Es la tarde"
            else -> "Es la noche"
        }

        val personality = if (isSarcastic) {
            """Usá humor sarcástico argentino pero asegurate que la info sea clara.
            Podés ser irónico sobre lo ocupado que está o lo desorganizado que parece."""
        } else {
            """Sé amigable, motivador y profesional con un toque argentino cálido."""
        }

        val prompt = """
            Como Chapelotas, generá un resumen para el usuario.
            HORA ACTUAL: ${currentTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - $timeContext
            $personality
            
            Eventos de hoy:
            ${if (todayEvents.isEmpty()) {
            "No hay eventos programados"
        } else {
            val pastEvents = todayEvents.filter { it.startTime.isBefore(currentTime) }
            val futureEvents = todayEvents.filter { it.startTime.isAfter(currentTime) }
            val currentEvent = todayEvents.find {
                it.startTime.isBefore(currentTime) && it.endTime.isAfter(currentTime)
            }

            buildString {
                if (currentEvent != null) {
                    append("AHORA MISMO: ${currentEvent.title}\n")
                }

                if (pastEvents.isNotEmpty()) {
                    append("\nEventos que ya pasaron (${pastEvents.size}):\n")
                    pastEvents.forEach { event ->
                        append("- ${event.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} ${event.title} ✓\n")
                    }
                }

                if (futureEvents.isNotEmpty()) {
                    append("\nEventos próximos (${futureEvents.size}):\n")
                    futureEvents.forEach { event ->
                        append("- ${event.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} ${event.title}")
                        if (event.isCritical) append(" [CRÍTICO]")
                        append("\n")
                    }
                }
            }
        }}
            
            IMPORTANTE:
            - Si es tarde/noche, NO digas "buen día" o "arrancás con"
            - Si es tarde y ya pasaron muchos eventos, enfocate en lo que queda
            - Si no quedan eventos, mencioná que el día ya está casi terminado
            - Ajustá el saludo según la hora (buenos días/tardes/noches)
            - Máximo 4 líneas
            - Sé natural con el contexto temporal
            """.trimIndent()

        return callOpenAIForText(prompt, temperature = 0.8)
    }

    override suspend fun generateTomorrowSummary(
        tomorrowEvents: List<CalendarEvent>,
        todayContext: String?,
        isSarcastic: Boolean
    ): String {
        if (apiKey.isEmpty()) {
            return generateSimulatedTomorrowSummary(tomorrowEvents, isSarcastic)
        }

        val personality = if (isSarcastic) {
            "Modo sarcástico: podés bromear sobre su agenda o hacer comentarios irónicos."
        } else {
            "Modo amigable: sé alentador y ayudá a prepararse mentalmente."
        }

        val prompt = """
        Como Chapelotas, generá un resumen de mañana.
        $personality
        
        Eventos de mañana:
        ${if (tomorrowEvents.isEmpty()) {
            "No hay eventos programados"
        } else {
            tomorrowEvents.joinToString("\n") { event ->
                "- ${event.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} ${event.title}"
            }
        }}
        
        Creá un mensaje corto (3 líneas) que:
        - Prepare mentalmente al usuario para mañana
        - Mencione la hora del primer evento si hay
        - Sugiera preparativos si es necesario
        - Use tono argentino natural
        """.trimIndent()

        return callOpenAIForText(prompt)
    }

    override suspend fun suggestCriticalEvents(
        event: CalendarEvent,
        userHistory: List<CalendarEvent>?
    ): Boolean {
        // Por ahora seguimos con la lógica simple
        val criticalKeywords = listOf(
            "importante", "urgente", "crítico", "deadline",
            "entrevista", "examen", "médico", "doctor", "dentista",
            "vuelo", "avión", "presentación", "reunión con jefe",
            "final", "parcial", "entrega"
        )

        return criticalKeywords.any { keyword ->
            event.title.contains(keyword, ignoreCase = true) ||
                    event.description?.contains(keyword, ignoreCase = true) == true
        }
    }

    override suspend fun generatePreparationTips(
        event: CalendarEvent,
        weatherInfo: String?,
        trafficInfo: String?
    ): String? {
        if (apiKey.isEmpty()) {
            return generateSimulatedTips(event)
        }

        val prompt = """
        Como Chapelotas, generá UN tip de preparación muy breve para:
        Evento: ${event.title}
        Lugar: ${event.location ?: "No especificado"}
        
        El tip debe ser:
        - Una sola oración corta
        - Práctico y específico
        - En tono argentino casual
        
        Ejemplos: "Llevá paraguas, se largó a llover" o "Salí 10 min antes, hay piquete en Callao"
        """.trimIndent()

        return try {
            callOpenAIForText(prompt, temperature = 0.7).takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            generateSimulatedTips(event)
        }
    }

    override suspend fun testConnection(): Boolean {
        if (apiKey.isEmpty()) return false

        return try {
            val response = openAIApi.createChatCompletion(
                authorization = "Bearer $apiKey",
                request = ChatCompletionRequest(
                    model = "gpt-3.5-turbo",
                    messages = listOf(
                        Message("user", "Di 'OK' si funcionás")
                    ),
                    temperature = 0.1
                )
            )
            response.choices.isNotEmpty()
        } catch (e: Exception) {
            Log.e("AIRepository", "Error testing connection", e)
            false
        }
    }

    override suspend fun getCurrentModel(): String {
        return if (apiKey.isNotEmpty()) "GPT-3.5 Turbo (OpenAI)" else "Modo Simulado"
    }

    // Funciones auxiliares
    // Agregar estos métodos en AIRepositoryImpl

    override suspend fun callOpenAIForJSON(
        prompt: String,
        temperature: Double
    ): String {
        if (apiKey.isEmpty()) {
            throw Exception("No API key configured")
        }

        return try {
            val response = openAIApi.createChatCompletion(
                authorization = "Bearer $apiKey",
                request = ChatCompletionRequest(
                    model = "gpt-3.5-turbo",
                    messages = listOf(
                        Message("system", "Responde SOLO con JSON válido, sin explicaciones adicionales"),
                        Message("user", prompt)
                    ),
                    temperature = temperature
                )
            )

            response.choices.firstOrNull()?.message?.content?.trim()
                ?: throw Exception("No response from AI")

        } catch (e: Exception) {
            Log.e("AIRepository", "Error calling OpenAI for JSON", e)
            throw e
        }
    }



    override suspend fun callOpenAIForText(
        prompt: String,
        temperature: Double
    ): String {
        return try {
            val response = openAIApi.createChatCompletion(
                authorization = "Bearer $apiKey",
                request = ChatCompletionRequest(
                    model = "gpt-3.5-turbo",
                    messages = listOf(Message("user", prompt)),
                    temperature = temperature
                )
            )

            response.choices.firstOrNull()?.message?.content?.trim()
                ?: "No pude generar una respuesta"

        } catch (e: Exception) {
            Log.e("AIRepository", "Error calling OpenAI", e)
            "Error de conexión con Chapelotas IA"
        }
    }

    private fun parseAIPlanFromJson(
        json: String,
        events: List<CalendarEvent>
    ): AIPlan {
        return try {
            val planResponse = gson.fromJson(json, AIPlanResponse::class.java)

            val notifications = planResponse.notifications.mapNotNull { notif ->
                val event = events.find { it.id == notif.eventId } ?: return@mapNotNull null

                PlannedNotification(
                    eventId = notif.eventId,
                    suggestedTime = event.startTime.minusMinutes(notif.minutesBeforeEvent.toLong()),
                    suggestedMessage = notif.message,
                    priority = try {
                        NotificationPriority.valueOf(notif.priority)
                    } catch (e: Exception) {
                        NotificationPriority.NORMAL
                    },
                    type = when (notif.type) {
                        "CRITICAL" -> NotificationType.CRITICAL_ALERT
                        "PREPARATION" -> NotificationType.PREPARATION_TIP
                        else -> NotificationType.EVENT_REMINDER
                    },
                    rationale = notif.rationale
                )
            }

            AIPlan(
                id = UUID.randomUUID().toString(),
                generatedAt = LocalDateTime.now(),
                eventsAnalyzed = events.map { it.id },
                notifications = notifications,
                aiInsights = planResponse.insights,
                suggestedFocus = planResponse.suggestedFocus
            )
        } catch (e: Exception) {
            Log.e("AIRepository", "Error parsing AI response", e)
            generateSimulatedPlan(events)
        }
    }

    // Funciones de fallback (modo simulado)

    private fun generateSimulatedPlan(events: List<CalendarEvent>): AIPlan {
        val notifications = mutableListOf<PlannedNotification>()

        events.forEach { event ->
            // Notificación estándar 15 min antes
            notifications.add(
                PlannedNotification(
                    eventId = event.id,
                    suggestedTime = event.startTime.minusMinutes(15),
                    suggestedMessage = "¡Ey! ${event.title} empieza en 15 minutos" +
                            if (event.location != null) " en ${event.location}" else "",
                    priority = if (event.isCritical) NotificationPriority.HIGH else NotificationPriority.NORMAL,
                    type = NotificationType.EVENT_REMINDER,
                    rationale = "Recordatorio estándar"
                )
            )

            // Si es crítico, agregar extra
            if (event.isCritical) {
                notifications.add(
                    PlannedNotification(
                        eventId = event.id,
                        suggestedTime = event.startTime.minusMinutes(30),
                        suggestedMessage = "🚨 EVENTO CRÍTICO: ${event.title} en 30 minutos",
                        priority = NotificationPriority.CRITICAL,
                        type = NotificationType.CRITICAL_ALERT,
                        rationale = "Alerta extra para evento crítico"
                    )
                )
            }
        }

        return AIPlan(
            id = UUID.randomUUID().toString(),
            generatedAt = LocalDateTime.now(),
            eventsAnalyzed = events.map { it.id },
            notifications = notifications,
            aiInsights = "Modo simulado - Activá la IA para mensajes personalizados",
            suggestedFocus = if (events.any { it.isCritical }) {
                "Concentrate en los eventos críticos"
            } else {
                "Día normal, mantené el ritmo"
            }
        )
    }

    private fun generateSimulatedMessage(
        event: CalendarEvent,
        messageType: String,
        isSarcastic: Boolean
    ): String {
        return when (messageType) {
            "CRITICAL_SARCASTIC" -> "¿En serio necesitás que te recuerde ${event.title}? Dale..."
            "CRITICAL_URGENT" -> "⚠️ URGENTE: ${event.title} está por empezar!"
            else -> if (isSarcastic) {
                "Che genio, ${event.title} en ${event.minutesUntilStart()} min. ¿Te movés?"
            } else {
                "Recordatorio: ${event.title} empieza en ${event.minutesUntilStart()} minutos"
            }
        }
    }

    private fun generateSimulatedDailySummary(
        events: List<CalendarEvent>,
        isSarcastic: Boolean
    ): String {
        if (events.isEmpty()) {
            return if (isSarcastic) {
                "¡Mirá vos! No tenés nada hoy. ¿Vacaciones o te olvidaste de anotar todo?"
            } else {
                "¡Buen día! Hoy tenés el día libre. ¡Aprovechalo!"
            }
        }

        return if (isSarcastic) {
            "Tenés ${events.size} eventos hoy. Sí, ${events.size}. ¿Necesitás que te los lea uno por uno o ya captaste?"
        } else {
            "Buenos días! Hoy tenés ${events.size} eventos programados. ¡Vamos que se puede!"
        }
    }

    private fun generateSimulatedTomorrowSummary(
        events: List<CalendarEvent>,
        isSarcastic: Boolean
    ): String {
        if (events.isEmpty()) {
            return if (isSarcastic) {
                "Mañana no tenés nada. Disfrutá mientras dure."
            } else {
                "Mañana tenés el día libre. Descansá bien!"
            }
        }

        val firstEvent = events.minByOrNull { it.startTime }
        return if (isSarcastic) {
            "Mañana arrancás a las ${firstEvent?.startTime?.format(DateTimeFormatter.ofPattern("HH:mm"))}. ¿Ponés alarma o confías en mí?"
        } else {
            "Mañana tu primer evento es a las ${firstEvent?.startTime?.format(DateTimeFormatter.ofPattern("HH:mm"))}. ¡Que descanses!"
        }
    }

    private fun generateSimulatedTips(event: CalendarEvent): String? {
        return when {
            event.title.contains("reunión", ignoreCase = true) ->
                "Llevá notebook y cargador"
            event.title.contains("médico", ignoreCase = true) ->
                "Llevá estudios previos"
            event.location?.contains("aeropuerto", ignoreCase = true) == true ->
                "Check-in online y llegá 2hs antes"
            else -> null
        }
    }
}