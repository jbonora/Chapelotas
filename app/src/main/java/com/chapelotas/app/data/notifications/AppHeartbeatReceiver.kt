package com.chapelotas.app.data.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.chapelotas.app.di.Constants
import com.chapelotas.app.domain.debug.DebugLog
import java.util.concurrent.TimeUnit

class AppHeartbeatReceiver : BroadcastReceiver() {

    companion object {
        private val HEARTBEAT_INTERVAL = TimeUnit.MINUTES.toMillis(15)
        private const val REQUEST_CODE = 9999
    }

    override fun onReceive(context: Context, intent: Intent) {
        DebugLog.add("❤️ HEARTBEAT: Receptor activado. Realizando pulso de supervivencia...")

        // 1. Despertar el hardware con una micro-vibración.
        // Esto reemplaza la necesidad de un PrewarmReceiver para cada alarma.
        wakeUpHardware(context)

        // 2. Asegurarse de que el servicio principal esté corriendo.
        ensureMainServiceAlive(context)

        // 3. Programar el siguiente latido.
        scheduleNextHeartbeat(context)
    }

    private fun ensureMainServiceAlive(context: Context) {
        val serviceIntent = Intent(context, ChapelotasNotificationService::class.java).apply {
            action = Constants.ACTION_START_MONITORING
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            DebugLog.add("❤️ HEARTBEAT: Orden de inicio/monitoreo enviada al servicio.")
        } catch (e: Exception) {
            DebugLog.add("❤️ HEARTBEAT: ❌ Error al intentar iniciar el servicio: ${e.message}")
        }
    }

    private fun wakeUpHardware(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            // Una vibración de 1 milisegundo es imperceptible para el usuario pero efectiva para despertar el hardware.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(1, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(1)
            }
            DebugLog.add("❤️ HEARTBEAT: Pulso de hardware enviado (1ms de vibración).")
        } catch (e: Exception) {
            DebugLog.add("❤️ HEARTBEAT: ⚠️ No se pudo ejecutar la micro-vibración: ${e.message}")
        }
    }


    fun scheduleNextHeartbeat(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AppHeartbeatReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + HEARTBEAT_INTERVAL,
                pendingIntent
            )
            DebugLog.add("❤️ HEARTBEAT: Próximo latido programado en 15 minutos.")
        } catch (e: SecurityException) {
            DebugLog.add("❤️ HEARTBEAT: ❌ Error de seguridad al programar alarma: ${e.message}")
        }
    }
}