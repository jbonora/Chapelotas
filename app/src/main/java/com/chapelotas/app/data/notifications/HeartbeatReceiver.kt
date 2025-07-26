package com.chapelotas.app.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log // Importante añadir esta línea
import androidx.core.app.NotificationCompat
import com.chapelotas.app.R
import com.chapelotas.app.domain.debug.DebugLog
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Este es el "desfibrilador" de Chapelotas.
 * Es despertado por una alarma del sistema y ahora genera una notificación visible
 * como "prueba de vida".
 */
class HeartbeatReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // --- INICIO DE LA CORRECCIÓN ---
        // Este mensaje aparecerá en Logcat y nos confirmará que la alarma funciona.
        Log.d("HeartbeatReceiver", ">>>> ⏰ ¡Alarma de Heartbeat RECIBIDA! Intentando mostrar notificación...")
        // --- FIN DE LA CORRECCIÓN ---

        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val message = "❤️ Heartbeat a las $time. Chapelotas sigue vivo."

        // 1. Registrar en nuestra caja negra
        DebugLog.add(message)

        // 2. Mostrar una notificación visible
        showHeartbeatNotification(context, message)

        // 3. Intentar reiniciar el servicio principal
        val serviceIntent = Intent(context, ChapelotasNotificationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    private fun showHeartbeatNotification(context: Context, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "chapelotas_heartbeat"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Pruebas de Vida (Debug)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de debug para saber si la app está viva."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Chapelotas - Prueba de Vida")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOnlyAlertOnce(true)
            .build()

        notificationManager.notify(HEARTBEAT_NOTIFICATION_ID, notification)
    }

    companion object {
        const val HEARTBEAT_NOTIFICATION_ID = 111
    }
}