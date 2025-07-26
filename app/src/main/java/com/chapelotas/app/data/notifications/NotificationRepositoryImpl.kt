package com.chapelotas.app.data.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.chapelotas.app.MainActivity
import com.chapelotas.app.R
import com.chapelotas.app.domain.entities.ChapelotasNotification
import com.chapelotas.app.domain.entities.NotificationAction
import com.chapelotas.app.domain.entities.NotificationType
import com.chapelotas.app.domain.repositories.NotificationRepository
import com.chapelotas.app.presentation.ui.CriticalAlertActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationRepository {

    private val notificationManager = NotificationManagerCompat.from(context)

    companion object {
        const val CHANNEL_GENERAL = "chapelotas_general"
        const val CHANNEL_CRITICAL = "chapelotas_critical"
        const val CHANNEL_SERVICE = "chapelotas_service"
        const val NOTIFICATION_DATA_KEY = "notification_data"
        private const val REQUEST_CODE_OPEN = 1000
    }

    init {
        createNotificationChannels()
    }

    override suspend fun showImmediateNotification(notification: ChapelotasNotification) {
        when (notification.type) {
            NotificationType.CRITICAL_ALERT -> showCriticalAlert(notification)
            else -> showStandardNotification(notification)
        }
    }

    private fun showStandardNotification(notification: ChapelotasNotification) {
        val channelId = CHANNEL_GENERAL
        val notificationIdLong = notification.id.toLongOrNull() ?: notification.id.hashCode().toLong()
        val androidNotifId = (notificationIdLong and Int.MAX_VALUE.toLong()).toInt()

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigation_route", "today")
            putExtra("notification_id", notificationIdLong)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN + androidNotifId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Chapelotas tiene un mensaje")
            .setContentText(notification.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
            .setSubText("Chapelotas")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setFullScreenIntent(openAppPendingIntent, true)

        // Añadir acciones dinámicamente
        notification.actions.forEach { action ->
            val pendingIntent = createActionIntent(androidNotifId, notification, action)
            val actionText = when (action) {
                NotificationAction.ACKNOWLEDGE -> "Entendido"
                NotificationAction.FINISH_DONE -> "Done"
            }
            val icon = when (action) {
                NotificationAction.FINISH_DONE -> R.drawable.ic_check
                else -> R.drawable.ic_snooze
            }
            builder.addAction(icon, actionText, pendingIntent)
        }

        postNotification(androidNotifId, builder.build())
    }

    private fun showCriticalAlert(notification: ChapelotasNotification) {
        val notificationIdLong = notification.id.toLongOrNull() ?: notification.id.hashCode().toLong()
        val androidNotifId = (notificationIdLong and Int.MAX_VALUE.toLong()).toInt()

        val fullScreenIntent = Intent(context, CriticalAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("message", notification.message)
            putExtra("event_id", notification.eventId) // eventId es String
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            (REQUEST_CODE_OPEN + 1) + androidNotifId,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_CRITICAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🚨 Alerta Crítica de Chapelotas")
            .setContentText(notification.message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)

        postNotification(androidNotifId, builder.build())
    }

    private fun postNotification(id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w("NotificationRepo", "Permiso POST_NOTIFICATIONS no concedido.")
                return
            }
        }
        notificationManager.notify(id, notification)
    }

    private fun createActionIntent(androidNotifId: Int, notification: ChapelotasNotification, action: NotificationAction): PendingIntent {
        val intentActionString = when (action) {
            NotificationAction.ACKNOWLEDGE -> NotificationActionReceiver.ACTION_ACKNOWLEDGE
            NotificationAction.FINISH_DONE -> NotificationActionReceiver.ACTION_FINISH_DONE
        }

        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = intentActionString
            putExtra(NotificationActionReceiver.EXTRA_EVENT_ID, notification.eventId)
            putExtra(NotificationActionReceiver.EXTRA_ANDROID_NOTIF_ID, androidNotifId)
        }

        val requestCode = (androidNotifId + action.ordinal)

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        try {
            val alarmAudioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val customSoundUri: Uri? = try {
                Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.chapelotas_alert}")
            } catch (e: Exception) {
                Log.e("NotificationRepo", "No se pudo encontrar el sonido personalizado", e)
                null
            }

            val criticalChannel = NotificationChannel(
                CHANNEL_CRITICAL, "Alertas Críticas", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Eventos urgentes que no podés perderte."
                setSound(customSoundUri, alarmAudioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500)
                setBypassDnd(true)
            }

            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL, "Recordatorios Generales", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Recordatorios de eventos y resúmenes diarios."
                setSound(customSoundUri, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
            }

            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE, "Servicio Chapelotas", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene a Chapelotas funcionando en segundo plano."
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannels(listOf(generalChannel, criticalChannel, serviceChannel))

        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error al crear canales de notificación", e)
        }
    }

    override suspend fun scheduleNotification(notification: ChapelotasNotification) {
        Log.d("NotificationRepo", "scheduleNotification: ${notification.message}")
    }

    override suspend fun cancelNotification(notificationId: String) {
        try {
            val intId = notificationId.toLongOrNull()?.toInt() ?: notificationId.hashCode()
            notificationManager.cancel(intId)
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error al cancelar notificación", e)
        }
    }

    override suspend fun isNotificationServiceRunning(): Boolean {
        return true
    }

    override suspend fun startNotificationService() {
        val intent = Intent(context, ChapelotasNotificationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}