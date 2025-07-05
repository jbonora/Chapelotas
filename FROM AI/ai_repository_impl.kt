package com.chapelotas.app.data.ai

import com.chapelotas.app.domain.entities.*
import com.chapelotas.app.domain.repositories.AIRepository
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Implementación del repositorio de IA
 * Por ahora usa respuestas simuladas, pero está preparado para conectar con una API real
 */
@Singleton
class AIRepositoryImpl @Inject constructor() : AIRepository {
    
    // API configuration from BuildConfig (seguro, no se sube a Git)
    private val apiKey = BuildConfig.OPENAI_API_KEY
    private val apiEndpoint = "https://api.openai.com/v1/chat/completions"
    
    init {
        if (apiKey.isEmpty()) {
            android.util.Log.w("AIRepository", "API Key no configurada en local.properties")
        }
    }
    
    override suspend fun generateCommunicationPlan(
        events: List<CalendarEvent>,
        userContext: String?
    ): AIPlan {
        // Simular delay de red
        delay(1000)
        
        // Por ahora, generar un plan básico
        val notifications = mutableListOf<PlannedNotification>()
        
        events.forEach { event ->
            // Notificación 15 minutos antes
            notifications.add(
                PlannedNotification(
                    eventId = event.id,
                    suggestedTime = event.startTime.minusMinutes(15),
                    suggestedMessage = "¡Ey! ${event.title} empieza en 15 minutos. " +
                            if (event.location != null) "En ${event.location}. " else "" +
                            "¿Ya estás listo?",
                    priority = if (event.isCritical) NotificationPriority.HIGH else NotificationPriority.NORMAL,
                    type = NotificationType.EVENT_REMINDER,
                    rationale = "Recordatorio estándar 15 minutos antes"
                )
            )
            
            // Si es crítico, agregar notificación extra
            if (event.isCritical) {
                notifications.add(
                    PlannedNotification(
                        eventId = event.id,
                        suggestedTime = event.startTime.minusMinutes(30),
                        suggestedMessage = "⚠️ EVENTO CRÍTICO: ${event.title} en 30 minutos. " +
                                "Esto es IMPORTANTE. No lo olvides.",
                        priority = NotificationPriority.HIGH,
                        type = NotificationType.CRITICAL_ALERT,
                        rationale = "Alerta adicional para evento crítico"
                    )
                )
            }
        }
        
        return AIPlan(
            id = java.util.UUID.randomUUID().toString(),
            generatedAt = LocalDateTime.now(),
            eventsAnalyzed = events.map { it.id },
            notifications = notifications,
            aiInsights = generateInsights(events),
            suggestedFocus = if (events.any { it.isCritical }) {
                "Tenés eventos críticos hoy. Concentrate en no fallar."
            } else {
                "Día normal. Mantené el ritmo."
            }
        )
    }
    
    override suspend fun generateNotificationMessage(
        event: CalendarEvent,
        messageType: String,
        isSarcastic: Boolean,
        additionalContext: String?
    ): String {
        delay(500) // Simular delay
        
        return when (messageType) {
            "CRITICAL_SARCASTIC" -> generateCriticalSarcasticMessage(event)
            "CRITICAL_URGENT" -> generateCriticalUrgentMessage(event)
            else -> generateStandardMessage(event, isSarcastic)
        }
    }
    
    override suspend fun generateDailySummary(
        todayEvents: List<CalendarEvent>,
        isSarcastic: Boolean
    ): String {
        delay(800)
        
        if (todayEvents.isEmpty()) {
            return if (isSarcastic) {
                "¡Mirá qué productivo! No tenés NADA agendado hoy. " +
                "O sos un genio de la organización o te olvidaste de anotar todo. " +
                "¿Cuál será? 🤔"
            } else {
                "¡Buenos días! Hoy tenés el día libre según tu calendario. " +
                "¡Aprovechá para descansar o ponerte al día!"
            }
        }
        
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val eventList = todayEvents.joinToString("\n") { event ->
            "• ${event.startTime.format(formatter)} - ${event.title}" +
                    if (event.location != null) " (${event.location})" else ""
        }
        
        return if (isSarcastic) {
            "Buenos días, persona súper ocupada. Hoy tenés ${todayEvents.size} eventos " +
            "porque aparentemente no sabés decir que no:\n\n$eventList\n\n" +
            "¿Necesitás que te recuerde respirar también? 😏"
        } else {
            "¡Buenos días! Acá está tu agenda para hoy:\n\n$eventList\n\n" +
            "¡Que tengas un excelente día! 💪"
        }
    }
    
    override suspend fun generateTomorrowSummary(
        tomorrowEvents: List<CalendarEvent>,
        todayContext: String?,
        isSarcastic: Boolean
    ): String {
        delay(800)
        
        if (tomorrowEvents.isEmpty()) {
            return if (isSarcastic) {
                "Mañana no tenés nada. Cero. Nada. " +
                "¿Es un logro o una señal de que tu vida social murió? " +
                "Vos decidís. 🎉"
            } else {
                "Mañana tenés el día libre. " +
                "¡Perfecto para planificar algo espontáneo!"
            }
        }
        
        val firstEvent = tomorrowEvents.minByOrNull { it.startTime }
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        
        return if (isSarcastic) {
            "Preparate psicológicamente: mañana arrancás a las ${firstEvent?.startTime?.format(formatter)} " +
            "con ${firstEvent?.title}. Total tenés ${tomorrowEvents.size} eventos. " +
            "¿Dormís o directamente te quedás despierto? 🦉"
        } else {
            "Para mañana tenés ${tomorrowEvents.size} eventos programados. " +
            "El primero es ${firstEvent?.title} a las ${firstEvent?.startTime?.format(formatter)}. " +
            "¡Descansá bien esta noche!"
        }
    }
    
    override suspend fun suggestCriticalEvents(
        event: CalendarEvent,
        userHistory: List<CalendarEvent>?
    ): Boolean {
        // Por ahora, sugerir como crítico si:
        // - Tiene "importante", "urgente", "crítico" en el título
        // - Es una entrevista, examen, médico, etc.
        val criticalKeywords = listOf(
            "importante", "urgente", "crítico", "deadline",
            "entrevista", "examen", "médico", "doctor", "dentista",
            "vuelo", "avión", "presentación", "reunión con jefe"
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
        delay(500)
        
        // Generar tips basados en el tipo de evento
        return when {
            event.title.contains("reunión", ignoreCase = true) -> 
                "Llevá notebook y cargador. Revisá la agenda previa."
            event.title.contains("médico", ignoreCase = true) -> 
                "Llevá estudios previos y lista de síntomas/preguntas."
            event.location?.contains("aeropuerto", ignoreCase = true) == true -> 
                "Check-in online, llegá 2 horas antes, documento a mano."
            else -> null
        }
    }
    
    override suspend fun testConnection(): Boolean {
        delay(1000)
        // TODO: Implementar test real de conexión
        return true
    }
    
    override suspend fun getCurrentModel(): String {
        return "Simulado (Implementar API real)"
    }
    
    // Funciones auxiliares para generar mensajes
    
    private fun generateInsights(events: List<CalendarEvent>): String {
        return when {
            events.size > 5 -> "Día muy cargado. Considerá delegar o reprogramar algo."
            events.any { it.durationInMinutes > 180 } -> "Tenés reuniones largas. Programá breaks."
            events.all { it.startTime.hour >= 14 } -> "Mañana libre. ¿Aprovechás para tareas importantes?"
            else -> "Día balanceado. Bien distribuido."
        }
    }
    
    private fun generateCriticalSarcasticMessage(event: CalendarEvent): String {
        val messages = listOf(
            "¿En serio necesitás que te recuerde ${event.title}? ¿TAN mal estamos? 🙄",
            "ÚLTIMO AVISO antes de que arruines todo: ${event.title}. De nada.",
            "Mirá, yo te aviso: ${event.title}. Si llegás tarde, no digas que no insistí.",
            "🚨 ${event.title} 🚨 ¿O esperabas que te lo dijera con flores y chocolates?"
        )
        return messages.random()
    }
    
    private fun generateCriticalUrgentMessage(event: CalendarEvent): String {
        return "⚠️ ATENCIÓN: ${event.title} está por empezar. " +
               "Esto es CRÍTICO. Por favor, no lo olvides. ⚠️"
    }
    
    private fun generateStandardMessage(event: CalendarEvent, isSarcastic: Boolean): String {
        return if (isSarcastic) {
            "Che, genio, ${event.title} en ${event.minutesUntilStart()} minutos. " +
            "¿Ya te estás moviendo o seguís en modo estatua?"
        } else {
            "Recordatorio: ${event.title} empieza en ${event.minutesUntilStart()} minutos. " +
            if (event.location != null) "Lugar: ${event.location}" else "¡No te olvides!"
        }
    }
}

/**
 * Clase para la API real (para cuando quieras implementarla)
 */
data class OpenAIRequest(
    val model: String = "gpt-3.5-turbo",
    val messages: List<Message>,
    val temperature: Double = 0.7
)

data class Message(
    val role: String,
    val content: String
)

// TODO: Implementar las llamadas reales a la API cuando tengas el API key