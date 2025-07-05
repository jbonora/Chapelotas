package com.chapelotas.app.data.notifications

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.chapelotas.app.R
import com.chapelotas.app.MainActivity
import android.app.PendingIntent
import dagger.hilt.android.AndroidEntryPoint

/**
 * Servicio en primer plano que mantiene Chapelotas activo
 * Esto asegura que las notificaciones funcionen incluso si el sistema mata la app
 */
@AndroidEntryPoint
class ChapelotasNotificationService : Service() {

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY hace que el servicio se reinicie si el sistema lo mata
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        // No necesitamos binding
        return null
    }

    /**
     * Inicia el servicio en primer plano con una notificación persistente
     */
    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NotificationRepositoryImpl.CHANNEL_SERVICE)
            .setContentTitle("Chapelotas está activo")
            .setContentText("Vigilando tu calendario para que no te olvides de nada 👀")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NotificationRepositoryImpl.FOREGROUND_SERVICE_ID, notification)
    }
}