package com.chapelotas.app.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "Vigía-AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "⏰ Alarma recibida")
        // TODO V2: Implementar lógica de alarmas
    }
}