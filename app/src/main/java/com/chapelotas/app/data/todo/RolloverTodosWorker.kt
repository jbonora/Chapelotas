package com.chapelotas.app.data.todo

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chapelotas.app.domain.debug.DebugLog
import com.chapelotas.app.domain.usecases.RolloverTodosUseCase // <-- IMPORTAMOS EL USECASE
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class RolloverTodosWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    // PASO 3.1: Inyectamos el UseCase en lugar de los repositorios.
    private val rolloverTodosUseCase: RolloverTodosUseCase,
    private val debugLog: DebugLog
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // PASO 3.2: El worker ahora solo delega toda la lógica al UseCase.
            rolloverTodosUseCase.execute()
            debugLog.add("🔄 TODO-ROLLOVER: Proceso de worker completado con éxito.")
            Result.success()
        } catch (e: Exception) {
            debugLog.add("🔄 TODO-ROLLOVER: ❌ Error fatal en el worker de traspaso: ${e.message}")
            Result.failure()
        }
    }
}