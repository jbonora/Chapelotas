package com.chapelotas.app.data.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.chapelotas.app.domain.debug.DebugLog
import com.chapelotas.app.domain.repositories.PreferencesRepository
import com.chapelotas.app.domain.usecases.AlarmSchedulerUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

/**
 * BroadcastReceiver especial para manejar alarmas intermedias en dispositivos Huawei.
 *
 * Este receiver se activa cuando una alarma excede el límite de 270 minutos en Huawei,
 * y su función es reprogramar la alarma real cuando esté dentro del límite permitido.
 */
@AndroidEntryPoint
class HuaweiIntermediateAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmScheduler: AlarmSchedulerUseCase

    @Inject
    lateinit var debugLog: DebugLog

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        debugLog.add("HUAWEI_INTERMEDIATE: ⏰ Alarma intermedia activada")
        debugLog.add("HUAWEI_INTERMEDIATE: Verificando datos para reprogramar alarma final...")

        // Extraer los datos de la alarma final
        val finalAlarmDateStr = intent.getStringExtra("final_alarm_date")
        val finalAlarmTimeStr = intent.getStringExtra("final_alarm_time")

        debugLog.add("HUAWEI_INTERMEDIATE: Datos recibidos:")
        debugLog.add("HUAWEI_INTERMEDIATE:   - Fecha final: $finalAlarmDateStr")
        debugLog.add("HUAWEI_INTERMEDIATE:   - Hora final: $finalAlarmTimeStr")

        if (finalAlarmDateStr == null || finalAlarmTimeStr == null) {
            debugLog.add("HUAWEI_INTERMEDIATE: ❌ ERROR - Datos de alarma final no encontrados")
            return
        }

        try {
            val finalAlarmDate = LocalDate.parse(finalAlarmDateStr)
            val finalAlarmTime = LocalTime.parse(finalAlarmTimeStr)
            val finalDateTime = LocalDateTime.of(finalAlarmDate, finalAlarmTime)
            val now = LocalDateTime.now()

            // Calcular los minutos reales hasta la alarma final
            val actualRemainingMinutes = java.time.Duration.between(now, finalDateTime).toMinutes()

            debugLog.add("HUAWEI_INTERMEDIATE: Cálculo de tiempo:")
            debugLog.add("HUAWEI_INTERMEDIATE:   - Hora actual: $now")
            debugLog.add("HUAWEI_INTERMEDIATE:   - Hora objetivo: $finalDateTime")
            debugLog.add("HUAWEI_INTERMEDIATE:   - Minutos calculados: $actualRemainingMinutes")

            if (actualRemainingMinutes > 0) {
                // Verificar si ahora estamos dentro del límite de Huawei
                if (actualRemainingMinutes <= 270) {
                    debugLog.add("HUAWEI_INTERMEDIATE: ✅ Dentro del límite de 270 minutos")
                    debugLog.add("HUAWEI_INTERMEDIATE: Reprogramando alarma final...")

                    scope.launch {
                        try {
                            val settings = preferencesRepository.observeAppSettings().first()
                            alarmScheduler.scheduleAlarm(
                                minutesFromNow = actualRemainingMinutes,
                                alarmDate = finalAlarmDate,
                                alarmTime = finalAlarmTime,
                                settings = settings
                            )
                            debugLog.add("HUAWEI_INTERMEDIATE: ✅ Alarma final reprogramada exitosamente")
                        } catch (e: Exception) {
                            debugLog.add("HUAWEI_INTERMEDIATE: ❌ Error reprogramando: ${e.message}")
                        }
                    }
                } else {
                    // Si aún excede el límite, programar otra alarma intermedia
                    debugLog.add("HUAWEI_INTERMEDIATE: ⚠️ Aún excede el límite ($actualRemainingMinutes min > 270 min)")
                    debugLog.add("HUAWEI_INTERMEDIATE: Programando otra alarma intermedia...")

                    scope.launch {
                        scheduleAnotherIntermediateAlarm(context, finalDateTime)
                    }
                }
            } else {
                debugLog.add("HUAWEI_INTERMEDIATE: ⚠️ La alarma ya pasó (minutos: $actualRemainingMinutes)")
                if (actualRemainingMinutes >= -5) {
                    debugLog.add("HUAWEI_INTERMEDIATE: 🔔 Activando alarma inmediatamente (margen de tolerancia)")
                    val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                        putExtra("alarm_date", finalAlarmDateStr)
                        putExtra("alarm_time", finalAlarmTimeStr)
                        putExtra("is_delayed", true)
                        putExtra("sarcastic_mode", intent.getBooleanExtra("sarcastic_mode", false))
                        putExtra("snooze_minutes", intent.getIntExtra("snooze_minutes", 10))
                    }
                    context.sendBroadcast(alarmIntent)
                } else {
                    debugLog.add("HUAWEI_INTERMEDIATE: ❌ Alarma perdida (más de 5 minutos tarde)")
                }
            }
        } catch (e: Exception) {
            debugLog.add("HUAWEI_INTERMEDIATE: ❌ Error procesando alarma intermedia: ${e.message}")
        }
    }

    private fun scheduleAnotherIntermediateAlarm(
        context: Context,
        finalDateTime: LocalDateTime
    ) {
        val now = LocalDateTime.now()
        val currentRemainingMinutes = java.time.Duration.between(now, finalDateTime).toMinutes()
        val nextIntermediateMinutes = minOf(250L, currentRemainingMinutes - 10)

        if (nextIntermediateMinutes <= 0) {
            debugLog.add("HUAWEI_INTERMEDIATE: No se necesita otra alarma intermedia.")
            return
        }

        debugLog.add("HUAWEI_INTERMEDIATE: Configurando siguiente alarma intermedia en $nextIntermediateMinutes minutos.")

        val intent = Intent(context, HuaweiIntermediateAlarmReceiver::class.java).apply {
            action = "com.chapelotas.app.HUAWEI_INTERMEDIATE_ALARM"
            putExtra("final_alarm_date", finalDateTime.toLocalDate().toString())
            putExtra("final_alarm_time", finalDateTime.toLocalTime().toString())
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            AlarmSchedulerUseCase.INTERMEDIATE_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager != null) {
            val triggerTime = System.currentTimeMillis() + (nextIntermediateMinutes * 60 * 1000)
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            debugLog.add("HUAWEI_INTERMEDIATE: ✅ Siguiente alarma intermedia programada")
        } else {
            debugLog.add("HUAWEI_INTERMEDIATE: ❌ No se pudo obtener AlarmManager")
        }
    }
}