package com.chapelotas.app.data.alarms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chapelotas.app.data.notifications.NotificationAlarmReceiver
import com.chapelotas.app.di.Constants
import com.chapelotas.app.domain.debug.DebugLog
import com.chapelotas.app.domain.repositories.TaskRepository
import com.chapelotas.app.domain.usecases.ReminderEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class AlarmAuditWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val reminderEngine: ReminderEngine,
    private val debugLog: DebugLog
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        debugLog.add("AUDITOR: 🧐 Iniciando auditoría de alarmas.")

        val activeTasks = taskRepository.observeAllTasks().first().filter { !it.isFinished }
        if (activeTasks.isEmpty()) {
            debugLog.add("AUDITOR: No hay tareas activas. Nada que auditar.")
            return Result.success()
        }

        var rescheduledCount = 0
        activeTasks.forEach { task ->
            val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
                action = Constants.ACTION_TRIGGER_REMINDER
                putExtra(Constants.EXTRA_TASK_ID, task.id)
            }
            val alarmRequestCode = task.id.hashCode()

            // Verificamos si la alarma AÚN EXISTE en el sistema
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmRequestCode,
                intent,
                // --- LÍNEA CORREGIDA ---
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )

            if (pendingIntent == null && task.nextReminderAt != null) {
                // La alarma no existe, ¡pero debería! La reprogramamos.
                debugLog.add("AUDITOR: ⚠️ Alarma perdida para '${task.title}'. Reprogramando...")
                reminderEngine.initializeOrUpdateAllReminders() // La forma más fácil es re-evaluar todo
                rescheduledCount++
            }
        }

        debugLog.add("AUDITOR: 🧐 Auditoría finalizada. Se reprogramaron $rescheduledCount alarmas.")
        return Result.success()
    }
}