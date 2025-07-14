package com.chapelotas.app.data.notifications

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.chapelotas.app.R
import com.chapelotas.app.domain.events.ChapelotasEvent
import com.chapelotas.app.domain.events.ChapelotasEventBus
import com.chapelotas.app.domain.usecases.UnifiedMonkeyService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Servicio ultra-persistente estilo WhatsApp
 * ACTUALIZADO para trabajar con la arquitectura de AlarmManager
 */
@AndroidEntryPoint
class ChapelotasNotificationService : Service() {

    @Inject
    lateinit var unifiedMonkey: UnifiedMonkeyService

    @Inject
    lateinit var eventBus: ChapelotasEventBus

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false

    override fun onCreate() {
        super.onCreate()
        Log.d("ChapelotasService", "🚀 Servicio creado - Edición Bello Durmiente")

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Chapelotas::MonkeyWakeLock"
        ).apply {
            acquire(10*60*1000L)
        }
        eventBus.tryEmit(ChapelotasEvent.ServiceStarted("ChapelotasNotificationService"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ChapelotasService", "📍 onStartCommand llamado")

        if (!isServiceStarted) {
            isServiceStarted = true
            startForegroundService()
            // --- CAMBIO CLAVE: Ya no llamamos a startMonkey() ---
            // En su lugar, nos aseguramos de que la próxima alarma esté programada.
            // Esto es crucial por si el servicio se reinicia y no hay alarmas pendientes.
            serviceScope.launch {
                unifiedMonkey.scheduleNextAlarm()
            }
            scheduleRestartAlarm()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.w("ChapelotasService", "⚠️ Servicio destruido - Programando reinicio...")
        eventBus.tryEmit(ChapelotasEvent.ServiceStopped(
            serviceName = "ChapelotasNotificationService",
            reason = "Android killed service"
        ))
        wakeLock?.release()
        serviceScope.cancel()
        isServiceStarted = false
        scheduleRestartAlarm(delayMillis = 1000)

        // --- CAMBIO CLAVE: Hacemos el intent explícito ---
        val restartIntent = Intent(this, RestartServiceReceiver::class.java)
        sendBroadcast(restartIntent)

        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.w("ChapelotasService", "📱 App removida de recientes - Manteniéndome vivo...")
        val restartServiceIntent = Intent(applicationContext, ChapelotasNotificationService::class.java).apply {
            setPackage(packageName)
        }
        val restartServicePendingIntent = PendingIntent.getService(
            applicationContext, 1, restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmService = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmService.set(
            AlarmManager.ELAPSED_REALTIME,
            android.os.SystemClock.elapsedRealtime() + 1000,
            restartServicePendingIntent
        )
        super.onTaskRemoved(rootIntent)
    }

    private fun startForegroundService() {
        createInvisibleChannel()
        val notification = NotificationCompat.Builder(this, INVISIBLE_CHANNEL_ID)
            .setContentTitle("")
            .setContentText("")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setOngoing(true)
            .build()
        startForeground(FOREGROUND_SERVICE_ID, notification)

        serviceScope.launch {
            while (isActive) {
                delay(5 * 60 * 1000)
                wakeLock?.let {
                    if (it.isHeld) it.release()
                    it.acquire(10*60*1000L)
                }
            }
        }
    }

    private fun createInvisibleChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                INVISIBLE_CHANNEL_ID,
                "Servicio Chapelotas",
                NotificationManager.IMPORTANCE_NONE
            ).apply {
                description = "Mantiene Chapelotas activo"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun scheduleRestartAlarm(delayMillis: Long = 60000) {
        val intent = Intent(this, RestartServiceReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            SERVICE_RESTART_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + delayMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + delayMillis,
                pendingIntent
            )
        }
    }

    companion object {
        private const val INVISIBLE_CHANNEL_ID = "chapelotas_invisible"
        private const val FOREGROUND_SERVICE_ID = 1337
        private const val SERVICE_RESTART_CODE = 1984
    }
}

class RestartServiceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("RestartReceiver", "🔄 Reiniciando servicio...")
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