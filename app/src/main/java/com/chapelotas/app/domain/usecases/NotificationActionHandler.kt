package com.chapelotas.app.domain.usecases

import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.chapelotas.app.data.database.ChapelotasDatabase
import com.chapelotas.app.data.database.entities.UserAction
import com.chapelotas.app.domain.events.ChapelotasEvent
import com.chapelotas.app.domain.events.ChapelotasEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maneja las acciones de notificaciones (Snooze, Dismiss, etc)
 * RESUELVE EL PROBLEMA DEL SNOOZE!
 */
@Singleton
class NotificationActionHandler @Inject constructor(
    private val database: ChapelotasDatabase,
    private val eventBus: ChapelotasEventBus,
    private val notificationManager: NotificationManagerCompat,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "NotificationHandler"
        private const val DEFAULT_SNOOZE_MINUTES = 5
    }

    /**
     * Manejar acción de SNOOZE
     * Ahora sí funciona! 🎉
     */
    fun handleSnooze(
        notificationId: Long,
        eventId: String,
        snoozeMinutes: Int = DEFAULT_SNOOZE_MINUTES
    ) {
        scope.launch {
            try {
                Log.d(TAG, "📱 Snooze solicitado: Notif=$notificationId, Minutos=$snoozeMinutes")

                // 1. Obtener la notificación original
                val notification = database.notificationDao().getNotification(notificationId)

                if (notification == null) {
                    Log.w(TAG, "⚠️ Notificación $notificationId no encontrada")
                    return@launch
                }

                // 2. Calcular nueva hora
                val snoozeUntil = LocalDateTime.now().plusMinutes(snoozeMinutes.toLong())

                // 3. Actualizar en Room (el mono la detectará)
                val snoozed = database.notificationDao().snoozeNotification(
                    notificationId = notificationId,
                    minutes = snoozeMinutes
                )

                if (snoozed) {
                    Log.d(TAG, "✅ Snooze actualizado en DB hasta: $snoozeUntil")

                    // 4. Cancelar notificación actual de Android
                    notificationManager.cancel(notification.getAndroidNotificationId())

                    // 5. Log para analytics
                    updateNotificationLog(
                        notificationId = notificationId,
                        action = UserAction.SNOOZED,
                        responseTime = calculateResponseTime(notification.scheduledTime)
                    )

                    // 6. Emitir evento
                    eventBus.emit(ChapelotasEvent.NotificationSnoozed(
                        notificationId = notificationId,
                        eventId = eventId,
                        snoozeMinutes = snoozeMinutes,
                        newTime = snoozeUntil
                    ))

                    // 7. Log amigable
                    val event = database.eventPlanDao().getEvent(eventId)
                    Log.d(TAG, "🔔 '${event?.title}' pospuesto $snoozeMinutes minutos")

                } else {
                    Log.e(TAG, "❌ No se pudo hacer snooze")
                }

            } catch (e: Exception) {
                Log.e(TAG, "💥 Error en snooze", e)
            }
        }
    }

    /**
     * Manejar DISMISS (Listo/Entendido)
     */
    fun handleDismiss(
        notificationId: Long,
        eventId: String,
        wasEffective: Boolean = true
    ) {
        scope.launch {
            try {
                Log.d(TAG, "📱 Dismiss: Notif=$notificationId")

                // 1. Marcar como dismissed en DB
                database.notificationDao().markAsDismissed(notificationId)

                // 2. Cancelar notificación de Android
                val notification = database.notificationDao().getNotification(notificationId)
                notification?.let {
                    notificationManager.cancel(it.getAndroidNotificationId())
                }

                // 3. Log para analytics
                updateNotificationLog(
                    notificationId = notificationId,
                    action = UserAction.DISMISSED,
                    responseTime = notification?.let {
                        calculateResponseTime(it.scheduledTime)
                    } ?: 0
                )

                // 4. Emitir evento
                eventBus.emit(ChapelotasEvent.NotificationDismissed(
                    notificationId = notificationId,
                    eventId = eventId,
                    wasEffective = wasEffective
                ))

                // 5. Incrementar contador en el evento
                database.eventPlanDao().incrementNotificationCount(
                    eventId = eventId,
                    time = LocalDateTime.now()
                )

            } catch (e: Exception) {
                Log.e(TAG, "💥 Error en dismiss", e)
            }
        }
    }

    /**
     * Manejar OPEN (abrir la app desde notificación)
     */
    fun handleOpen(
        notificationId: Long,
        eventId: String
    ) {
        scope.launch {
            try {
                // Similar a dismiss pero con UserAction.OPENED
                database.notificationDao().markAsExecuted(notificationId)

                updateNotificationLog(
                    notificationId = notificationId,
                    action = UserAction.OPENED,
                    responseTime = 0 // Respuesta inmediata
                )

                eventBus.emit(ChapelotasEvent.NotificationActionTaken(
                    notificationId = notificationId,
                    action = UserAction.OPENED,
                    responseTimeSeconds = 0
                ))

            } catch (e: Exception) {
                Log.e(TAG, "💥 Error en open", e)
            }
        }
    }

    /**
     * Manejar timeout (usuario ignoró la notificación)
     */
    fun handleTimeout(
        notificationId: Long,
        timeoutMinutes: Int = 30
    ) {
        scope.launch {
            try {
                val notification = database.notificationDao().getNotification(notificationId)
                    ?: return@launch

                // Solo marcar como ignorada si no fue ejecutada ni dismissed
                if (!notification.executed && !notification.dismissed) {
                    updateNotificationLog(
                        notificationId = notificationId,
                        action = UserAction.IGNORED,
                        responseTime = timeoutMinutes * 60L
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "💥 Error en timeout", e)
            }
        }
    }

    /**
     * Actualizar log de notificación con la acción del usuario
     */
    private suspend fun updateNotificationLog(
        notificationId: Long,
        action: UserAction,
        responseTime: Long
    ) {
        try {
            // Buscar el log más reciente para esta notificación
            val logs = database.notificationLogDao()
                .getLogsByEvent("") // TODO: Mejorar query

            val log = logs.find { it.notificationId == notificationId }

            log?.let {
                database.notificationLogDao().updateUserAction(
                    logId = it.logId,
                    action = action,
                    responseTime = responseTime
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando log", e)
        }
    }

    /**
     * Calcular tiempo de respuesta en segundos
     */
    private fun calculateResponseTime(scheduledTime: LocalDateTime): Long {
        return ChronoUnit.SECONDS.between(scheduledTime, LocalDateTime.now())
            .coerceAtLeast(0)
    }
}