package com.chapelotas.app.data.ai

import android.util.Log
import com.chapelotas.app.BuildConfig
import com.chapelotas.app.domain.entities.*
import com.chapelotas.app.domain.repositories.AIRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
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

@Singleton
class AIRepositoryImpl @Inject constructor() : AIRepository {

    private val apiKey = BuildConfig.OPENAI_API_KEY
    private val gson = GsonBuilder().setPrettyPrinting().create() // Usamos PrettyPrinting para logs legibles

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openai.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(
            OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
        )
        .build()

    private val openAIApi = retrofit.create(OpenAIApi::class.java)

    companion object {
        const val TAG = "Vigía-AI-Repo"
    }

    init {
        if (apiKey.isEmpty() || apiKey == "YOUR_API_KEY") {
            Log.w(TAG, "API Key no configurada - usando modo simulado")
        } else {
            Log.d(TAG, "API Key configurada correctamente ✓")
        }
    }

    override suspend fun createDailyPlanBatch(events: List<CalendarEvent>): Map<Long, AIEventPlan> {
        if (apiKey.isEmpty() || apiKey == "YOUR_API_KEY" || events.isEmpty()) {
            return emptyMap()
        }

        val systemPrompt = """
        ### ROL Y OBJETIVO ###
        Eres un asistente ejecutivo experto en planificación para una app. Tu tarea es analizar una lista completa de eventos de un día y devolver un plan de notificación detallado para CADA UNO de ellos, incluyendo la detección de conflictos.
        ### FORMATO DE RESPUESTA ###
        Debes responder ÚNICAMENTE con un objeto JSON que contenga una sola clave: "plans". El valor de "plans" debe ser un array de objetos JSON, uno por cada evento que te envié. No incluyas texto extra fuera de este JSON.
        La estructura de CADA objeto en el array "plans" debe ser:
        {
          "eventId": "ID_DEL_EVENTO_ORIGINAL",
          "isCritical": boolean,
          "conflict": { "hasConflict": boolean, "conflictingEventId": "ID_DEL_OTRO_EVENTO_EN_CONFLICTO", "conflictType": "OVERLAP | TOO_CLOSE" },
          "notifications": [ { "minutesBefore": integer, "message": "Mensaje corto y directo.", "priority": "NORMAL | HIGH | CRITICAL", "type": "EVENT_REMINDER | PREPARATION_TIP" } ]
        }
        ### REGLAS DE PLANIFICACIÓN ###
        1. Analiza la lista completa para tomar tus decisiones.
        2. Crítico: Marca `isCritical: true` si el título contiene "Examen", "Vuelo", "Jefe", "Final", "Entrevista", "Médico".
        3. Conflictos: Si dos eventos se superponen, marca `hasConflict: true` y `conflictType: "OVERLAP"`. Si hay menos de 15 min entre ellos, marca `hasConflict: true` y `conflictType: "TOO_CLOSE"`. Si no hay conflicto, `hasConflict` debe ser `false`.
        4. Notificaciones: Eventos críticos deben tener dos notificaciones `CRITICAL_ALERT`. Eventos con `location` deben tener un `PREPARATION_TIP`. Eventos normales, un `EVENT_REMINDER`.
        """.trimIndent()

        val eventsAsJson = gson.toJson(events.map { it.toDataForAI() })
        val userPrompt = """
        ### DATOS DEL DÍA (LISTA DE EVENTOS) ###
        $eventsAsJson
        """.trimIndent()

        Log.d(TAG, "---------- INICIO LLAMADO BATCH A IA ----------")
        Log.d(TAG, "===> PAYLOAD ENVIADO:\n$eventsAsJson")

        try {
            val request = ChatCompletionRequest(
                model = "gpt-4o",
                messages = listOf(Message("system", systemPrompt), Message("user", userPrompt)),
                temperature = 0.0,
                response_format = ResponseFormat(type = "json_object")
            )
            val response = openAIApi.createChatCompletion("Bearer $apiKey", request)
            val jsonString = response.choices.firstOrNull()?.message?.content

            if (jsonString.isNullOrBlank()) {
                Log.e(TAG, "<=== RESPUESTA RECIBIDA: La IA devolvió una respuesta vacía o nula.")
                return emptyMap()
            }

            Log.d(TAG, "<=== RESPUESTA CRUDA RECIBIDA:\n$jsonString")

            val responseType = object : TypeToken<AIPlanBatchResponse>() {}.type
            val batchResponse: AIPlanBatchResponse = gson.fromJson(jsonString, responseType)

            Log.d(TAG, "✅ Deserialización del JSON exitosa. Se recibieron ${batchResponse.plans.size} planes.")
            Log.d(TAG, "---------- FIN LLAMADO BATCH A IA ----------")

            return batchResponse.plans.associateBy { it.eventId }

        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "🛑 ERROR DE SINTAXIS JSON: La IA devolvió un JSON mal formado. No se puede procesar.", e)
            return emptyMap()
        }
        catch (e: Exception) {
            Log.e(TAG, "🛑 ERROR GENERAL en la llamada batch a la IA.", e)
            return emptyMap()
        }
    }

    private fun CalendarEvent.toDataForAI() = mapOf(
        "eventId" to this.id,
        "title" to this.title,
        "description" to this.description,
        "startTime" to this.startTime.toString(),
        "endTime" to this.endTime.toString(),
        "location" to this.location
    )

    @Deprecated("Usar createDailyPlanBatch para un análisis contextual completo.")
    override suspend fun createIntelligentEventPlan(event: CalendarEvent): List<PlannedNotification> {
        val plan = createDailyPlanBatch(listOf(event))
        val eventPlan = plan[event.id] ?: return generateSimulatedEventPlan(event)
        return eventPlan.notifications.map {
            PlannedNotification(
                eventId = event.id, suggestedTime = event.startTime.minusMinutes(it.minutesBefore.toLong()),
                suggestedMessage = it.message, priority = com.chapelotas.app.domain.entities.NotificationPriority.valueOf(it.priority),
                type = com.chapelotas.app.domain.entities.NotificationType.valueOf(it.type), rationale = "Planificado por IA (single)",
                hasBeenScheduled = false
            )
        }
    }

    override suspend fun generateDailySummary(todayEvents: List<CalendarEvent>, isSarcastic: Boolean): String {
        if (apiKey.isEmpty() || apiKey == "YOUR_API_KEY") {
            return generateSimulatedDailySummary(todayEvents, isSarcastic)
        }
        val prompt = if (isSarcastic) buildSarcasticSummaryPrompt(todayEvents) else buildProfessionalSummaryPrompt(todayEvents)
        return callOpenAIForText(prompt, temperature = if (isSarcastic) 0.7 else 0.2)
    }

    private fun buildProfessionalSummaryPrompt(events: List<CalendarEvent>): String {
        val eventsText = if (events.isEmpty()) "Ninguna" else events.joinToString("\n- ") { "${it.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} ${it.title}" }
        return """
        ### ROL Y OBJETIVO ###
        Tu nombre es Chapelotas. Eres la secretaria ejecutiva, proactiva y atenta de un usuario. Tu misión es comunicar el resumen del día basándote ESTRICTAMENTE en los datos de calendario que te proporciono, para ayudarlo a estar perfectamente organizado.
        ### PERSONALIDAD ###
        Tu tono es amable, profesional y servicial. Tu objetivo es informar con claridad y eficiencia.
        ### INSTRUCCIONES ###
        1. Saludo: Empieza con un saludo que se ajuste a la hora del día en Argentina.
        2. MANDATORIO: ENUMERAR TAREAS: Debes comunicar de forma clara TODAS las tareas pendientes que te paso en la sección de DATOS. Puedes hacerlo en una lista o en un párrafo, como te parezca más natural, pero NO puedes omitir ninguna.
        3. REGLA DE ORO (NO INVENTAR): Está terminantemente prohibido inventar o sugerir actividades que no estén en la lista de DATOS. Cíñete exclusivamente a la información del calendario que te entrego.
        4. MANEJO DE DÍA VACÍO: Si en la sección de DATOS te entrego "Ninguna" tarea, tienes PROHIBIDO mencionar la palabra "tareas" o "actividades". En su lugar, simplemente desea un buen día o una buena noche de forma cordial.
        ### DATOS ###
        Hora Actual: ${LocalDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))}
        Tareas Pendientes:
        - ${eventsText}
        """.trimIndent()
    }

    private fun buildSarcasticSummaryPrompt(events: List<CalendarEvent>): String {
        val eventsText = if (events.isEmpty()) "Ninguna" else events.joinToString("\n- ") { "${it.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} ${it.title}" }
        return """
        ### ROL Y OBJETIVO ###
        Tu nombre es Chapelotas. Eres la secretaria ejecutiva, proactiva e "hincha pelotas" de un usuario. Tu misión es comunicar el resumen del día basándote ESTRICTAMENTE en los datos de calendario que te proporciono. Tu nombre significa que eres insistente porque te preocupas de que todo se cumpla.
        ### PERSONALIDAD ###
        Tu tono es el de una secretaria argentina con humor ácido. Eres sarcástica para relajar al usuario, que tiene muchas presiones. Puedes usar la ironía y meter presión de forma divertida, sobre todo si es tarde y aún quedan cosas por hacer.
        ### INSTRUCCIONES ###
        1. Saludo: Empieza con un saludo que se ajuste a la hora del día en Argentina.
        2. MANDATORIO: ENUMERAR TAREAS: Debes comunicar de forma clara TODAS las tareas pendientes que te paso en la sección de DATOS. Puedes hacerlo en una lista o en un párrafo, como te parezca más natural, pero NO puedes omitir ninguna.
        3. REGLA DE ORO (NO INVENTAR): Está terminantemente prohibido inventar o sugerir actividades que no estén en la lista de DATOS. Cíñete exclusivamente a la información del calendario que te entrego.
        4. MANEJO DE DÍA VACÍO: Si en la sección de DATOS te entrego "Ninguna" tarea, tienes PROHIBIDO mencionar la palabra "tareas" o "actividades". En su lugar, haz un comentario ingenioso sobre la falta de actividad (la "vagancia").
        ### DATOS ###
        Hora Actual: ${LocalDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))}
        Tareas Pendientes:
        - ${eventsText}
        """.trimIndent()
    }

    override suspend fun generateCommunicationPlan(events: List<CalendarEvent>, userContext: String?): AIPlan = generateSimulatedPlan(events)
    override suspend fun generateNotificationMessage(event: CalendarEvent, messageType: String, isSarcastic: Boolean, additionalContext: String?): String = generateSimulatedMessage(event, messageType, isSarcastic)
    override suspend fun generateTomorrowSummary(tomorrowEvents: List<CalendarEvent>, todayContext: String?, isSarcastic: Boolean): String = generateSimulatedTomorrowSummary(tomorrowEvents, isSarcastic)
    override suspend fun callOpenAIForText(prompt: String, temperature: Double): String = try { openAIApi.createChatCompletion(authorization = "Bearer $apiKey", request = ChatCompletionRequest(messages = listOf(Message("user", prompt)), temperature = temperature)).choices.firstOrNull()?.message?.content?.trim() ?: "No pude generar una respuesta." } catch (e: Exception) { "Error de conexión con la IA de Chapelotas." }
    private fun generateSimulatedEventPlan(event: CalendarEvent): List<PlannedNotification> = if (listOf("examen", "vuelo", "médico", "entrevista", "jefe", "final").any { event.title.contains(it, ignoreCase = true) }) { listOf(PlannedNotification(event.id, event.startTime.minusMinutes(15), "¡ATENCIÓN! Evento crítico '${event.title}' en 15 minutos.", com.chapelotas.app.domain.entities.NotificationPriority.CRITICAL, com.chapelotas.app.domain.entities.NotificationType.EVENT_REMINDER, null, false)) } else { listOf(PlannedNotification(event.id, event.startTime.minusMinutes(15), "Recordatorio: ${event.title} en 15 minutos.", com.chapelotas.app.domain.entities.NotificationPriority.NORMAL, com.chapelotas.app.domain.entities.NotificationType.EVENT_REMINDER, null, false)) }
    private fun generateSimulatedPlan(events: List<CalendarEvent>): AIPlan = AIPlan(UUID.randomUUID().toString(), LocalDateTime.now(ZoneId.systemDefault()), events.map { it.id }, events.map { PlannedNotification(it.id, it.startTime.minusMinutes(15), "Recordatorio: ${it.title}", com.chapelotas.app.domain.entities.NotificationPriority.NORMAL, com.chapelotas.app.domain.entities.NotificationType.EVENT_REMINDER, null, false) }, "Modo simulado activado", "Revisar eventos.")
    private fun generateSimulatedMessage(event: CalendarEvent, messageType: String, isSarcastic: Boolean): String = if (isSarcastic) "Che, ${event.title} en ${event.timeUntilStartDescription()}." else "Recordatorio: ${event.title} en ${event.timeUntilStartDescription()}."
    private fun generateSimulatedDailySummary(events: List<CalendarEvent>, isSarcastic: Boolean): String = if (isSarcastic) { if (events.isEmpty()) "Hoy te la pasás mirando el techo, parece. Cero eventos." else "Hoy tenés ${events.size} cosas. A ver si las hacés." } else { if (events.isEmpty()) "No tienes eventos para hoy. ¡Disfruta tu día!" else "Hoy tienes ${events.size} eventos programados." }
    private fun generateSimulatedTomorrowSummary(events: List<CalendarEvent>, isSarcastic: Boolean): String = if (isSarcastic) { if (events.isEmpty()) "Mañana pinta para la reposera. Nada en la agenda." else "Mañana hay ${events.size} eventos. Andá preparándote." } else { if (events.isEmpty()) "No tienes eventos para mañana." else "Mañana tienes ${events.size} eventos programados." }
    override suspend fun callOpenAIForJSON(prompt: String, temperature: Double): String = openAIApi.createChatCompletion(authorization = "Bearer $apiKey", request = ChatCompletionRequest(messages = listOf(Message("system", "Responde SOLO con JSON válido."), Message("user", prompt)), temperature = temperature)).choices.firstOrNull()?.message?.content?.trim() ?: throw Exception("No response from AI")
    override suspend fun suggestCriticalEvents(event: CalendarEvent, userHistory: List<CalendarEvent>?): Boolean = false
    override suspend fun generatePreparationTips(event: CalendarEvent, weatherInfo: String?, trafficInfo: String?): String? = null
    override suspend fun testConnection(): Boolean = try { openAIApi.createChatCompletion(authorization = "Bearer $apiKey", request = ChatCompletionRequest(messages = listOf(Message("user", "Di 'OK'")))).choices.isNotEmpty() } catch (e: Exception) { false }
    override suspend fun getCurrentModel(): String = if (apiKey.isNotEmpty()) "GPT-4o (Batch)" else "Modo Simulado"
}