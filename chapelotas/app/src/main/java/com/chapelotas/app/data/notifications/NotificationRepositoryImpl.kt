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
import com.chapelotas.app.domain.entities.NotificationPriority
import com.chapelotas.app.domain.entities.NotificationType
import com.chapelotas.app.domain.repositories.NotificationRepository
import com.chapelotas.app.presentation.ui.CriticalAlertActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationRepository {

    private val notificationManager = NotificationManagerCompat.from(context)
    private val _notificationsFlow = MutableStateFlow<List<ChapelotasNotification>>(emptyList())

    companion object {
        const val CHANNEL_GENERAL = "chapelotas_general"
        const val CHANNEL_CRITICAL = "chapelotas_critical"
        const val CHANNEL_SERVICE = "chapelotas_service"
        const val NOTIFICATION_DATA_KEY = "notification_data"
        private const val REQUEST_CODE_OPEN = 1000
        private const val REQUEST_CODE_SNOOZE = 2000
        private const val REQUEST_CODE_DISMISS = 3000
        private const val REQUEST_CODE_FULL_SCREEN = 4000
    }

    init {
        createNotificationChannels()
    }

    override suspend fun showImmediateNotification(notification: ChapelotasNotification) {
        when (notification.type) {
            NotificationType.CRITICAL_ALERT -> showCriticalAlert(notification)
            else -> showStandardNotification(notification)
        }
        markAsShown(notification.id)
    }

    private fun showStandardNotification(notification: ChapelotasNotification) {
        val channelId = if (notification.priority == NotificationPriority.CRITICAL) CHANNEL_CRITICAL else CHANNEL_GENERAL
        val notificationIdLong = notification.id.toLongOrNull() ?: notification.id.hashCode().toLong()
        val androidNotifId = (notificationIdLong and Int.MAX_VALUE.toLong()).toInt()

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigation_route", "today")
            putExtra("notification_id", notificationIdLong)
        }
        val openAppPendingIntent = PendingIntent.getActivity(context, REQUEST_CODE_OPEN + androidNotifId, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val snoozeIntent = createActionIntent(androidNotifId, notification, NotificationActionReceiver.ACTION_SNOOZE)
        val dismissIntent = createActionIntent(androidNotifId, notification, NotificationActionReceiver.ACTION_DISMISS)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Chapelotas tiene un mensaje")
            .setContentText(notification.message.lines().firstOrNull() ?: "Toca para ver los detalles.")
            .setSubText("Chapelotas")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_snooze, "Posponer 15min", snoozeIntent)
            .addAction(R.drawable.ic_check, "Listo", dismissIntent)

        postNotification(androidNotifId, builder.build())
    }

    private fun showCriticalAlert(notification: ChapelotasNotification) {
        val notificationIdLong = notification.id.toLongOrNull() ?: notification.id.hashCode().toLong()
        val androidNotifId = (notificationIdLong and Int.MAX_VALUE.toLong()).toInt()

        val fullScreenIntent = Intent(context, CriticalAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("message", notification.message)
            putExtra("event_id", notification.eventId)
            putExtra("notification_id", notificationIdLong)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_FULL_SCREEN + androidNotifId,
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
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(id, notification)
            } else {
                Log.w("NotificationRepo", "Permiso POST_NOTIFICATIONS no concedido. Notificación suprimida.")
            }
        } else {
            notificationManager.notify(id, notification)
        }
    }

    private fun createActionIntent(androidNotifId: Int, notification: ChapelotasNotification, action: String): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notification.id.toLongOrNull() ?: 0L)
            putExtra(NotificationActionReceiver.EXTRA_EVENT_ID, notification.eventId.toString())
            if (action == NotificationActionReceiver.ACTION_SNOOZE) {
                putExtra(NotificationActionReceiver.EXTRA_SNOOZE_MINUTES, 15)
            }
        }
        val requestCode = when(action) {
            NotificationActionReceiver.ACTION_SNOOZE -> REQUEST_CODE_SNOOZE
            else -> REQUEST_CODE_DISMISS
        }
        return PendingIntent.getBroadcast(context, requestCode + androidNotifId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val customSoundUri: Uri? = try {
                Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.chapelotas_alert}")
            } catch (e: Exception) {
                Log.e("NotificationRepo", "No se pudo encontrar el sonido personalizado 'chapelotas_alert'. Usando sonido por defecto.", e)
                null
            }

            val criticalChannel = NotificationChannel(
                CHANNEL_CRITICAL, "Alertas Críticas", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Eventos urgentes que no podés perderte. Se mostrarán en pantalla completa."
                setSound(customSoundUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500)
                setBypassDnd(true)
            }

            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL, "Recordatorios Generales", NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Recordatorios de eventos y resúmenes diarios."
                setSound(customSoundUri, audioAttributes)
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
            Log.e("NotificationRepo", "FALLO CRÍTICO al crear canales de notificación. La app no crasheará, pero las notificaciones pueden no funcionar como se espera.", e)
        }
    }

    override suspend fun scheduleNotification(notification: ChapelotasNotification) {
        updateNotificationsList(notification)
    }

    override suspend fun scheduleMultipleNotifications(notifications: List<ChapelotasNotification>) {
        notifications.forEach { updateNotificationsList(it) }
    }

    override suspend fun cancelNotification(notificationId: String) {
        try {
            val intId = notificationId.toLongOrNull()?.toInt() ?: notificationId.hashCode()
            notificationManager.cancel(intId)
            removeFromNotificationsList(notificationId)
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error al cancelar notificación", e)
        }
    }

    override suspend fun cancelNotificationsForEvent(eventId: Long) {
        notificationManager.cancel(eventId.toInt())
    }

    override suspend fun getPendingNotifications(): List<ChapelotasNotification> {
        return _notificationsFlow.value.filter { !it.hasBeenShown }
    }

    override suspend fun getNotificationsForEvent(eventId: Long): List<ChapelotasNotification> {
        return _notificationsFlow.value.filter { it.eventId == eventId }
    }

    override suspend fun markAsShown(notificationId: String) {
        val updated = _notificationsFlow.value.map { notification ->
            if (notification.id == notificationId) {
                notification.copy(hasBeenShown = true)
            } else {
                notification
            }
        }
        _notificationsFlow.value = updated
    }

    override suspend fun cleanOldNotifications(daysToKeep: Int) {
        val cutoffDate = LocalDateTime.now(ZoneId.systemDefault()).minusDays(daysToKeep.toLong())
        val filtered = _notificationsFlow.value.filter { notification ->
            !notification.hasBeenShown || notification.createdAt.isAfter(cutoffDate)
        }
        _notificationsFlow.value = filtered
    }

    override fun observeNotifications(): Flow<List<ChapelotasNotification>> {
        return _notificationsFlow.asStateFlow()
    }

    override suspend fun isNotificationServiceRunning(): Boolean {
        // Esta es una simplificación. Una implementación real requeriría un mecanismo
        // más complejo para verificar si el servicio está realmente activo.
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

    override suspend fun stopNotificationService() {
        val intent = Intent(context, ChapelotasNotificationService::class.java)
        context.stopService(intent)
    }

    private fun updateNotificationsList(notification: ChapelotasNotification) {
        _notificationsFlow.value = (_notificationsFlow.value + notification).distinctBy { it.id }
    }

    private fun removeFromNotificationsList(notificationId: String) {
        _notificationsFlow.value = _notificationsFlow.value.filter { it.id != notificationId }
    }
}