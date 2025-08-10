package com.chapelotas.app.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.chapelotas.app.R
import com.chapelotas.app.di.Constants
import com.chapelotas.app.domain.debug.DebugLog
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class HeartbeatReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("HeartbeatReceiver", ">>>> ⏰ ¡Alarma de Heartbeat RECIBIDA! Intentando mostrar notificación...")

        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val message = "❤️ Heartbeat a las $time. Chapelotas sigue vivo."

        DebugLog.add(message)
        showHeartbeatNotification(context, message)

        val serviceIntent = Intent(context, ChapelotasNotificationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    private fun showHeartbeatNotification(context: Context, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.CHANNEL_ID_HEARTBEAT,
                context.getString(R.string.notification_heartbeat_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_heartbeat_channel_description)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, Constants.CHANNEL_ID_HEARTBEAT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_heartbeat_title))
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOnlyAlertOnce(true)
            .build()

        notificationManager.notify(Constants.NOTIFICATION_ID_HEARTBEAT, notification)
    }
}