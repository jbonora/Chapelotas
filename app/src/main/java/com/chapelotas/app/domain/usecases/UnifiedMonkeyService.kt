package com.chapelotas.app.domain.usecases

import android.content.Context
import android.util.Log
import com.chapelotas.app.data.database.ChapelotasDatabase
import com.chapelotas.app.data.database.entities.*
import com.chapelotas.app.domain.events.ChapelotasEvent
import com.chapelotas.app.domain.events.ChapelotasEventBus
import com.chapelotas.app.domain.repositories.NotificationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * El Mono 2.0 - Ahora con Room y más inteligente
 * No más JSON, todo en base de datos
 */
@Singleton
class UnifiedMonkeyService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: ChapelotasDatabase,
    private val eventBus: ChapelotasEventBus,
    private val notificationRepository: NotificationRepository,
    private val masterPlanController: MasterPlanController,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "MonkeyService"
        private const val CHECK_INTERVAL_MS = 5 * 60 * 1000L // 5 minutos
        private const val MIN_CHECK_INTERVAL_MS = 2 * 60 * 1000L // 2 minutos cuando hay eventos próximos
    }

    private var checkJob: Job? = null
    private var checkCounter = 0L

    /**
     * Iniciar el mono - ahora más simple y robusto
     */
    fun startMonkey() {
        Log.d(TAG, "🐵 Mono 2.0 despertando con Room...")

        checkJob?.cancel()
        checkJob = scope.launch {
            // Primero, asegurar que existe el plan de hoy
            ensureTodayPlan()

            while (isActive) {
                try {
                    performCheck()

                    // Calcular próximo check inteligentemente
                    val nextInterval = calculateNextCheckInterval()
                    delay(nextInterval)

                } catch (e: Exception) {
                    Log.e(TAG, "🐵 Error en check: ${e.message}", e)
                    eventBus.tryEmit(ChapelotasEvent.MonkeyError(
                        error = e.message ?: "Unknown error",
                        willRetry = true,
                        retryInSeconds = 30
                    ))
                    delay(30_000) // Retry en 30 segundos
                }
            }
        }
    }

    /**
     * Detener el mono (aunque no debería detenerse nunca)
     */
    fun stopMonkey() {
        Log.w(TAG, "🐵 Alguien intenta detener al mono... ignorando")
        // El mono es eterno
    }

    /**
     * Check principal del mono
     */
    private suspend fun performCheck() {
        checkCounter++
        val startTime = System.currentTimeMillis()

        Log.d(TAG, "🐵 Check #$checkCounter iniciando...")
        eventBus.emit(ChapelotasEvent.MonkeyCheckStarted(checkCounter))

        val now = LocalDateTime.now()
        var notificationsShown = 0

        // 1. Obtener notificaciones pendientes de Room
        val pendingNotifications = database.notificationDao()
            .getPendingNotifications(now)

        Log.d(TAG, "🐵 Encontradas ${pendingNotifications.size} notificaciones pendientes")

        // 2. Procesar cada notificación
        for (notification in pendingNotifications) {
            if (notification.shouldShowNow()) {
                showNotification(notification)
                notificationsShown++
            }
        }

        // 3. Checkear conflictos no notificados
        checkUnnotifiedConflicts()

        // 4. Actualizar próximo check time en el plan
        updateNextCheckTime()

        // 5. Emitir evento de completado
        val nextCheck = database.notificationDao()
            .getNextNotificationTime(now) ?: now.plusMinutes(5)

        val duration = System.currentTimeMillis() - startTime
        Log.d(TAG, "🐵 Check completado en ${duration}ms. Notificaciones: $notificationsShown")

        eventBus.emit(ChapelotasEvent.MonkeyCheckCompleted(
            checkNumber = checkCounter,
            notificationsShown = notificationsShown,
            nextCheckTime = nextCheck
        ))
    }

    /**
     * Mostrar una notificación
     */
    private suspend fun showNotification(notification: ScheduledNotification) {
        try {
            // Obtener el evento de la DB
            val event = database.eventPlanDao().getEvent(notification.eventId)
                ?: run {
                    Log.w(TAG, "🐵 Evento ${notification.eventId} no encontrado")
                    database.notificationDao().markAsDismissed(notification.notificationId)
                    return
                }

            // Obtener el plan del día
            val dayPlan = database.dayPlanDao().getPlan(event.dayDate)
                ?: return

            // Generar mensaje con IA o usar el pre-generado
            val message = notification.message ?: generateNotificationMessage(
                event = event,
                plan = dayPlan,
                notification = notification
            )

            // Convertir a notificación del dominio
            val domainNotification = com.chapelotas.app.domain.entities.ChapelotasNotification(
                id = notification.notificationId.toString(),
                eventId = event.calendarEventId,
                scheduledTime = notification.scheduledTime,
                message = message,
                priority = when(notification.priority) {
                    NotificationPriority.LOW -> com.chapelotas.app.domain.entities.NotificationPriority.LOW
                    NotificationPriority.NORMAL -> com.chapelotas.app.domain.entities.NotificationPriority.NORMAL
                    NotificationPriority.HIGH -> com.chapelotas.app.domain.entities.NotificationPriority.HIGH
                    NotificationPriority.CRITICAL -> com.chapelotas.app.domain.entities.NotificationPriority.CRITICAL
                },
                type = when(notification.type) {
                    NotificationType.REMINDER -> com.chapelotas.app.domain.entities.NotificationType.EVENT_REMINDER
                    NotificationType.CRITICAL_ALERT -> com.chapelotas.app.domain.entities.NotificationType.CRITICAL_ALERT
                    NotificationType.PREPARATION_TIP -> com.chapelotas.app.domain.entities.NotificationType.PREPARATION_TIP
                    else -> com.chapelotas.app.domain.entities.NotificationType.EVENT_REMINDER
                }
            )

            // Mostrar la notificación
            notificationRepository.showImmediateNotification(domainNotification)

            // Marcar como ejecutada
            database.notificationDao().markAsExecuted(notification.notificationId)

            // Log para analytics
            val logId = database.notificationLogDao().logNotificationShown(
                notification = notification,
                event = event,
                message = message,
                deviceLocked = false, // TODO: Detectar estado del dispositivo
                appInForeground = false // TODO: Detectar si app está en foreground
            )

            // Emitir evento
            eventBus.emit(ChapelotasEvent.NotificationShown(
                notificationId = notification.notificationId,
                eventId = event.eventId,
                message = message
            ))

            Log.d(TAG, "🐵 Notificación mostrada: ${event.title}")

        } catch (e: Exception) {
            Log.e(TAG, "🐵 Error mostrando notificación", e)
        }
    }

    /**
     * Generar mensaje con IA
     */
    private suspend fun generateNotificationMessage(
        event: EventPlan,
        plan: DayPlan,
        notification: ScheduledNotification
    ): String {
        return try {
            // Preparar contexto para la IA
            val timeNow = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            val eventTime = event.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))

            // Obtener eventos críticos pendientes
            val criticalEvents = database.eventPlanDao()
                .getTodayCriticalEvents()
                .filter { it.startTime.isAfter(LocalDateTime.now()) }

            // Crear estructura similar al JSON para la IA
            val eventInfo = buildString {
                append("Evento: ${event.title}\n")
                append("Hora: $eventTime\n")
                event.location?.let { append("Lugar: $it\n") }
                append("Distancia: ${event.distance.displayName}\n")
                if (event.isCritical) append("ES CRÍTICO\n")
            }

            val criticalInfo = if (criticalEvents.isNotEmpty()) {
                "Eventos críticos pendientes: ${criticalEvents.joinToString { it.title }}"
            } else ""

            // Llamar a la IA (reutilizando el método existente)
            masterPlanController.callOpenAIForText(
                prompt = """
                    Sos Chapelotas, secretaria ejecutiva de ${plan.userName.ifEmpty { "este usuario" }}.
                    ${if (plan.sarcasticMode) "Modo: sarcástico argentino" else "Modo: profesional"}
                    Hora actual: $timeNow
                    
                    EVENTO A NOTIFICAR:
                    $eventInfo
                    
                    $criticalInfo
                    
                    Genera un mensaje corto (máximo 2 líneas) recordando este evento.
                    ${if (plan.sarcasticMode) "Usá humor argentino pero siempre claro con la info" else "Sé profesional y cordial"}
                """.trimIndent(),
                temperature = 0.8
            )
        } catch (e: Exception) {
            // Fallback message
            val minutesUntil = event.minutesUntilStart()
            when {
                event.isCritical -> "🚨 CRÍTICO: ${event.title} en $minutesUntil minutos!"
                minutesUntil <= 15 -> "⏰ ${event.title} empieza en $minutesUntil minutos"
                else -> "Recordatorio: ${event.title} a las ${event.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            }
        }
    }

    /**
     * Checkear conflictos no notificados
     */
    private suspend fun checkUnnotifiedConflicts() {
        val unnotifiedConflicts = database.conflictDao().getUnnotifiedConflicts()

        for (conflict in unnotifiedConflicts) {
            // Crear notificación especial para conflicto
            val message = conflict.getUserMessage()

            val notification = com.chapelotas.app.domain.entities.ChapelotasNotification(
                id = "conflict_${conflict.conflictId}",
                eventId = 0, // No es de un evento específico
                scheduledTime = LocalDateTime.now(),
                message = message,
                priority = when (conflict.severity) {
                    ConflictSeverity.HIGH -> com.chapelotas.app.domain.entities.NotificationPriority.CRITICAL
                    ConflictSeverity.MEDIUM -> com.chapelotas.app.domain.entities.NotificationPriority.HIGH
                    ConflictSeverity.LOW -> com.chapelotas.app.domain.entities.NotificationPriority.NORMAL
                },
                type = com.chapelotas.app.domain.entities.NotificationType.EVENT_REMINDER
            )

            notificationRepository.showImmediateNotification(notification)
            database.conflictDao().markAsNotified(conflict.conflictId)

            eventBus.emit(ChapelotasEvent.ConflictDetected(
                conflictId = conflict.conflictId,
                event1Id = conflict.event1Id,
                event2Id = conflict.event2Id,
                type = conflict.type,
                severity = conflict.severity
            ))
        }
    }

    /**
     * Calcular próximo intervalo de check inteligentemente
     */
    private suspend fun calculateNextCheckInterval(): Long {
        val nextNotification = database.notificationDao()
            .getNextNotificationTime()

        return if (nextNotification != null) {
            val minutesUntil = java.time.Duration
                .between(LocalDateTime.now(), nextNotification)
                .toMinutes()

            when {
                minutesUntil <= 10 -> MIN_CHECK_INTERVAL_MS // 2 min
                minutesUntil <= 30 -> 3 * 60 * 1000L // 3 min
                else -> CHECK_INTERVAL_MS // 5 min
            }
        } else {
            CHECK_INTERVAL_MS // Default 5 min
        }
    }

    /**
     * Asegurar que existe plan para hoy
     */
    private suspend fun ensureTodayPlan() {
        database.dayPlanDao().getOrCreateTodayPlan()
    }

    /**
     * Actualizar próximo check time
     */
    private suspend fun updateNextCheckTime() {
        val nextTime = LocalTime.now().plusMinutes(5)
            .format(DateTimeFormatter.ofPattern("HH:mm"))

        database.dayPlanDao().updateNextCheckTime(
            LocalDate.now(),
            nextTime
        )
    }
}