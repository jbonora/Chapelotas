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
import com.chapelotas.app.di.Constants
import com.chapelotas.app.domain.debug.DebugLog
import com.chapelotas.app.domain.entities.ChapelotasNotification
import com.chapelotas.app.domain.entities.NotificationAction
import com.chapelotas.app.domain.entities.NotificationType
import com.chapelotas.app.domain.repositories.NotificationRepository
import com.chapelotas.app.presentation.ui.CriticalAlertActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val debugLog: DebugLog // AGREGADO: Inyección de DebugLog
) : NotificationRepository {

    private val notificationManager = NotificationManagerCompat.from(context)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss") // Para logs más precisos

    init {
        createNotificationChannels()
    }

    override suspend fun showImmediateNotification(notification: ChapelotasNotification) {
        debugLog.add("📬 NOTIF: Mostrando notificación inmediata - ID: ${notification.id}, Mensaje: ${notification.message}")

        if (notification.type == NotificationType.CRITICAL_ALERT) {
            showCriticalAlert(notification)
        } else {
            showStandardNotification(notification)
        }
    }

    override fun scheduleExactReminder(taskId: String, triggerAt: LocalDateTime) {
        // AGREGADO: Log detallado al inicio
        val formattedTime = triggerAt.format(timeFormatter)
        debugLog.add("📅 ALARM: Programando recordatorio para tarea '$taskId' a las $formattedTime")

        val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
            action = Constants.ACTION_TRIGGER_REMINDER
            putExtra(Constants.EXTRA_TASK_ID, taskId)
        }

        val alarmRequestCode = taskId.hashCode()
        debugLog.add("📅 ALARM: RequestCode para '$taskId': $alarmRequestCode")

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = triggerAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val minutesFromNow = (triggerAtMillis - System.currentTimeMillis()) / 60000
        debugLog.add("📅 ALARM: Alarma se disparará en $minutesFromNow minutos")

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                debugLog.add("✅ ALARM: Alarma EXACTA programada exitosamente para '$taskId'")
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                debugLog.add("⚠️ ALARM: Alarma NO exacta programada para '$taskId' (sin permisos exactos)")
            }
        } catch (e: SecurityException) {
            debugLog.add("❌ ALARM: ERROR de seguridad al programar alarma para '$taskId': ${e.message}")
            Log.e("NotificationRepo", "Error de seguridad al programar alarma exacta.", e)
        } catch (e: Exception) {
            debugLog.add("❌ ALARM: ERROR inesperado al programar alarma para '$taskId': ${e.message}")
            Log.e("NotificationRepo", "Error inesperado al programar alarma.", e)
        }
    }

    override fun cancelReminder(taskId: String) {
        debugLog.add("🗑️ ALARM: Cancelando recordatorio para tarea '$taskId'")

        val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
            action = Constants.ACTION_TRIGGER_REMINDER
            putExtra(Constants.EXTRA_TASK_ID, taskId)
        }

        val alarmRequestCode = taskId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmRequestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            debugLog.add("✅ ALARM: Recordatorio cancelado exitosamente para '$taskId'")
        } else {
            debugLog.add("ℹ️ ALARM: No había recordatorio activo para '$taskId'")
        }
    }

    override fun cancelTaskNotification(taskId: String) {
        val androidNotifId = taskId.hashCode()
        notificationManager.cancel(androidNotifId)
        debugLog.add("🗑️ NOTIF: Notificación cancelada para la tarea '$taskId' (ID: $androidNotifId)")
    }

    private fun showCriticalAlert(notification: ChapelotasNotification) {
        debugLog.add("🚨 NOTIF: Mostrando alerta CRÍTICA")

        val androidNotifId = notification.id.hashCode()
        val fullScreenIntent = Intent(context, CriticalAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(Constants.EXTRA_NOTIFICATION_MESSAGE, notification.message)
            putExtra(Constants.EXTRA_EVENT_ID, notification.eventId)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            Constants.REQUEST_CODE_CRITICAL_ALERT + androidNotifId,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, Constants.CHANNEL_ID_CRITICAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_critical_alert_title))
            .setContentText(notification.message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)

        postNotification(androidNotifId, builder.build())
    }

    private fun showStandardNotification(notification: ChapelotasNotification) {
        val androidNotifId = notification.eventId.hashCode()
        debugLog.add("📢 NOTIF: Mostrando notificación estándar - Android ID: $androidNotifId, Canal: ${notification.channelId}")

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            action = "${Constants.ACTION_SHOW_TASK_DETAILS}_${notification.eventId}"
            putExtra(Constants.EXTRA_TASK_ID_TO_HIGHLIGHT, notification.eventId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            Constants.REQUEST_CODE_OPEN_APP + androidNotifId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, notification.channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_general_title))
            .setContentText(notification.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)

        notification.actions.forEach { action ->
            debugLog.add("📢 NOTIF: Agregando acción: $action")
            builder.addAction(createAction(androidNotifId, notification.eventId, action))
        }

        postNotification(androidNotifId, builder.build())
    }

    private fun createAction(androidNotifId: Int, eventId: String, action: NotificationAction): NotificationCompat.Action {
        val intentActionString = when (action) {
            NotificationAction.ACKNOWLEDGE -> Constants.ACTION_ACKNOWLEDGE
            NotificationAction.FINISH_DONE -> Constants.ACTION_FINISH_DONE
        }

        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = intentActionString
            putExtra(Constants.EXTRA_EVENT_ID, eventId)
            putExtra(Constants.EXTRA_ANDROID_NOTIF_ID, androidNotifId)
        }

        val requestCode = (androidNotifId + action.ordinal)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val actionText = when (action) {
            NotificationAction.ACKNOWLEDGE -> context.getString(R.string.notification_action_acknowledge)
            NotificationAction.FINISH_DONE -> context.getString(R.string.notification_action_finish_done)
        }

        val icon = when (action) {
            NotificationAction.FINISH_DONE -> R.drawable.ic_check
            else -> R.drawable.ic_snooze
        }

        return NotificationCompat.Action(icon, actionText, pendingIntent)
    }

    private fun postNotification(id: Int, notification: Notification) {
        // AGREGADO: Log antes de verificar permisos
        debugLog.add("📮 NOTIF: Intentando publicar notificación con ID: $id")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                debugLog.add("❌ NOTIF: Sin permiso POST_NOTIFICATIONS - notificación bloqueada")
                return
            }
        }

        try {
            notificationManager.notify(id, notification)
            debugLog.add("✅ NOTIF: Notificación publicada exitosamente con ID: $id")
        } catch (e: Exception) {
            debugLog.add("❌ NOTIF: Error al publicar notificación: ${e.message}")
            Log.e("NotificationRepo", "Error al publicar notificación", e)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        debugLog.add("🔧 NOTIF: Creando canales de notificación")

        try {
            fun getSoundUri(soundResId: Int): Uri? {
                return try {
                    Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/$soundResId")
                } catch (e: Exception) {
                    debugLog.add("⚠️ NOTIF: No se pudo crear URI de sonido para recurso $soundResId")
                    null
                }
            }

            val alarmAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val soundAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val criticalChannel = NotificationChannel(
                Constants.CHANNEL_ID_CRITICAL,
                "Alertas Críticas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Eventos urgentes que no podés perderte."
                setSound(getSoundUri(R.raw.chapelotas_alert), alarmAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500)
                setBypassDnd(true)
            }

            val generalChannel = NotificationChannel(
                Constants.CHANNEL_ID_GENERAL,
                "Recordatorios Generales",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Recordatorios de eventos y avisos principales."
                setSound(getSoundUri(R.raw.chapelotas_alert), soundAttributes)
                enableVibration(true)
            }

            val insistenceNormal = NotificationChannel(
                Constants.CHANNEL_ID_INSISTENCE_NORMAL,
                "Insistencia (Normal)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de seguimiento con el sonido de alerta principal."
                setSound(getSoundUri(R.raw.alerta_insistencia_fuerte), soundAttributes)
                enableVibration(true)
            }

            val insistenceMedium = NotificationChannel(
                Constants.CHANNEL_ID_INSISTENCE_MEDIUM,
                "Insistencia (Media)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de seguimiento con sonido moderado."
                setSound(getSoundUri(R.raw.alerta_insistencia_media), soundAttributes)
                enableVibration(true)
            }

            val insistenceLow = NotificationChannel(
                Constants.CHANNEL_ID_INSISTENCE_LOW,
                "Insistencia (Baja)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de seguimiento con sonido bajo."
                setSound(getSoundUri(R.raw.alerta_insistencia_baja), soundAttributes)
                enableVibration(true)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannels(listOf(
                criticalChannel,
                generalChannel,
                insistenceNormal,
                insistenceMedium,
                insistenceLow
            ))

            debugLog.add("✅ NOTIF: Canales de notificación creados exitosamente")

        } catch (e: Exception) {
            debugLog.add("❌ NOTIF: Error al crear canales de notificación: ${e.message}")
            Log.e("NotificationRepo", "Error al crear canales de notificación", e)
        }
    }
}