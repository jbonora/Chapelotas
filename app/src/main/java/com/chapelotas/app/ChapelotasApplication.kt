package com.chapelotas.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.chapelotas.app.data.alarms.AlarmAuditWorker
import com.chapelotas.app.data.alarms.SystemAlarmWorker
import com.chapelotas.app.data.notifications.ChapelotasNotificationService
import com.chapelotas.app.data.notifications.KeepAliveWorker
import com.chapelotas.app.data.todo.RolloverTodosWorker
import com.chapelotas.app.di.Constants
import dagger.hilt.android.HiltAndroidApp
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import com.chapelotas.app.battery.HuaweiUtils
import com.chapelotas.app.data.notifications.NightGuardianReceiver
import com.chapelotas.app.domain.events.EventLogger

@HiltAndroidApp
class ChapelotasApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var eventLogger: EventLogger

    override fun onCreate() {
        super.onCreate()
        Log.d("ChapelotasApp", "✅ Aplicación iniciada y configurada con Hilt.")

        // Iniciar el logging de eventos
        eventLogger.startLogging()

        createAlarmNotificationChannel()
        startChapelotasService()
        setupDailyAlarmWorker()
        setupKeepAliveWorker()
        setupAlarmAuditWorker()
        setupRolloverTodosWorker()
        setupNightGuardian()
    }

    private fun setupNightGuardian() {
        // Solo activar el guardian nocturno en Huawei
        if (HuaweiUtils.isHuaweiDevice()) {
            val guardian = NightGuardianReceiver()
            guardian.scheduleNextHeartbeat(this)

            Log.d("ChapelotasApp", "🌙 Guardian nocturno activado para Huawei")
        }
    }

    private fun startChapelotasService() {
        val serviceIntent = Intent(this, ChapelotasNotificationService::class.java).apply {
            // ⚡ CORRECCIÓN AGREGADA: Enviamos la acción para que el servicio inicie el monitoreo
            action = Constants.ACTION_START_MONITORING
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        Log.d("ChapelotasApp", "🚀 Servicio iniciado con orden de monitoreo")
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

    private fun setupDailyAlarmWorker() {
        val now = ZonedDateTime.now()
        var nextRun = now.withHour(2).withMinute(0).withSecond(0)
        if (now.isAfter(nextRun)) {
            nextRun = nextRun.plusDays(1)
        }
        val initialDelay = java.time.Duration.between(now, nextRun).toMillis()

        val alarmWorkRequest = PeriodicWorkRequestBuilder<SystemAlarmWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailySystemAlarmWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            alarmWorkRequest
        )
    }

    private fun setupKeepAliveWorker() {
        // Esta función está vacía en tu código original
    }

    private fun setupAlarmAuditWorker() {
        val auditRequest = PeriodicWorkRequestBuilder<AlarmAuditWorker>(
            6, // Cada 6 horas
            TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "AlarmAuditWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            auditRequest
        )
    }

    private fun setupRolloverTodosWorker() {
        val now = ZonedDateTime.now()
        var nextRun = now.withHour(23).withMinute(55).withSecond(0)
        if (now.isAfter(nextRun)) {
            nextRun = nextRun.plusDays(1)
        }
        val initialDelay = java.time.Duration.between(now, nextRun).toMillis()

        val rolloverRequest = PeriodicWorkRequestBuilder<RolloverTodosWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "RolloverTodosWorker",
            ExistingPeriodicWorkPolicy.REPLACE,
            rolloverRequest
        )
        Log.d("ChapelotasApp", "🔄 Worker de traspaso de To-Dos agendado para las 23:55.")
    }

    override val workManagerConfiguration: Configuration
        get() =
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
}