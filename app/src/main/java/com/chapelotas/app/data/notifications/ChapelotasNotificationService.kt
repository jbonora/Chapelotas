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
 * ACTUALIZADO para usar UnifiedMonkeyService con Room
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
        Log.d("ChapelotasService", "🚀 Servicio creado - Room Edition")

        // Adquirir WakeLock parcial para mantener CPU activa
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Chapelotas::MonkeyWakeLock"
        ).apply {
            acquire(10*60*1000L) // 10 minutos, se renueva automáticamente
        }

        // Emitir evento de servicio iniciado
        eventBus.tryEmit(ChapelotasEvent.ServiceStarted("ChapelotasNotificationService"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ChapelotasService", "📍 onStartCommand llamado")

        if (!isServiceStarted) {
            isServiceStarted = true
            startForegroundService()
            startMonkey()
            scheduleRestartAlarm() // Por si Android lo mata
        }

        // START_STICKY para máxima persistencia
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.w("ChapelotasService", "⚠️ Servicio destruido - Programando reinicio...")

        // Emitir evento
        eventBus.tryEmit(ChapelotasEvent.ServiceStopped(
            serviceName = "ChapelotasNotificationService",
            reason = "Android killed service"
        ))

        // Liberar recursos
        wakeLock?.release()
        serviceScope.cancel()
        isServiceStarted = false

        // Programar reinicio inmediato
        scheduleRestartAlarm(delayMillis = 1000) // 1 segundo

        // Enviar broadcast para reiniciar
        sendBroadcast(Intent("com.chapelotas.RESTART_SERVICE"))

        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.w("ChapelotasService", "📱 App removida de recientes - Manteniéndome vivo...")

        // Reiniciar el servicio cuando quitan la app de recientes
        val restartServiceIntent = Intent(applicationContext, ChapelotasNotificationService::class.java)
        restartServiceIntent.setPackage(packageName)

        val restartServicePendingIntent = PendingIntent.getService(
            applicationContext,
            1,
            restartServiceIntent,
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

    /**
     * Crear notificación invisible en canal de importancia NONE
     */
    private fun startForegroundService() {
        createInvisibleChannel()

        val notification = NotificationCompat.Builder(this, INVISIBLE_CHANNEL_ID)
            .setContentTitle("") // Sin título
            .setContentText("") // Sin texto
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setOngoing(true)
            .build()

        startForeground(FOREGROUND_SERVICE_ID, notification)

        // Renovar WakeLock periódicamente
        serviceScope.launch {
            while (isActive) {
                delay(5 * 60 * 1000) // Cada 5 minutos
                wakeLock?.let {
                    if (it.isHeld) it.release()
                    it.acquire(10*60*1000L)
                }
            }
        }
    }

    /**
     * Crear canal invisible
     */
    private fun createInvisibleChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                INVISIBLE_CHANNEL_ID,
                "Servicio Chapelotas",
                NotificationManager.IMPORTANCE_NONE // Invisible
            ).apply {
                description = "Mantiene Chapelotas activo"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Iniciar el mono unificado con Room
     */
    private fun startMonkey() {
        serviceScope.launch {
            try {
                // Esperar un poco para que todo se inicialice
                delay(3000)

                Log.d("ChapelotasService", "🐵 Iniciando Mono Unificado con Room...")
                unifiedMonkey.startMonkey()

                Log.d("ChapelotasService", "✅ Mono iniciado exitosamente")

            } catch (e: Exception) {
                Log.e("ChapelotasService", "❌ Error iniciando mono", e)

                // Emitir evento de error
                eventBus.tryEmit(ChapelotasEvent.MonkeyError(
                    error = e.message ?: "Unknown error",
                    willRetry = true,
                    retryInSeconds = 30
                ))

                // Reintentar en 30 segundos
                delay(30_000)
                startMonkey()
            }
        }
    }

    /**
     * Programar alarma para reiniciar el servicio si Android lo mata
     */
    private fun scheduleRestartAlarm(delayMillis: Long = 60000) { // 1 minuto por defecto
        val intent = Intent(this, RestartServiceReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            SERVICE_RESTART_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Usar alarma exacta para mayor confiabilidad
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

/**
 * Receiver para reiniciar el servicio
 * DEBE ESTAR EN EL MISMO ARCHIVO
 */
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