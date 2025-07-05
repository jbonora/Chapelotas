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
        val notificationManager = NotificationManagerCompat.from(context)

        when (intent.action) {
            "SNOOZE" -> {
                Log.d("NotificationAction", "Snooze: $notificationId")
                // Por ahora solo cerrar, luego implementar snooze real
                notificationManager.cancel(notificationId.hashCode())

                // TODO: Reprogramar para 5 minutos después
                CoroutineScope(Dispatchers.IO).launch {
                    // Implementar snooze
                }
            }

            "DISMISS" -> {
                Log.d("NotificationAction", "Dismiss: $notificationId")
                notificationManager.cancel(notificationId.hashCode())
            }
        }
    }
}