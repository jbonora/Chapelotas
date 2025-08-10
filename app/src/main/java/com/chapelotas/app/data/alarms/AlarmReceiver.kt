package com.chapelotas.app.data.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.chapelotas.app.presentation.ui.AlarmActivity

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Lanzar la actividad de alarma
        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

            // --- INICIO DE LA MODIFICACIÓN ---
            // Pasar todos los extras recibidos a la nueva actividad
            putExtras(intent.extras ?: Bundle())
            // --- FIN DE LA MODIFICACIÓN ---
        }
        context.startActivity(alarmIntent)
    }
}