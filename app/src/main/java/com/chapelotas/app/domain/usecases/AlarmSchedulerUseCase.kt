package com.chapelotas.app.domain.usecases

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.content.getSystemService
import com.chapelotas.app.MainActivity
import com.chapelotas.app.data.alarms.AlarmReceiver
import com.chapelotas.app.data.alarms.HuaweiIntermediateAlarmReceiver
import com.chapelotas.app.domain.debug.DebugLog
import com.chapelotas.app.domain.events.AlarmEvent
import com.chapelotas.app.domain.events.EventBus
import com.chapelotas.app.domain.models.AppSettings
import com.chapelotas.app.domain.repositories.PreferencesRepository
import com.chapelotas.app.domain.repositories.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmSchedulerUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskRepository: TaskRepository,
    private val preferencesRepository: PreferencesRepository,
    private val debugLog: DebugLog,
    private val eventBus: EventBus
) {
    private val alarmManager = context.getSystemService<AlarmManager>()
    private val useCaseScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)


    companion object {
        private val DEFAULT_WORK_START_TIME = LocalTime.of(8, 0)
        const val ALARM_REQUEST_CODE = 4242
        const val INTERMEDIATE_ALARM_REQUEST_CODE = 4244
        private const val HUAWEI_MAX_ALARM_MINUTES = 270 // 4.5 horas
        private val HUAWEI_MANUFACTURERS = listOf("huawei", "honor")
    }

    private fun isHuaweiDevice(): Boolean {
        return Build.MANUFACTURER.lowercase() in HUAWEI_MANUFACTURERS
    }

    suspend fun scheduleNextAlarm() {
        debugLog.add("SCHEDULER: ==> Iniciando scheduleNextAlarm()")
        if (alarmManager == null) {
            debugLog.add("SCHEDULER: ❌ ERROR - AlarmManager no está disponible.")
            return
        }

        val settings = preferencesRepository.observeAppSettings().first()
        if (!settings.autoCreateAlarm) {
            debugLog.add("SCHEDULER: 🛑 Función desactivada. Cancelando alarmas existentes.")
            cancelPreviousAlarm()
            return
        }

        val today = LocalDate.now()
        val tasksToday = taskRepository.observeTasksForDate(today).first()
        val tasksTomorrow = taskRepository.observeTasksForDate(today.plusDays(1)).first()

        val allUpcomingTasks = (tasksToday + tasksTomorrow)
            .filter { it.scheduledTime.isAfter(LocalDateTime.now()) }
            .sortedBy { it.scheduledTime }

        val workStartTime = try {
            LocalTime.parse(settings.workStartTime)
        } catch (e: Exception) {
            DEFAULT_WORK_START_TIME
        }

        val nextTaskTime = allUpcomingTasks.firstOrNull()?.scheduledTime?.toLocalTime()
        val firstEventTime = if (nextTaskTime != null && nextTaskTime.isBefore(workStartTime)) {
            nextTaskTime
        } else {
            workStartTime
        }

        val alarmTime = firstEventTime.minusMinutes(settings.alarmOffsetMinutes.toLong())

        val now = LocalDateTime.now().withSecond(0).withNano(0)
        var targetDateTime = now.toLocalDate().atTime(alarmTime)

        if (targetDateTime.isBefore(now)) {
            targetDateTime = targetDateTime.plusDays(1)
        }

        val minutesUntilAlarm = ChronoUnit.MINUTES.between(now, targetDateTime)

        debugLog.add("SCHEDULER: ⏰ La alarma sonará en ${minutesUntilAlarm} minutos.")

        scheduleAlarm(
            minutesFromNow = minutesUntilAlarm,
            alarmDate = targetDateTime.toLocalDate(),
            alarmTime = targetDateTime.toLocalTime(),
            settings = settings
        )
    }

    suspend fun scheduleAlarm(
        minutesFromNow: Long,
        alarmDate: LocalDate,
        alarmTime: LocalTime,
        settings: AppSettings
    ) {
        if (alarmManager == null || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms())) {
            return
        }

        preferencesRepository.clearFinalHuaweiAlarmTime()

        if (isHuaweiDevice() && minutesFromNow > HUAWEI_MAX_ALARM_MINUTES) {
            scheduleHuaweiIntermediateAlarm(finalAlarmDate = alarmDate, finalAlarmTime = alarmTime)
            return
        }

        cancelPreviousAlarm()

        val triggerTimeMillis = System.currentTimeMillis() - (System.currentTimeMillis() % 60000) + (minutesFromNow * 60 * 1000)

        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_date", alarmDate.toString())
            putExtra("alarm_time", alarmTime.toString())
            putExtra("sarcastic_mode", settings.sarcasticMode)
            putExtra("snooze_minutes", settings.snoozeMinutes)
        }
        val alarmPendingIntent = PendingIntent.getBroadcast(
            context, ALARM_REQUEST_CODE, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context, ALARM_REQUEST_CODE + 1000, showIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTimeMillis, showPendingIntent)

        try {
            alarmManager.setAlarmClock(alarmClockInfo, alarmPendingIntent)
            val message = "Alarma programada para las ${alarmTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            debugLog.add("SCHEDULER: 🚀 ¡ÉXITO! $message")

            eventBus.emit(AlarmEvent.AlarmScheduled(alarmTime.toString(), alarmDate.toString(), ALARM_REQUEST_CODE))
            withContext(Dispatchers.Main) { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
        } catch (e: Exception) {
            debugLog.add("SCHEDULER: ❌ ERROR al programar alarma: ${e.message}")
        }
    }

    private suspend fun scheduleHuaweiIntermediateAlarm(
        finalAlarmDate: LocalDate,
        finalAlarmTime: LocalTime
    ) {
        val finalDateTime = LocalDateTime.of(finalAlarmDate, finalAlarmTime)
        preferencesRepository.setFinalHuaweiAlarmTime(finalDateTime)
        debugLog.add("SCHEDULER: 💾 Guardada hora final de Huawei: $finalDateTime")

        val intermediateMinutes = HUAWEI_MAX_ALARM_MINUTES - 20L
        val triggerTimeMillis = System.currentTimeMillis() + (intermediateMinutes * 60 * 1000)

        val intermediateIntent = Intent(context, HuaweiIntermediateAlarmReceiver::class.java).apply {
            action = "com.chapelotas.app.HUAWEI_INTERMEDIATE_ALARM"
            putExtra("final_alarm_date", finalAlarmDate.toString())
            putExtra("final_alarm_time", finalAlarmTime.toString())
        }

        val intermediatePendingIntent = PendingIntent.getBroadcast(
            context, INTERMEDIATE_ALARM_REQUEST_CODE, intermediateIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        cancelAlarmByRequestCode(INTERMEDIATE_ALARM_REQUEST_CODE)
        alarmManager?.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, intermediatePendingIntent)

        val message = "Alarma (Modo Huawei) programada para las ${finalAlarmTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        withContext(Dispatchers.Main) { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
        eventBus.emit(AlarmEvent.AlarmScheduled(finalAlarmTime.toString(), finalAlarmDate.toString(), ALARM_REQUEST_CODE))
    }

    private fun cancelPreviousAlarm() {
        cancelAlarmByRequestCode(ALARM_REQUEST_CODE)
        if (isHuaweiDevice()) {
            cancelAlarmByRequestCode(INTERMEDIATE_ALARM_REQUEST_CODE)
            useCaseScope.launch {
                preferencesRepository.clearFinalHuaweiAlarmTime()
            }
        }
    }

    private fun cancelAlarmByRequestCode(requestCode: Int) {
        if (alarmManager == null) return
        val intent = when (requestCode) {
            INTERMEDIATE_ALARM_REQUEST_CODE -> Intent(context, HuaweiIntermediateAlarmReceiver::class.java)
            else -> Intent(context, AlarmReceiver::class.java)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            debugLog.add("SCHEDULER: 🗑️ Alarma cancelada (requestCode: $requestCode)")
            eventBus.tryEmit(AlarmEvent.AlarmCancelled(requestCode = requestCode))
        }
    }
}