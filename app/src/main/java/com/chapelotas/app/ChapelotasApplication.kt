package com.chapelotas.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.chapelotas.app.data.alarms.AlarmAuditWorker
import com.chapelotas.app.data.alarms.SystemAlarmWorker
import com.chapelotas.app.data.todo.RolloverTodosWorker
import com.chapelotas.app.domain.events.EventLogger
import dagger.hilt.android.HiltAndroidApp
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class ChapelotasApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var eventLogger: EventLogger

    override fun onCreate() {
        super.onCreate()
        Log.d("ChapelotasApp", "✅ Aplicación iniciada y configurada con Hilt.")

        eventLogger.startLogging()
        createAlarmNotificationChannel()

        // ✅ CORRECTION: Moved worker setup from the initializer to here
        setupDailyWorkers()
    }

    private fun createAlarmNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SystemAlarmWorker.CHANNEL_ID,
                "Alarmas Despertador Chapelotas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para las alarmas despertador automáticas."
                enableVibration(true)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupDailyWorkers() {
        val workManager = WorkManager.getInstance(this)

        val alarmWorkRequest = PeriodicWorkRequestBuilder<SystemAlarmWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(getInitialDelay(2, 0), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork("DailySystemAlarmWorker", ExistingPeriodicWorkPolicy.KEEP, alarmWorkRequest)
        Log.d("ChapelotasApp", "⏰ Worker de alarma despertador agendado para las 02:00.")

        // 🔧 CAMBIO CRÍTICO: Usar la función diseñada con 2 horas en lugar de 6
        AlarmAuditWorker.schedulePeriodicAudit(this)
        Log.d("ChapelotasApp", "🔧 Worker de auditoría configurado para cada 2 horas (en lugar de 6).")

        val rolloverRequest = PeriodicWorkRequestBuilder<RolloverTodosWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(getInitialDelay(23, 55), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork("RolloverTodosWorker", ExistingPeriodicWorkPolicy.REPLACE, rolloverRequest)
        Log.d("ChapelotasApp", "📄 Worker de traspaso de To-Dos agendado para las 23:55.")
    }

    private fun getInitialDelay(targetHour: Int, targetMinute: Int): Long {
        val now = ZonedDateTime.now()
        var nextRun = now.withHour(targetHour).withMinute(targetMinute).withSecond(0)
        if (now.isAfter(nextRun)) {
            nextRun = nextRun.plusDays(1)
        }
        return java.time.Duration.between(now, nextRun).toMillis()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}