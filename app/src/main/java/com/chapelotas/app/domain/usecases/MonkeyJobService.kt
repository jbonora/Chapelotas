package com.chapelotas.app.domain.usecases

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MonkeyJobService : Service() {

    @Inject
    lateinit var unifiedMonkey: UnifiedMonkeyService

    private val serviceScope = CoroutineScope(Dispatchers.Default)

    companion object {
        const val TAG = "Vigía-JobService"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: 🟢 Servicio de trabajo iniciado, pidiendo al Mono que procese.")

        serviceScope.launch {
            unifiedMonkey.processNotificationsAndReschedule()
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "onStartCommand: ✅ Trabajo completado. Servicio destruido.")
        super.onDestroy()
    }
}