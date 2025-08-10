package com.chapelotas.app.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.chapelotas.app.di.Constants
import com.chapelotas.app.domain.usecases.ReminderEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderEngine: ReminderEngine
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Constants.ACTION_TRIGGER_REMINDER) {
            val taskId = intent.getStringExtra(Constants.EXTRA_TASK_ID)
            if (taskId.isNullOrEmpty()) {
                Log.e("AlarmReceiver", "Alarma recibida sin ID de tarea.")
                return
            }

            Log.d("AlarmReceiver", "⏰ ¡Timbre sonó! La alarma para la tarea '$taskId' fue recibida.")

            val pendingResult = goAsync()

            scope.launch {
                try {
                    Log.d("AlarmReceiver", "Delegando trabajo al ReminderEngine.")
                    reminderEngine.processAndSendReminder(taskId)
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