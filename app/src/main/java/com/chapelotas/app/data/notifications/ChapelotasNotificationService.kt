package com.chapelotas.app.data.notifications

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.chapelotas.app.MainActivity
import com.chapelotas.app.R
import com.chapelotas.app.di.Constants
import com.chapelotas.app.domain.debug.DebugLog
import com.chapelotas.app.domain.usecases.CalendarSyncUseCase
import com.chapelotas.app.domain.usecases.ReminderEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class ChapelotasNotificationService : Service() {

    @Inject
    lateinit var calendarSyncUseCase: CalendarSyncUseCase

    @Inject
    lateinit var reminderEngine: ReminderEngine

    @Inject
    lateinit var debugLog: DebugLog

    private var serviceJob = SupervisorJob()
    private var serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var monitoringJob: Job? = null
    private val isMonitoringActive = AtomicBoolean(false)
    private val isInitialized = AtomicBoolean(false)

    companion object {
        private const val INITIALIZATION_DELAY = 5000L
        private const val RESTART_DELAY = 2000L
        private const val ERROR_RESTART_DELAY = 10000L
        private const val MAX_RESTART_ATTEMPTS = 3
        private var restartAttempts = 0
    }

    override fun onCreate() {
        super.onCreate()
        debugLog.add("✅ SERVICIO: onCreate - El servicio se está creando.")
        resetServiceState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logServiceStart(intent)

        try {
            startForegroundWithNotification()
            reinitializeCoroutineScopeIfNeeded()

            val action = intent?.action
            when {
                action == Constants.ACTION_START_MONITORING -> handleMonitoringStart()
                action == null -> handleGenericStart()
                else -> debugLog.add("⚠️ SERVICIO: Acción desconocida: $action")
            }

            resetRestartAttempts()
        } catch (e: Exception) {
            debugLog.add("❌ SERVICIO: Error en onStartCommand: ${e.message}")
            handleServiceError(e)
        }

        return START_STICKY
    }

    private fun logServiceStart(intent: Intent?) {
        val caller = Thread.currentThread().stackTrace.find {
            it.className.contains("com.chapelotas") &&
                    !it.className.contains("NotificationService")
        }
        debugLog.add(
            "🚀 SERVICIO: Iniciado desde ${caller?.className ?: "desconocido"} " +
                    "con action: ${intent?.action ?: "NULL"}"
        )
    }

    private fun reinitializeCoroutineScopeIfNeeded() {
        if (serviceJob.isCancelled) {
            debugLog.add("🔄 SERVICIO: Reinicializando CoroutineScope cancelado")
            serviceJob = SupervisorJob()
            serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
        }
    }

    private fun handleMonitoringStart() {
        if (isMonitoringActive.compareAndSet(false, true)) {
            debugLog.add("✅ SERVICIO: Iniciando monitoreo de calendario")
            startCalendarMonitoring()
            scheduleReminderInitialization()
        } else {
            debugLog.add("⚠️ SERVICIO: Monitoreo ya activo, ignorando duplicado")
        }
    }

    private fun handleGenericStart() {
        debugLog.add("⚠️ SERVICIO: Iniciado sin acción específica - servicio en espera")
        // Optionally start monitoring anyway if not already active
        if (!isMonitoringActive.get()) {
            debugLog.add("🔄 SERVICIO: Iniciando monitoreo por defecto")
            handleMonitoringStart()
        }
    }

    private fun scheduleReminderInitialization() {
        serviceScope.launch {
            try {
                delay(INITIALIZATION_DELAY)
                debugLog.add("🔍 SERVICIO: Verificando tareas pendientes al iniciar servicio...")

                reminderEngine.initializeOrUpdateAllReminders()
                isInitialized.set(true)
                debugLog.add("✅ SERVICIO: Recordatorios inicializados correctamente")
            } catch (e: Exception) {
                debugLog.add("❌ SERVICIO: Error al inicializar recordatorios: ${e.message}")
                // Retry initialization after delay
                scheduleRetryInitialization()
            }
        }
    }

    private fun scheduleRetryInitialization() {
        serviceScope.launch {
            delay(ERROR_RESTART_DELAY)
            debugLog.add("🔄 SERVICIO: Reintentando inicialización de recordatorios...")
            try {
                reminderEngine.initializeOrUpdateAllReminders()
                isInitialized.set(true)
                debugLog.add("✅ SERVICIO: Recordatorios inicializados en reintento")
            } catch (e: Exception) {
                debugLog.add("❌ SERVICIO: Error en reintento de inicialización: ${e.message}")
            }
        }
    }

    private fun startCalendarMonitoring() {
        // Cancel previous monitoring job
        monitoringJob?.cancel()

        monitoringJob = serviceScope.launch {
            try {
                debugLog.add("👁️ CAL-MONITOR: Activando vigía de calendario...")
                calendarSyncUseCase.startMonitoring()
            } catch (e: Exception) {
                handleMonitoringError(e)
            }
        }
    }

    private suspend fun handleMonitoringError(e: Exception) {
        when (e) {
            is CancellationException -> {
                debugLog.add("👁️ CAL-MONITOR: Tarea de monitoreo cancelada (normal).")
            }
            else -> {
                debugLog.add("❌ CAL-MONITOR: Error - ${e.message}")
                debugLog.add("❌ CAL-MONITOR: Stack trace: ${e.stackTraceToString()}")

                if (restartAttempts < MAX_RESTART_ATTEMPTS) {
                    restartAttempts++
                    delay(ERROR_RESTART_DELAY)
                    debugLog.add("🔄 CAL-MONITOR: Reintento $restartAttempts/$MAX_RESTART_ATTEMPTS")
                    startCalendarMonitoring()
                } else {
                    debugLog.add("💀 CAL-MONITOR: Máximo de reintentos alcanzado")
                    isMonitoringActive.set(false)
                }
            }
        }
    }

    private fun handleServiceError(e: Exception) {
        debugLog.add("💀 SERVICIO: Error crítico en servicio: ${e.message}")
        // Could implement crash reporting here
    }

    private fun resetServiceState() {
        isMonitoringActive.set(false)
        isInitialized.set(false)
        restartAttempts = 0
    }

    private fun resetRestartAttempts() {
        restartAttempts = 0
    }

    override fun onDestroy() {
        debugLog.add("💀 SERVICIO: onDestroy - ¡El sistema está matando el servicio!")

        try {
            isMonitoringActive.set(false)
            monitoringJob?.cancel()
            serviceJob.cancel()

            // Enhanced restart logic
            scheduleServiceRestart()
        } catch (e: Exception) {
            debugLog.add("❌ SERVICIO: Error en onDestroy: ${e.message}")
        }

        super.onDestroy()
    }

    private fun scheduleServiceRestart() {
        try {
            val broadcastIntent = Intent(this, RestartServiceReceiver::class.java).apply {
                putExtra("restart_reason", "onDestroy")
                putExtra("restart_timestamp", System.currentTimeMillis())
            }
            sendBroadcast(broadcastIntent)
            debugLog.add("📡 SERVICIO: Señal de reinicio enviada")
        } catch (e: Exception) {
            debugLog.add("❌ SERVICIO: Error enviando señal de reinicio: ${e.message}")
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        debugLog.add("👋 SERVICIO: onTaskRemoved - El usuario cerró la app. Dejando ancla para revivir.")

        try {
            val restartServiceIntent = Intent(applicationContext, this::class.java).apply {
                setPackage(packageName)
                action = Constants.ACTION_START_MONITORING
                putExtra("restart_reason", "taskRemoved")
            }

            val restartServicePendingIntent = PendingIntent.getService(
                this,
                1,
                restartServiceIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmService = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmService.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + RESTART_DELAY,
                restartServicePendingIntent
            )

            debugLog.add("⏰ SERVICIO: Alarma de reinicio programada")
        } catch (e: Exception) {
            debugLog.add("❌ SERVICIO: Error en onTaskRemoved: ${e.message}")
        }

        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundWithNotification() {
        try {
            createNotificationChannelIfNeeded()

            val notificationIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this,
                Constants.REQUEST_CODE_OPEN_APP,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, Constants.CHANNEL_ID_SERVICE)
                .setContentTitle(getString(R.string.notification_service_title))
                .setContentText(getString(R.string.notification_service_text))
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .build()

            startForeground(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, notification)
            debugLog.add("📱 SERVICIO: Notificación foreground mostrada")
        } catch (e: Exception) {
            debugLog.add("❌ SERVICIO: Error creando notificación foreground: ${e.message}")
            throw e
        }
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.CHANNEL_ID_SERVICE,
                getString(R.string.notification_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_service_channel_description)
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
}

class RestartServiceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val debugLog = DebugLog
        val reason = intent?.getStringExtra("restart_reason") ?: "unknown"
        val timestamp = intent?.getLongExtra("restart_timestamp", 0L) ?: 0L

        debugLog.add("🔄 RESTART-RECEIVER: Recibida señal para reiniciar el servicio. Razón: $reason")

        context?.let { ctx ->
            try {
                val serviceIntent = Intent(ctx, ChapelotasNotificationService::class.java).apply {
                    action = Constants.ACTION_START_MONITORING
                    putExtra("restart_source", "RestartServiceReceiver")
                    putExtra("original_reason", reason)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ctx.startForegroundService(serviceIntent)
                } else {
                    ctx.startService(serviceIntent)
                }

                debugLog.add("✅ RESTART-RECEIVER: Servicio reiniciado exitosamente")
            } catch (e: Exception) {
                debugLog.add("❌ RESTART-RECEIVER: Error reiniciando servicio: ${e.message}")
            }
        } ?: debugLog.add("❌ RESTART-RECEIVER: Context es null")
    }
}