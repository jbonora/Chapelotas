package com.chapelotas.app.data.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.chapelotas.app.data.notifications.NotificationAlarmReceiver
import com.chapelotas.app.di.Constants
import com.chapelotas.app.domain.debug.DebugLog
import com.chapelotas.app.domain.repositories.TaskRepository
import com.chapelotas.app.domain.usecases.ReminderEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@HiltWorker
class AlarmAuditWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val reminderEngine: ReminderEngine,
    private val debugLog: DebugLog
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_TAG = "alarm_audit"
        const val UNIQUE_WORK_NAME = "periodic_alarm_audit"

        fun schedulePeriodicAudit(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false) // Ejecutar incluso con batería baja
                .build()

            // Auditoría cada 2 horas
            val auditRequest = PeriodicWorkRequestBuilder<AlarmAuditWorker>(
                2, TimeUnit.HOURS,
                15, TimeUnit.MINUTES // Flexibilidad de 15 minutos
            )
                .setConstraints(constraints)
                .addTag(WORK_TAG)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15,
                    TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                auditRequest
            )
        }
    }

    override suspend fun doWork(): Result {
        val now = LocalDateTime.now()
        debugLog.add("🔧 AUDITOR: ===== AUDITORÍA INICIADA a las ${now} =====")

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager == null) {
            debugLog.add("❌ AUDITOR: No se pudo obtener AlarmManager")
            return Result.failure()
        }

        val activeTasks = taskRepository.observeAllTasks().first().filter { !it.isFinished }
        debugLog.add("🔧 AUDITOR: Encontradas ${activeTasks.size} tareas activas")

        if (activeTasks.isEmpty()) {
            debugLog.add("🔧 AUDITOR: No hay tareas activas. Nada que auditar.")
            return Result.success()
        }

        var alarmsFixed = 0
        var alarmsVerified = 0
        var tasksNeedingAttention = mutableListOf<String>()

        for (task in activeTasks) {
            val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
                action = Constants.ACTION_TRIGGER_REMINDER
                putExtra(Constants.EXTRA_TASK_ID, task.id)
            }
            val alarmRequestCode = task.id.hashCode()

            // Verificar si la alarma existe
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmRequestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )

            when {
                // Caso 1: La tarea necesita recordatorio pero no tiene alarma
                pendingIntent == null && task.nextReminderAt != null -> {
                    debugLog.add("⚠️ AUDITOR: Alarma perdida para '${task.title}'")

                    // Si el recordatorio debió haber sonado en las últimas 2 horas
                    if (task.nextReminderAt!!.isBefore(now) &&
                        task.nextReminderAt!!.isAfter(now.minusHours(2))) {
                        debugLog.add("🚨 AUDITOR: Recordatorio perdido reciente para '${task.title}'")
                        // Procesar inmediatamente
                        try {
                            reminderEngine.processAndSendReminder(task.id)
                        } catch (e: Exception) {
                            debugLog.add("❌ AUDITOR: Error procesando recordatorio perdido: ${e.message}")
                        }
                    }

                    tasksNeedingAttention.add(task.id)
                    alarmsFixed++
                }

                // Caso 2: La alarma existe y está bien
                pendingIntent != null && task.nextReminderAt != null -> {
                    alarmsVerified++
                }

                // Caso 3: Tarea sin siguiente recordatorio pero con alarma activa
                pendingIntent != null && task.nextReminderAt == null -> {
                    debugLog.add("🗑️ AUDITOR: Cancelando alarma huérfana para '${task.title}'")
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                }
            }
        }

        // Reprogramar todas las tareas que necesitan atención
        if (tasksNeedingAttention.isNotEmpty()) {
            debugLog.add("🔄 AUDITOR: Reprogramando ${tasksNeedingAttention.size} tareas...")
            for (taskId in tasksNeedingAttention) {
                try {
                    val task = taskRepository.getTask(taskId)
                    if (task != null && !task.isFinished) {
                        reminderEngine.initializeOrUpdateAllReminders()
                        break // Solo necesitamos llamarlo una vez
                    }
                } catch (e: Exception) {
                    debugLog.add("❌ AUDITOR: Error reprogramando tarea $taskId: ${e.message}")
                }
            }
        }

        debugLog.add("🔧 AUDITOR: ===== AUDITORÍA COMPLETADA =====")
        debugLog.add("   - Alarmas verificadas: $alarmsVerified")
        debugLog.add("   - Alarmas reparadas: $alarmsFixed")
        debugLog.add("   - Tiempo actual: ${LocalDateTime.now()}")

        return Result.success()
    }
}