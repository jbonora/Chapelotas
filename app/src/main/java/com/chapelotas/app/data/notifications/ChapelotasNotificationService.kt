package com.chapelotas.app.data.notifications

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.chapelotas.app.R
import com.chapelotas.app.domain.debug.DebugLog
import com.chapelotas.app.domain.usecases.CalendarSyncUseCase
import com.chapelotas.app.domain.usecases.ReminderEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class ChapelotasNotificationService : Service() {

    @Inject
    lateinit var reminderEngine: ReminderEngine
    @Inject
    lateinit var calendarSyncUseCase: CalendarSyncUseCase
    @Inject
    lateinit var debugLog: DebugLog

    private var serviceJob = SupervisorJob()
    private var serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    override fun onCreate() {
        super.onCreate()
        debugLog.add("✅ SERVICIO: onCreate - El servicio se está creando.")
        scheduleHeartbeatAlarm()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        debugLog.add("🚀 SERVICIO: onStartCommand - El servicio ha arrancado.")
        startForegroundService()

        if (serviceJob.isCancelled) {
            serviceJob = SupervisorJob()
            serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
        }

        debugLog.add("🏁 SERVICIO: Iniciando bucles de trabajo.")
        startReminderLoop()
        startCalendarMonitoring()

        return START_STICKY
    }

    override fun onDestroy() {
        debugLog.add("💀 SERVICIO: onDestroy - ¡El sistema está matando el servicio!")
        serviceJob.cancel()
        // Se mantiene la lógica de resurrección inmediata.
        val broadcastIntent = Intent(this, RestartServiceReceiver::class.java)
        sendBroadcast(broadcastIntent)
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        debugLog.add("👋 SERVICIO: onTaskRemoved - El usuario cerró la app. Dejando ancla para revivir.")
        val restartServiceIntent = Intent(applicationContext, this::class.java).apply {
            setPackage(packageName)
        }
        val restartServicePendingIntent = PendingIntent.getService(
            this,
            1,
            restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmService = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmService.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 2000,
            restartServicePendingIntent
        )
        super.onTaskRemoved(rootIntent)
    }

    private fun scheduleHeartbeatAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, HeartbeatReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            HEARTBEAT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + HEARTBEAT_INTERVAL,
            HEARTBEAT_INTERVAL,
            pendingIntent
        )
        debugLog.add("❤️ HEARTBEAT: Alarma programada para ejecutarse cada 1 hora.")
    }

    private fun startReminderLoop() {
        debugLog.add("⚙️ BUCLE-RECORDATORIOS: Iniciando ciclo del ReminderEngine.")
        serviceScope.launch {
            while (isActive) {
                try {
                    debugLog.add("⏰ BUCLE-RECORDATORIOS: Verificando tareas...")
                    reminderEngine.checkAndSendReminders()
                } catch (e: Exception) {
                    debugLog.add("❌ BUCLE-RECORDATORIOS: Error - ${e.message}")
                }
                delay(60 * 1000)
            }
        }
    }

    private fun startCalendarMonitoring() {
        debugLog.add("👁️ BUCLE-CALENDARIO: Activando monitoreo en tiempo real.")
        serviceScope.launch {
            try {
                calendarSyncUseCase.startMonitoring()
            } catch (e: Exception) {
                debugLog.add("❌ BUCLE-CALENDARIO: Error - ${e.message}")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundService() {
        createInvisibleChannel()
        val notification = NotificationCompat.Builder(this, INVISIBLE_CHANNEL_ID)
            .setContentTitle("Chapelotas está vigilando")
            .setContentText("El servicio está activo para no perderse nada.")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
        startForeground(FOREGROUND_SERVICE_ID, notification)
    }

    private fun createInvisibleChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                INVISIBLE_CHANNEL_ID,
                "Servicio Chapelotas",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Mantiene Chapelotas activo en segundo plano."
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val INVISIBLE_CHANNEL_ID = "chapelotas_service_channel"
        private const val FOREGROUND_SERVICE_ID = 1337
        private const val HEARTBEAT_REQUEST_CODE = 999
        // --- INICIO DE LA MODIFICACIÓN ---
        private const val HEARTBEAT_INTERVAL = 60 * 60 * 1000L // 1 hora
        // --- FIN DE LA MODIFICACIÓN ---
    }
}

/**
 * Este receiver es la última línea de defensa. Es llamado por onDestroy
 * para intentar reiniciar el servicio inmediatamente.
 */
class RestartServiceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val debugLog = DebugLog
        debugLog.add("🔄 RESTART-RECEIVER: Recibida señal para reiniciar el servicio.")
        context?.let { ctx ->
            val serviceIntent = Intent(ctx, ChapelotasNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(serviceIntent)
            } else {
                ctx.startService(serviceIntent)
            }
        }
    }
}