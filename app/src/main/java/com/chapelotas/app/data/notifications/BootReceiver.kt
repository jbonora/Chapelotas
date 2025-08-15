package com.chapelotas.app.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.chapelotas.app.di.Constants

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootReceiver", "📱 Teléfono encendido - Despertando a Chapelotas...")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {

                // 1. Iniciar el servicio foreground INMEDIATAMENTE.
                // Esta es la acción más crítica para que la app sobreviva al reinicio.
                val serviceIntent = Intent(context, ChapelotasNotificationService::class.java).apply {
                    action = Constants.ACTION_START_MONITORING
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                // 2. Iniciar la cadena de alarmas Heartbeat como respaldo para el futuro.
                // El servicio ya está en marcha, esto solo asegura que se mantenga así.
                AppHeartbeatReceiver().scheduleNextHeartbeat(context)

                Log.d("BootReceiver", "✅ Servicio iniciado y cadena Heartbeat programada desde el arranque.")
            }
        }
    }
}