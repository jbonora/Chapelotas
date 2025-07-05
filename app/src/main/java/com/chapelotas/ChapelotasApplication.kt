package com.chapelotas.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Clase Application para inicializar Hilt
 */
@HiltAndroidApp
class ChapelotasApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Aquí podemos inicializar otras librerías si es necesario
    }
}