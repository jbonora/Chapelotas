package com.chapelotas.app.data.alarms

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chapelotas.app.domain.debug.DebugLog
import com.chapelotas.app.domain.events.EventBus
import com.chapelotas.app.domain.events.SystemEvent
import com.chapelotas.app.domain.usecases.AlarmSchedulerUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SystemAlarmWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val alarmScheduler: AlarmSchedulerUseCase,
    private val debugLog: DebugLog,
    private val eventBus: EventBus // Inyectamos el EventBus
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val ALARM_REQUEST_CODE = 4242
        const val CHANNEL_ID = "chapelotas_system_alarm_channel"
    }

    override suspend fun doWork(): Result {
        debugLog.add("⏰ ALARM WORKER (Respaldo): Ejecutando verificación nocturna de la alarma.")

        // Emitir evento de que el worker está ejecutándose
        eventBus.emit(
            SystemEvent.ServiceStarted(serviceName = "SystemAlarmWorker")
        )

        return try {
            // Simplemente delegamos toda la lógica al UseCase
            alarmScheduler.scheduleNextAlarm()

            debugLog.add("⏰ ALARM WORKER (Respaldo): ✅ Verificación completada exitosamente.")
            Result.success()
        } catch (e: Exception) {
            debugLog.add("⏰ ALARM WORKER (Respaldo): ❌ Error durante la verificación: ${e.message}")
            Result.failure()
        }
    }
}