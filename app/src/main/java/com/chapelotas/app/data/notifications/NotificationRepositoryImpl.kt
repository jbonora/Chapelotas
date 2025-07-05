package com.chapelotas.app.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.chapelotas.app.R
import com.chapelotas.app.domain.entities.ChapelotasNotification
import com.chapelotas.app.domain.entities.NotificationPriority
import com.chapelotas.app.domain.entities.NotificationType
import com.chapelotas.app.domain.repositories.NotificationRepository
import com.chapelotas.app.MainActivity
import com.chapelotas.app.presentation.ui.CriticalAlertActivity
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación del repositorio de notificaciones
 * Usa WorkManager para programar y Foreground Service para persistencia
 */
@Singleton
class NotificationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationRepository {

    private val workManager = WorkManager.getInstance(context)
    private val notificationManager = NotificationManagerCompat.from(context)
    private val gson = Gson()

    private val _notificationsFlow = MutableStateFlow<List<ChapelotasNotification>>(emptyList())

    companion object {
        // Notification Channels
        const val CHANNEL_GENERAL = "chapelotas_general"
        const val CHANNEL_CRITICAL = "chapelotas_critical"
        const val CHANNEL_SERVICE = "chapelotas_service"

        // Tags
        const val WORK_TAG_PREFIX = "chapelotas_notification_"
        const val NOTIFICATION_DATA_KEY = "notification_data"

        // IDs
        const val FOREGROUND_SERVICE_ID = 1337
        private var notificationIdCounter = 2000
    }

    init {
        createNotificationChannels()
    }

    override suspend fun scheduleNotification(notification: ChapelotasNotification) {
        val delay = calculateDelay(notification.scheduledTime)

        if (delay < 0) {
            // Si ya pasó la hora, mostrar inmediatamente
            showImmediateNotification(notification)
            return
        }

        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    NOTIFICATION_DATA_KEY to gson.toJson(notification)
                )
            )
            .addTag(WORK_TAG_PREFIX + notification.id)
            .build()

        workManager.enqueue(workRequest)
        updateNotificationsList(notification)
    }

    override suspend fun scheduleMultipleNotifications(notifications: List<ChapelotasNotification>) {
        notifications.forEach { scheduleNotification(it) }
    }

    override suspend fun cancelNotification(notificationId: String) {
        workManager.cancelAllWorkByTag(WORK_TAG_PREFIX + notificationId)
        removeFromNotificationsList(notificationId)
    }

    override suspend fun cancelNotificationsForEvent(eventId: Long) {
        // Obtener todas las notificaciones del evento
        val notifications = _notificationsFlow.value.filter { it.eventId == eventId }
        notifications.forEach { cancelNotification(it.id) }
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
        val cutoffDate = LocalDateTime.now().minusDays(daysToKeep.toLong())
        val filtered = _notificationsFlow.value.filter { notification ->
            !notification.hasBeenShown || notification.createdAt.isAfter(cutoffDate)
        }
        _notificationsFlow.value = filtered
    }

    override fun observeNotifications(): Flow<List<ChapelotasNotification>> {
        return _notificationsFlow.asStateFlow()
    }

    override suspend fun showImmediateNotification(notification: ChapelotasNotification) {
        when (notification.type) {
            NotificationType.CRITICAL_ALERT -> {
                // Las críticas siguen usando la pantalla completa
                showCriticalAlert(notification)
            }
            else -> {
                // Todas las demás usan estilo Calendar
                showCalendarStyleNotification(notification)
            }
        }
    }

    override suspend fun isNotificationServiceRunning(): Boolean {
        // En una implementación real, verificaríamos el estado del Foreground Service
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

    /**
     * Notificación estilo Google Calendar
     */
    private fun showCalendarStyleNotification(notification: ChapelotasNotification) {
        val channelId = when (notification.priority) {
            NotificationPriority.HIGH, NotificationPriority.CRITICAL -> CHANNEL_CRITICAL
            else -> CHANNEL_GENERAL
        }

        // Extraer información del mensaje
        val lines = notification.message.lines()
        val titulo = lines.firstOrNull() ?: "Recordatorio"

        // Calcular tiempo relativo
        val now = LocalDateTime.now()
        val minutesUntil = Duration.between(now, notification.scheduledTime).toMinutes()

        val timeText = when {
            minutesUntil <= 0 -> "ahora"
            minutesUntil < 60 -> "en $minutesUntil min"
            else -> "en ${minutesUntil / 60}h ${minutesUntil % 60}min"
        }

        // Intent para abrir la app
        val openAppIntent = PendingIntent.getActivity(
            context,
            notification.id.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent para posponer
        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            notification.id.hashCode() + 1,
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = "SNOOZE"
                putExtra("notification_id", notification.id)
                putExtra("event_id", notification.eventId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent para descartar
        val dismissIntent = PendingIntent.getBroadcast(
            context,
            notification.id.hashCode() + 2,
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = "DISMISS"
                putExtra("notification_id", notification.id)
                putExtra("event_id", notification.eventId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Construir notificación estilo Calendar
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titulo)
            .setContentText(timeText)
            .setSubText("Calendario")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(notification.message)
                    .setSummaryText(timeText)
            )
            .setPriority(
                when (notification.priority) {
                    NotificationPriority.CRITICAL, NotificationPriority.HIGH ->
                        NotificationCompat.PRIORITY_HIGH
                    NotificationPriority.NORMAL ->
                        NotificationCompat.PRIORITY_DEFAULT
                    NotificationPriority.LOW ->
                        NotificationCompat.PRIORITY_LOW
                }
            )
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setContentIntent(openAppIntent)
            .addAction(
                android.R.drawable.ic_menu_recent_history,
                "Posponer",
                snoozeIntent
            )
            .addAction(
                android.R.drawable.checkbox_on_background,
                "Listo",
                dismissIntent
            )
            .setVibrate(longArrayOf(0, 200, 100, 200))
            .setLights(android.graphics.Color.BLUE, 1000, 1000)

        // Para Android 8+ agregar badge
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
        }

        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(
                notification.eventId.toInt(),
                builder.build()
            )
        }
    }

    /**
     * Crea los canales de notificación necesarios
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_GENERAL,
                    "Notificaciones Generales",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Recordatorios y resúmenes de Chapelotas"
                    enableVibration(true)
                },

                NotificationChannel(
                    CHANNEL_CRITICAL,
                    "Alertas Críticas",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Eventos que no podés perderte"
                    enableVibration(true)
                    setBypassDnd(true)
                },

                NotificationChannel(
                    CHANNEL_SERVICE,
                    "Servicio de Notificaciones",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Mantiene Chapelotas activo"
                    setShowBadge(false)
                }
            )

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channels.forEach { manager.createNotificationChannel(it) }
        }
    }

    /**
     * Muestra una alerta crítica (pantalla completa)
     */
    private fun showCriticalAlert(notification: ChapelotasNotification) {
        val intent = Intent(context, CriticalAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("message", notification.message)
            putExtra("event_id", notification.eventId)
        }
        context.startActivity(intent)
    }

    /**
     * Calcula el delay para programar la notificación
     */
    private fun calculateDelay(scheduledTime: LocalDateTime): Long {
        return Duration.between(LocalDateTime.now(), scheduledTime).toMillis()
    }

    /**
     * Actualiza la lista de notificaciones en memoria
     */
    private fun updateNotificationsList(notification: ChapelotasNotification) {
        _notificationsFlow.value = _notificationsFlow.value + notification
    }

    /**
     * Remueve una notificación de la lista
     */
    private fun removeFromNotificationsList(notificationId: String) {
        _notificationsFlow.value = _notificationsFlow.value.filter { it.id != notificationId }
    }
}