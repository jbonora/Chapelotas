package com.chapelotas.app.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.chapelotas.app.di.Constants

/**
 * Receiver que inicia Chapelotas cuando el teléfono se enciende.
 * Inicia el servicio principal y la cadena de alarmas Keep-Alive.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootReceiver", "📱 Teléfono encendido - Despertando a Chapelotas...")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {

                // 1. Iniciar servicio foreground y darle la orden explícita de monitorear.
                val serviceIntent = Intent(context, ChapelotasNotificationService::class.java).apply {
                    // ✅ ESTA LÍNEA YA ESTÁ CORRECTA
                    action = Constants.ACTION_START_MONITORING
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                // 2. Iniciar la cadena de alarmas Keep-Alive como respaldo.
                KeepAliveReceiver().scheduleNext(context)

                Log.d("BootReceiver", "✅ Servicio (con orden de monitoreo) y cadena Keep-Alive iniciados desde boot.")
            }
        }
    }
}