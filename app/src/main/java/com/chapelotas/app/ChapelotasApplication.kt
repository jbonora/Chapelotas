package com.chapelotas.app

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.chapelotas.app.data.notifications.ChapelotasNotificationService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Clase de aplicación principal.
 * Ahora también es responsable de iniciar el servicio en primer plano
 * para garantizar que siempre esté activo cuando la app se inicie.
 */
@HiltAndroidApp
class ChapelotasApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        Log.d("ChapelotasApp", "✅ Aplicación iniciada y configurada con Hilt.")

        // --- LA LÍNEA AÑADIDA ---
        // Aquí iniciamos el servicio en primer plano.
        startChapelotasService()
        // -------------------------
    }

    private fun startChapelotasService() {
        val serviceIntent = Intent(this, ChapelotasNotificationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    override val workManagerConfiguration: Configuration
        get() =
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
}