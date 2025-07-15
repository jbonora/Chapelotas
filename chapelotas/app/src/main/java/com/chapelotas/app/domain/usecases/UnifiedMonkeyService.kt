package com.chapelotas.app.domain.usecases

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.chapelotas.app.data.database.ChapelotasDatabase
import com.chapelotas.app.data.database.entities.EventPlan
import com.chapelotas.app.data.notifications.NotificationAlarmReceiver
import com.chapelotas.app.domain.events.ChapelotasEvent
import com.chapelotas.app.domain.events.ChapelotasEventBus
import com.chapelotas.app.presentation.ui.CriticalAlertActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UnifiedMonkeyService - Ahora usando MonkeyAgenda
 * Mucho más simple y sin loops
 */
@Singleton
class UnifiedMonkeyService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: ChapelotasDatabase,
    private val eventBus: ChapelotasEventBus,
    private val monkeyAgendaService: MonkeyAgendaService
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val scope = CoroutineScope(SupervisorJob())

    companion object {
        private const val TAG = "UnifiedMonkey"
        private const val ALARM_REQUEST_CODE = 1337
        private const val CRITICAL_ALARM_BASE_CODE = 2000
    }

    /**
     * Planifica notificaciones para un evento en la MonkeyAgenda
     */
    suspend fun planNotificationsForEvent(event: EventPlan) {
        // No programar para eventos completados
        if (event.resolutionStatus.name == "COMPLETED") {
            Log.d(TAG, "Evento '${event.title}' está completado. No se programarán notificaciones.")
            return
        }

        // Cancelar notificaciones anteriores del evento
        database.monkeyAgendaDao().cancelPendingForEvent(event.eventId)

        val now = LocalDateTime.now(ZoneId.systemDefault())

        if (event.isCritical) {
            // Eventos críticos: programar llamadas directas
            scheduleCriticalEventCalls(event)
        } else {
            // Eventos normales: programar en MonkeyAgenda
            val notificationTimes = event.getNotificationMinutesList()

            notificationTimes.forEach { minutesBefore ->
                val notificationTime = event.startTime.minusMinutes(minutesBefore.toLong())

                if (notificationTime.isAfter(now)) {
                    monkeyAgendaService.scheduleAction(
                        scheduledTime = notificationTime,
                        actionType = "NOTIFY_EVENT",
                        eventId = event.eventId
                    )

                    Log.d(TAG, "Programada notificación para ${event.title} a las $notificationTime")
                }
            }
        }

        // Reprogramar alarma
        scheduleNextAlarm()
    }

    /**
     * Programa llamadas críticas (siguen usando AlarmManager directo)
     */
    private fun scheduleCriticalEventCalls(event: EventPlan) {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val criticalTimes = listOf(60, 30, 15, 5) // minutos antes

        criticalTimes.forEach { minutesBefore ->
            val callTime = event.startTime.minusMinutes(minutesBefore.toLong())

            if (callTime.isAfter(now)) {
                scheduleCriticalCall(event, minutesBefore, callTime)
            }
        }
    }

    private fun scheduleCriticalCall(event: EventPlan, minutesBefore: Int, callTime: LocalDateTime) {
        val alarmTimeMillis = callTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        Log.d(TAG, "🚨 Programando llamada crítica para '${event.title}' - $minutesBefore min antes")

        val callIntent = Intent(context, CriticalAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                    Intent.FLAG_FROM_BACKGROUND

            val message = when (minutesBefore) {
                60 -> "⏰ ALERTA CRÍTICA\n\n${event.title}\nen 1 HORA"
                30 -> "🚨 ATENCIÓN URGENTE\n\n${event.title}\nen 30 MINUTOS"
                15 -> "⚠️ MUY IMPORTANTE\n\n${event.title}\nen 15 MINUTOS"
                5 -> "🔴 URGENTE AHORA\n\n${event.title}\nen 5 MINUTOS"
                else -> "🚨 EVENTO CRÍTICO\n\n${event.title}"
            }

            putExtra("message", message)
            putExtra("event_id", event.eventId)
            putExtra("notification_id", "${event.eventId}_critical_$minutesBefore")

            event.location?.let {
                putExtra("location", "📍 $it")
            }
        }

        val requestCode = CRITICAL_ALARM_BASE_CODE + "${event.eventId}_$minutesBefore".hashCode()
        val callPending = PendingIntent.getActivity(
            context,
            requestCode,
            callIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val alarmClockInfo = AlarmManager.AlarmClockInfo(
                alarmTimeMillis,
                callPending
            )
            alarmManager.setAlarmClock(alarmClockInfo, callPending)
            Log.d(TAG, "✅ Llamada crítica programada con setAlarmClock")
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarmTimeMillis,
                callPending
            )
        }
    }

    /**
     * Procesa notificaciones y reprograma
     * AHORA USA MonkeyAgenda
     */
    fun processNotificationsAndReschedule() {
        scope.launch {
            Log.d(TAG, "🟢 Procesando acciones de MonkeyAgenda")

            // Delegar al MonkeyAgendaService
            monkeyAgendaService.processNextAction()

            // Reprogramar siguiente alarma
            scheduleNextAlarm()
        }
    }

    /**
     * Programa la siguiente alarma basada en MonkeyAgenda
     */
    suspend fun scheduleNextAlarm() {
        // Obtener próxima acción pendiente
        val nextTime = monkeyAgendaService.getNextPendingTime()

        if (nextTime != null) {
            setExactAlarm(nextTime)
        } else {
            // Si no hay nada pendiente, programar chequeo en 1 hora
            val nextCheckTime = LocalDateTime.now(ZoneId.systemDefault()).plusHours(1)

            // Programar chequeo de inactividad
            monkeyAgendaService.scheduleAction(
                scheduledTime = nextCheckTime,
                actionType = "IDLE_CHECK"
            )

            setExactAlarm(nextCheckTime)
        }
    }

    private fun setExactAlarm(triggerTime: LocalDateTime) {
        cancelAlarm()

        val now = LocalDateTime.now(ZoneId.systemDefault())

        // PROTECCIÓN ANTI-LOOP
        val minFutureTime = now.plusMinutes(1)
        val actualTriggerTime = if (triggerTime.isBefore(minFutureTime)) {
            Log.w(TAG, "⚠️ Ajustando alarma de $triggerTime a $minFutureTime para evitar loop")
            minFutureTime
        } else {
            triggerTime
        }

        val triggerAtMillis = actualTriggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        Log.d(TAG, "⏰ Próxima alarma programada para: $actualTriggerTime")

        val intent = Intent(context, NotificationAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }

            scope.launch {
                database.dayPlanDao().updateNextAlarmTime(LocalDate.now(), actualTriggerTime)
                eventBus.emit(ChapelotasEvent.MonkeyCheckCompleted(0, 0, actualTriggerTime))
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "Error programando alarma. ¿Falta permiso SCHEDULE_EXACT_ALARM?", e)
        }
    }

    private fun cancelAlarm() {
        val intent = Intent(context, NotificationAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}