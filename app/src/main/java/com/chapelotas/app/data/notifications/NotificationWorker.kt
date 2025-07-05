package com.chapelotas.app.data.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chapelotas.app.domain.entities.ChapelotasNotification
import com.chapelotas.app.domain.repositories.NotificationRepository
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker que ejecuta las notificaciones programadas
 */
@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationRepository: NotificationRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Obtener la notificación del input data
            val notificationJson = inputData.getString(NotificationRepositoryImpl.NOTIFICATION_DATA_KEY)
                ?: return Result.failure()

            val notification = Gson().fromJson(notificationJson, ChapelotasNotification::class.java)

            // Mostrar la notificación
            notificationRepository.showImmediateNotification(notification)

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}