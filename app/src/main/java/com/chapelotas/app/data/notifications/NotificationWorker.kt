package com.chapelotas.app.data.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chapelotas.app.domain.repositories.NotificationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker obsoleto. Con la nueva lógica de AlarmManager, este worker ya no es necesario.
 * Se mantiene vacío para evitar errores de compilación si estuviera referenciado
 * en alguna parte del sistema de build, pero no tiene ninguna funcionalidad activa.
 */
@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationRepository: NotificationRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // La lógica anterior ha sido eliminada. El worker ahora simplemente
        // reporta que ha completado su "trabajo" (que es no hacer nada) con éxito.
        return Result.success()
    }
}