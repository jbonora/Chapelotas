package com.chapelotas.app.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Receiver que inicia Chapelotas cuando el teléfono se enciende
 * NO puede usar @Inject, así que solo inicia el servicio
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootReceiver", "📱 Teléfono encendido - Despertando a Chapelotas...")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {

                // 1. Iniciar servicio foreground
                val serviceIntent = Intent(context, ChapelotasNotificationService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                // 2. Programar WorkManager como respaldo
                schedulePeriodicWork(context)

                Log.d("BootReceiver", "✅ Servicio iniciado desde boot")
            }
        }
    }

    private fun schedulePeriodicWork(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<MonkeyWorker>(
            15, TimeUnit.MINUTES // Mínimo permitido por Android
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false) // Funcionar incluso con batería baja
                    .build()
            )
            .addTag("monkey_periodic_check")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "monkey_periodic",
            ExistingPeriodicWorkPolicy.KEEP, // No reemplazar si ya existe
            workRequest
        )
    }
}

/**
 * Worker que verifica que el mono siga vivo
 */
class MonkeyWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        Log.d("MonkeyWorker", "🔍 Verificando estado del mono...")

        // Reiniciar servicio si no está activo
        val serviceIntent = Intent(applicationContext, ChapelotasNotificationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(serviceIntent)
        } else {
            applicationContext.startService(serviceIntent)
        }

        return Result.success()
    }
}