package com.chapelotas.app.data.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.chapelotas.app.di.Constants
import com.chapelotas.app.domain.debug.DebugLog
import java.util.concurrent.TimeUnit

class KeepAliveReceiver : BroadcastReceiver() {

    companion object {
        private val INTERVAL = TimeUnit.MINUTES.toMillis(45) // Cada 45 minutos
        private const val REQUEST_CODE = 1338
    }

    override fun onReceive(context: Context, intent: Intent) {
        DebugLog.add("❤️ KEEP-ALIVE: Receptor Keep-Alive activado.")

        // 1. Asegurarse de que el servicio principal esté corriendo.
        val serviceIntent = Intent(context, ChapelotasNotificationService::class.java).apply {
            // ⚡ CORRECCIÓN AGREGADA: Enviamos la acción para que el servicio monitoree
            action = Constants.ACTION_START_MONITORING
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        DebugLog.add("❤️ KEEP-ALIVE: Servicio reiniciado con orden de monitoreo")

        // 2. Programar la siguiente alarma en la cadena.
        scheduleNext(context)
    }

    fun scheduleNext(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, KeepAliveReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // Usamos setExactAndAllowWhileIdle para la máxima prioridad.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Log.w("KeepAliveReceiver", "No se pueden programar alarmas exactas. Usando fallback.")
                alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + INTERVAL, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + INTERVAL,
                    pendingIntent
                )
            }
            DebugLog.add("❤️ KEEP-ALIVE: Próxima alarma programada en 45 min.")
        } catch (e: SecurityException) {
            Log.e("KeepAliveReceiver", "Error de seguridad al programar alarma Keep-Alive.", e)
            DebugLog.add("❤️ KEEP-ALIVE: ❌ Error de seguridad al programar alarma.")
        }
    }
}