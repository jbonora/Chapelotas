package com.chapelotas.app.domain.usecases

import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.chapelotas.app.data.database.ChapelotasDatabase
import com.chapelotas.app.data.database.entities.ConversationLog
import com.chapelotas.app.data.database.entities.UserAction
import com.chapelotas.app.domain.events.ChapelotasEvent
import com.chapelotas.app.domain.events.ChapelotasEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationActionHandler @Inject constructor(
    private val database: ChapelotasDatabase,
    private val eventBus: ChapelotasEventBus,
    private val notificationManager: NotificationManagerCompat,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "NotificationHandler"
        private const val DEFAULT_SNOOZE_MINUTES = 15
    }

    fun handleSnooze(notificationId: Long, eventId: String, snoozeMinutes: Int = DEFAULT_SNOOZE_MINUTES) {
        scope.launch {
            try {
                Log.d(TAG, "📱 Snooze solicitado: Notif=$notificationId, Minutos=$snoozeMinutes")
                val notification = database.notificationDao().getNotification(notificationId)
                if (notification == null) {
                    Log.w(TAG, "⚠️ Notificación $notificationId para Snooze no encontrada.")
                    return@launch
                }

                // Usar el eventId de la notificación, no el parámetro
                val actualEventId = notification.eventId
                val event = database.eventPlanDao().getEvent(actualEventId)

                database.conversationLogDao().insert(ConversationLog(
                    timestamp = LocalDateTime.now(),
                    role = "user",
                    content = "Acción: POSPONER ${snoozeMinutes}min para el evento '${event?.title ?: "sin título"}'.",
                    eventId = actualEventId
                ))

                val snoozeUntil = LocalDateTime.now().plusMinutes(snoozeMinutes.toLong())
                database.notificationDao().snooze(notificationId, snoozeUntil)

                Log.d(TAG, "✅ Snooze actualizado en DB hasta: $snoozeUntil")
                notificationManager.cancel(notification.getAndroidNotificationId())
                updateNotificationLog(notificationId, UserAction.SNOOZED, calculateResponseTime(notification.scheduledTime))
                eventBus.emit(ChapelotasEvent.NotificationSnoozed(notificationId, actualEventId, snoozeMinutes, snoozeUntil))
                Log.d(TAG, "🔔 '${event?.title ?: "Evento"}' pospuesto $snoozeMinutes minutos")

            } catch (e: Exception) {
                Log.e(TAG, "💥 Error en snooze", e)
            }
        }
    }

    fun handleDismiss(notificationId: Long, eventId: String, wasEffective: Boolean = true) {
        scope.launch {
            try {
                Log.d(TAG, "📱 Dismiss: Notif=$notificationId. Marcando como resuelta.")

                // Primero obtener la notificación para tener el eventId correcto
                val notification = database.notificationDao().getNotification(notificationId)
                val actualEventId = notification?.eventId ?: eventId

                val event = database.eventPlanDao().getEvent(actualEventId)
                database.conversationLogDao().insert(ConversationLog(
                    timestamp = LocalDateTime.now(),
                    role = "user",
                    content = "Acción: MARCAR COMO HECHO para el evento '${event?.title ?: "sin título"}'.",
                    eventId = actualEventId
                ))

                database.notificationDao().markAsExecuted(notificationId)

                notification?.let {
                    notificationManager.cancel(it.getAndroidNotificationId())
                }

                updateNotificationLog(notificationId, UserAction.DISMISSED, notification?.let { calculateResponseTime(it.scheduledTime) } ?: 0)
                eventBus.emit(ChapelotasEvent.NotificationDismissed(notificationId, actualEventId, wasEffective))

                database.eventPlanDao().incrementNotificationCount(actualEventId, LocalDateTime.now())
                Log.d(TAG, "✅ Notificación $notificationId resuelta por el usuario.")
            } catch (e: Exception) {
                Log.e(TAG, "💥 Error en dismiss", e)
            }
        }
    }

    fun handleOpen(notificationId: Long, eventId: String) {
        scope.launch {
            try {
                // Obtener la notificación para tener el eventId correcto
                val notification = database.notificationDao().getNotification(notificationId)
                val actualEventId = notification?.eventId ?: eventId

                val event = database.eventPlanDao().getEvent(actualEventId)
                database.conversationLogDao().insert(ConversationLog(
                    timestamp = LocalDateTime.now(),
                    role = "user",
                    content = "Acción: ABRIR/VER DETALLE para el evento '${event?.title ?: "sin título"}'.",
                    eventId = actualEventId
                ))
                database.notificationDao().markAsExecuted(notificationId)
                updateNotificationLog(notificationId, UserAction.OPENED, 0)
                eventBus.emit(ChapelotasEvent.NotificationActionTaken(notificationId, UserAction.OPENED, 0))
            } catch (e: Exception) {
                Log.e(TAG, "💥 Error en open", e)
            }
        }
    }

    fun handleTimeout(notificationId: Long, timeoutMinutes: Int = 30) {
        scope.launch {
            try {
                val notification = database.notificationDao().getNotification(notificationId) ?: return@launch
                if (!notification.executed) {
                    val event = database.eventPlanDao().getEvent(notification.eventId)
                    database.conversationLogDao().insert(ConversationLog(
                        timestamp = LocalDateTime.now(),
                        role = "user",
                        content = "Acción: IGNORAR (timeout) para el evento '${event?.title ?: "sin título"}'.",
                        eventId = notification.eventId
                    ))
                    updateNotificationLog(notificationId, UserAction.IGNORED, timeoutMinutes * 60L)
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 Error en timeout", e)
            }
        }
    }

    private suspend fun updateNotificationLog(notificationId: Long, action: UserAction, responseTime: Long) {
        try {
            val logs = database.notificationLogDao().getLogsByEvent("")
            val log = logs.find { it.notificationId == notificationId }
            log?.let {
                database.notificationLogDao().updateUserAction(it.logId, action, responseTime = responseTime)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando log", e)
        }
    }

    private fun calculateResponseTime(scheduledTime: LocalDateTime): Long {
        return ChronoUnit.SECONDS.between(scheduledTime, LocalDateTime.now()).coerceAtLeast(0)
    }
}