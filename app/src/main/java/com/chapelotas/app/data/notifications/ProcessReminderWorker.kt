package com.chapelotas.app.data.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chapelotas.app.di.Constants
import com.chapelotas.app.domain.debug.DebugLog
import com.chapelotas.app.domain.usecases.ReminderEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ProcessReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val reminderEngine: ReminderEngine,
    private val debugLog: DebugLog
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString(Constants.EXTRA_TASK_ID)

        if (taskId.isNullOrEmpty()) {
            debugLog.add("❌ WORKER: TaskId vacío o nulo")
            return Result.failure()
        }

        debugLog.add("🔔 WORKER: Procesando recordatorio para tarea '$taskId'")

        return try {
            reminderEngine.processAndSendReminder(taskId)
            debugLog.add("✅ WORKER: Recordatorio procesado exitosamente para '$taskId'")
            Result.success()
        } catch (e: Exception) {
            debugLog.add("❌ WORKER: Error procesando recordatorio: ${e.message}")

            // Si hay menos de 3 intentos, reintentar
            if (runAttemptCount < 3) {
                debugLog.add("🔄 WORKER: Reintentando (intento ${runAttemptCount + 1}/3)")
                Result.retry()
            } else {
                debugLog.add("💀 WORKER: Máximo de reintentos alcanzado")
                Result.failure()
            }
        }
    }
}