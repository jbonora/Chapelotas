package com.chapelotas.app.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.chapelotas.app.domain.usecases.NotificationActionHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receiver para acciones de notificaciones
 * ACTUALIZADO para usar el nuevo NotificationActionHandler
 */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var actionHandler: NotificationActionHandler

    companion object {
        private const val TAG = "NotificationReceiver"

        // Actions
        const val ACTION_SNOOZE = "com.chapelotas.action.SNOOZE"
        const val ACTION_DISMISS = "com.chapelotas.action.DISMISS"
        const val ACTION_OPEN = "com.chapelotas.action.OPEN"

        // Extras
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "📱 Acción recibida: ${intent.action}")

        // Obtener datos del intent
        val notificationId = intent.getLongExtra(EXTRA_NOTIFICATION_ID, -1)
        val eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: ""

        if (notificationId == -1L || eventId.isEmpty()) {
            Log.e(TAG, "❌ Datos inválidos: notifId=$notificationId, eventId=$eventId")
            return
        }

        // Procesar según la acción
        when (intent.action) {
            ACTION_SNOOZE -> {
                val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 5)
                Log.d(TAG, "⏰ Snooze: $snoozeMinutes minutos")

                // Usar el handler inyectado
                actionHandler.handleSnooze(
                    notificationId = notificationId,
                    eventId = eventId,
                    snoozeMinutes = snoozeMinutes
                )
            }

            ACTION_DISMISS -> {
                Log.d(TAG, "✅ Dismiss")

                actionHandler.handleDismiss(
                    notificationId = notificationId,
                    eventId = eventId,
                    wasEffective = true
                )
            }

            ACTION_OPEN -> {
                Log.d(TAG, "📱 Open app")

                actionHandler.handleOpen(
                    notificationId = notificationId,
                    eventId = eventId
                )
            }

            else -> {
                Log.w(TAG, "⚠️ Acción desconocida: ${intent.action}")
            }
        }
    }
}