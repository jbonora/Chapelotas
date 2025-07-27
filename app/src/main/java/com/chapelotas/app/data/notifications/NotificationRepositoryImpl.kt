package com.chapelotas.app.data.notifications

import android.Manifest
import android.app.AlarmManager
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
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationRepository {

    private val notificationManager = NotificationManagerCompat.from(context)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val CHANNEL_GENERAL = "chapelotas_general"
        const val CHANNEL_CRITICAL = "chapelotas_critical"
        private const val REQUEST_CODE_OPEN = 1000
    }

    init {
        createNotificationChannels()
    }

    override suspend fun showImmediateNotification(notification: ChapelotasNotification) {
        if (notification.type == NotificationType.CRITICAL_ALERT) {
            showCriticalAlert(notification)
        } else {
            showStandardNotification(notification)
        }
    }

    override fun scheduleExactReminder(taskId: String, triggerAt: LocalDateTime) {
        val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
            action = NotificationAlarmReceiver.ACTION_TRIGGER_REMINDER
            putExtra(NotificationAlarmReceiver.EXTRA_TASK_ID, taskId)
        }

        // Se usa el hashcode del ID de la tarea como un ID único para el PendingIntent de la alarma.
        val alarmRequestCode = taskId.hashCode()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = triggerAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Se utiliza setExactAndAllowWhileIdle para asegurar que la alarma se dispare incluso en modo Doze.
        try {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.d("NotificationRepo", "⏰ Alarma exacta programada para la tarea $taskId a las $triggerAt")
            } else {
                Log.w("NotificationRepo", "Permiso para alarmas exactas no concedido. Usando alarma inexacta como fallback.")
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.e("NotificationRepo", "Error de seguridad al programar alarma exacta.", e)
        }
    }

    override fun cancelReminder(taskId: String) {
        val intent = Intent(context, NotificationAlarmReceiver::class.java)
        val alarmRequestCode = taskId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmRequestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            Log.d("NotificationRepo", "🚫 Alarma cancelada para la tarea $taskId")
        }
    }

    private fun showCriticalAlert(notification: ChapelotasNotification) {
        val androidNotifId = notification.id.hashCode()

        val fullScreenIntent = Intent(context, CriticalAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("message", notification.message)
            putExtra("event_id", notification.eventId)
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

    private fun showStandardNotification(notification: ChapelotasNotification) {
        val androidNotifId = notification.eventId.hashCode()

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigation_route", "today")
            putExtra("notification_id", androidNotifId)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN + androidNotifId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_GENERAL)
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

        notification.actions.forEach { action ->
            builder.addAction(createAction(androidNotifId, notification.eventId, action))
        }

        postNotification(androidNotifId, builder.build())
    }

    private fun createAction(androidNotifId: Int, eventId: String, action: NotificationAction): NotificationCompat.Action {
        val intentActionString = when (action) {
            NotificationAction.ACKNOWLEDGE -> NotificationActionReceiver.ACTION_ACKNOWLEDGE
            NotificationAction.FINISH_DONE -> NotificationActionReceiver.ACTION_FINISH_DONE
        }

        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = intentActionString
            putExtra(NotificationActionReceiver.EXTRA_EVENT_ID, eventId)
            putExtra(NotificationActionReceiver.EXTRA_ANDROID_NOTIF_ID, androidNotifId)
        }

        val requestCode = (androidNotifId + action.ordinal)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val actionText = when (action) {
            NotificationAction.ACKNOWLEDGE -> "Entendido"
            NotificationAction.FINISH_DONE -> "Done"
        }
        val icon = when (action) {
            NotificationAction.FINISH_DONE -> R.drawable.ic_check
            else -> R.drawable.ic_snooze
        }

        return NotificationCompat.Action(icon, actionText, pendingIntent)
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

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannels(listOf(generalChannel, criticalChannel))

        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error al crear canales de notificación", e)
        }
    }
}