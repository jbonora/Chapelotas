package com.chapelotas.app.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.chapelotas.app.domain.usecases.ReminderEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Este BroadcastReceiver es despertado por una alarma exacta del AlarmManager.
 * Su única responsabilidad es recibir la alarma y delegar el trabajo de enviar
 * el recordatorio al ReminderEngine.
 */
@AndroidEntryPoint
class NotificationAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderEngine: ReminderEngine
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val ACTION_TRIGGER_REMINDER = "com.chapelotas.action.TRIGGER_REMINDER"
        const val EXTRA_TASK_ID = "extra_task_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TRIGGER_REMINDER) {
            val taskId = intent.getStringExtra(EXTRA_TASK_ID)
            if (taskId.isNullOrEmpty()) {
                Log.e("AlarmReceiver", "Alarma recibida sin ID de tarea.")
                return
            }

            Log.d("AlarmReceiver", "⏰ ¡Timbre sonó! La alarma para la tarea '$taskId' fue recibida.")

            val pendingResult = goAsync()

            scope.launch {
                try {
                    Log.d("AlarmReceiver", "Delegando trabajo al ReminderEngine.")
                    // --- LA LÍNEA CORREGIDA ---
                    // Antes: reminderEngine.checkAndSendReminders()
                    // Ahora: Llamamos a la nueva función y le pasamos el ID de la tarea
                    reminderEngine.processAndSendReminder(taskId)
                    // -------------------------
                } catch(e: Exception) {
                    Log.e("AlarmReceiver", "Error al procesar la alarma para la tarea $taskId", e)
                }
                finally {
                    pendingResult.finish()
                    Log.d("AlarmReceiver", "Trabajo finalizado para la tarea '$taskId'.")
                }
            }
        }
    }
}