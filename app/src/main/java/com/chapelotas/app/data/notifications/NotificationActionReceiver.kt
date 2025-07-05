package com.chapelotas.app.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getStringExtra("notification_id") ?: return
        val eventId = intent.getLongExtra("event_id", -1)
        val notificationManager = NotificationManagerCompat.from(context)

        when (intent.action) {
            "SNOOZE" -> {
                Log.d("NotificationAction", "Snooze: $notificationId")
                // Cerrar notificación actual
                notificationManager.cancel(eventId.toInt())

                // Por ahora solo cerrar la notificación
                // TODO: Implementar snooze real reprogramando para 5 minutos después
                CoroutineScope(Dispatchers.IO).launch {
                    // En el futuro: reprogramar notificación
                }
            }

            "DISMISS" -> {
                Log.d("NotificationAction", "Dismiss: $notificationId")
                notificationManager.cancel(eventId.toInt())
            }
        }
    }
}