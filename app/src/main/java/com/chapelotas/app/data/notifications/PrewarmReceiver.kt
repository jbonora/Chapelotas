package com.chapelotas.app.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Vibrator
import com.chapelotas.app.domain.debug.DebugLog

class PrewarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val isPrewarm = intent.getBooleanExtra("is_prewarm", false)
        val taskId = intent.getStringExtra("task_id")

        if (isPrewarm) {
            // Solo despertar la app, NO mostrar nada
            DebugLog.add("🔥 PREWARM: App despertada para alarma próxima (Task: $taskId)")

            // Hacer algo mínimo pero que mantenga la app "viva"
            val serviceIntent = Intent(context, ChapelotasNotificationService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                DebugLog.add("🔥 PREWARM: Error iniciando servicio: ${e.message}")
            }

            // Vibración imperceptible (1ms) para despertar hardware
            try {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(1) // Casi imperceptible pero despierta el sistema
            } catch (e: Exception) {
                // Ignorar errores de vibración
            }
        }
    }
}