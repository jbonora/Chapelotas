package com.chapelotas.app.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.chapelotas.app.domain.usecases.MonkeyJobService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "Vigía-AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: ⏰ ¡Alarma recibida! Iniciando servicio de trabajo del Mono...")

        val serviceIntent = Intent(context, MonkeyJobService::class.java)
        context.startService(serviceIntent)
        Log.d(TAG, "onReceive: ✅ Servicio de trabajo iniciado.")
    }
}