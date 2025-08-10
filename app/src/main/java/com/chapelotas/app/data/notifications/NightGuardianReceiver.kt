package com.chapelotas.app.data.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Vibrator
import android.util.Log
import com.chapelotas.app.di.Constants
import com.chapelotas.app.domain.debug.DebugLog
import java.util.*
import java.util.concurrent.TimeUnit

class NightGuardianReceiver : BroadcastReceiver() {

    companion object {
        private val HEARTBEAT_INTERVAL = TimeUnit.MINUTES.toMillis(45)
        private const val REQUEST_CODE = 9999
    }

    override fun onReceive(context: Context, intent: Intent) {
        // 1. Hacer algo MUY ligero pero que despierte la app
        DebugLog.add("💓 GUARDIAN: App despierta - ${System.currentTimeMillis()}")

        // 2. Verificar si hay alarmas próximas (siguiente hora)
        checkUpcomingAlarms(context)

        // 3. Asegurar que el servicio principal siga vivo
        ensureMainServiceAlive(context)

        // 4. Programar el siguiente heartbeat
        scheduleNextHeartbeat(context)
    }

    private fun checkUpcomingAlarms(context: Context) {
        // Solo entre 22:00 y 08:00 (horario crítico)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour in 22..23 || hour in 0..8) {
            DebugLog.add("💓 GUARDIAN: Horario nocturno crítico - verificando alarmas")
            // Hacer micro-vibración imperceptible para despertar hardware
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(1) // 1ms - casi imperceptible
        }
    }

    private fun ensureMainServiceAlive(context: Context) {
        val serviceIntent = Intent(context, ChapelotasNotificationService::class.java).apply {
            // ⚡ CORRECCIÓN AGREGADA: Enviamos la acción para que el servicio monitoree
            action = Constants.ACTION_START_MONITORING
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            DebugLog.add("💓 GUARDIAN: Servicio reiniciado con orden de monitoreo")
        } catch (e: Exception) {
            DebugLog.add("💓 GUARDIAN: Error reiniciando servicio: ${e.message}")
        }
    }

    fun scheduleNextHeartbeat(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NightGuardianReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // USAR setExactAndAllowWhileIdle - esto bypasea Doze
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + HEARTBEAT_INTERVAL,
                pendingIntent
            )
            DebugLog.add("💓 GUARDIAN: Próximo heartbeat programado en 45 min")
        } catch (e: SecurityException) {
            Log.e("NightGuardian", "Error programando heartbeat: ${e.message}")
        }
    }
}