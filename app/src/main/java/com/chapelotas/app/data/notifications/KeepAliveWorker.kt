package com.chapelotas.app.data.notifications

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chapelotas.app.domain.debug.DebugLog
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class KeepAliveWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val debugLog: DebugLog
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        debugLog.add("❤️ WORKER-KEEPALIVE: Tarea de supervivencia ejecutada.")

        // Asegurarse de que el servicio principal esté corriendo.
        val serviceIntent = Intent(context, ChapelotasNotificationService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            // En Android 12+ puede haber excepciones si la app está en segundo plano.
            debugLog.add("❤️ WORKER-KEEPALIVE: No se pudo iniciar el servicio: ${e.message}")
        }

        return Result.success()
    }
}