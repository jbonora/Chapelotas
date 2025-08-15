package com.chapelotas.app.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.chapelotas.app.di.Constants
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class NotificationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Constants.ACTION_TRIGGER_REMINDER) {
            val taskId = intent.getStringExtra(Constants.EXTRA_TASK_ID)
            if (taskId.isNullOrEmpty()) {
                Log.e("AlarmReceiver", "Alarma recibida sin ID de tarea.")
                return
            }

            Log.d("AlarmReceiver", "⏰ ¡Timbre sonó! Alarma para tarea '$taskId'")

            // CAMBIO CRÍTICO: Usar WorkManager para garantizar que el trabajo se complete
            // incluso si el sistema mata el BroadcastReceiver
            val workRequest = OneTimeWorkRequestBuilder<ProcessReminderWorker>()
                .setInputData(
                    workDataOf(Constants.EXTRA_TASK_ID to taskId)
                )
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    1,
                    TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "reminder_$taskId",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )

            Log.d("AlarmReceiver", "✅ Trabajo encolado en WorkManager para tarea '$taskId'")
        }
    }
}