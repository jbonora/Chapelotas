package com.chapelotas.app

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ChapelotasApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    init {
        // Tu log, que está perfecto.
        Log.d("ChapelotasDebug", "ChapelotasApplication - INIT: La clase Application se está inicializando.")
    }

    override fun onCreate() {
        super.onCreate()
        // Tu log, que está perfecto.
        Log.d("ChapelotasDebug", "ChapelotasApplication - ON_CREATE: El método onCreate se ha completado.")
    }

    override val workManagerConfiguration: Configuration
        get() {
            // Tu log, que está perfecto.
            Log.d("ChapelotasDebug", "ChapelotasApplication - GET_CONFIG: ¡WorkManager está pidiendo la configuración de Hilt! Proveyendo HiltWorkerFactory.")

            return Configuration.Builder()
                // La única corrección necesaria.
                .setMinimumLoggingLevel(Log.DEBUG)
                .setWorkerFactory(workerFactory)
                .build()
        }
}