package com.chapelotas.app.battery

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.chapelotas.app.data.notifications.PrewarmReceiver
import com.chapelotas.app.domain.debug.DebugLog
import com.chapelotas.app.domain.models.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestiona las estrategias específicas para despertar la app en dispositivos Huawei,
 * que son conocidos por su agresivo manejo de la batería.
 */
@Singleton
class HuaweiWakeUpManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val debugLog: DebugLog
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Si el dispositivo es Huawei, programa alarmas de "pre-calentamiento" 10 y 5 minutos
     * antes de la alarma principal para despertar el sistema.
     */
    fun schedulePrewarmingIfNecessary(task: Task, alarmTime: LocalDateTime?) {
        if (alarmTime == null || !HuaweiUtils.isHuaweiDevice()) {
            return
        }

        val alarmTimeZoned = ZonedDateTime.of(alarmTime, ZoneId.systemDefault())

        // Programar una alarma 10 minutos antes
        val prewarmTime10Mins = alarmTimeZoned.minusMinutes(10)
        schedulePrewarmAlarm(task.id + "_10m", prewarmTime10Mins)

        // Programar una alarma de respaldo 5 minutos antes
        val prewarmTime5Mins = alarmTimeZoned.minusMinutes(5)
        schedulePrewarmAlarm(task.id + "_5m", prewarmTime5Mins)
    }

    private fun schedulePrewarmAlarm(uniqueId: String, time: ZonedDateTime) {
        val intent = Intent(context, PrewarmReceiver::class.java).apply {
            putExtra("task_id_for_log", uniqueId)
            putExtra("is_prewarm", true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            "prewarm_$uniqueId".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                time.toInstant().toEpochMilli(),
                pendingIntent
            )
            debugLog.add("🔥 HUAWEI_WAKEUP: Pre-calentamiento programado para $uniqueId a las ${time.toLocalTime()}")
        } catch (e: SecurityException) {
            debugLog.add("🔥 HUAWEI_WAKEUP: Error programando pre-calentamiento para $uniqueId")
        }
    }
}