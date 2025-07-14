package com.chapelotas.app.presentation.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chapelotas.app.data.notifications.NotificationActionReceiver
import com.chapelotas.app.ui.theme.ChapelotasTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

@AndroidEntryPoint
class CriticalAlertActivity : ComponentActivity() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var proximityWakeLock: PowerManager.WakeLock? = null
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var notificationId: Long = 0
    private var eventId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar ventana para máxima prioridad
        setupWindow()

        // Obtener WakeLocks
        acquireWakeLocks()

        // Configurar como si fuera una llamada entrante
        setupAsIncomingCall()

        val message = intent.getStringExtra("message") ?: "¡EVENTO CRÍTICO!"
        eventId = intent.getStringExtra("event_id") ?: "0"
        notificationId = intent.getLongExtra("notification_id", 0)

        startAlarm()

        // Auto-cerrar después de 60 segundos si no hay respuesta
        activityScope.launch {
            delay(60000)
            if (!isFinishing) {
                stopAlarm()
                handleTimeout()
            }
        }

        setContent {
            ChapelotasTheme {
                Scaffold { padding ->
                    CriticalCallScreen(
                        modifier = Modifier.padding(padding),
                        message = message,
                        onAccept = {
                            stopAlarm()
                            handleDismiss()
                        },
                        onReject = {
                            stopAlarm()
                            handleDismiss()
                        }
                    )
                }
            }
        }
    }

    private fun setupWindow() {
        // Hacer que la actividad se muestre sobre la pantalla de bloqueo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)

            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        // Configuración adicional de ventana
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // Para Huawei específicamente
        if (Build.MANUFACTURER.equals("HUAWEI", ignoreCase = true)) {
            window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
        }
    }

    private fun acquireWakeLocks() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        // WakeLock principal
        wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
            "Chapelotas::CriticalAlertWakeLock"
        ).apply {
            acquire(60000) // 60 segundos máximo
        }

        // WakeLock de proximidad (como en llamadas reales)
        if (powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            proximityWakeLock = powerManager.newWakeLock(
                PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                "Chapelotas::ProximityWakeLock"
            ).apply {
                acquire(60000)
            }
        }
    }

    private fun setupAsIncomingCall() {
        // Configurar audio como llamada
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Guardar modo actual
        val previousMode = audioManager.mode
        val previousSpeaker = audioManager.isSpeakerphoneOn

        // Configurar como llamada entrante
        audioManager.mode = AudioManager.MODE_RINGTONE

        // Restaurar configuración al cerrar
        activityScope.launch {
            delay(500) // Dar tiempo para que se configure
            audioManager.mode = previousMode
            audioManager.isSpeakerphoneOn = previousSpeaker
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
        releaseWakeLocks()
        activityScope.cancel()
    }

    private fun releaseWakeLocks() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        proximityWakeLock?.let {
            if (it.isHeld) it.release()
        }
    }

    private fun handleDismiss() {
        val intent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DISMISS
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(NotificationActionReceiver.EXTRA_EVENT_ID, eventId)
        }
        sendBroadcast(intent)
        finishAndRemoveTask()
    }

    private fun handleTimeout() {
        // Registrar que se ignoró por timeout
        stopAlarm()
        finishAndRemoveTask()
    }

    private fun startAlarm() {
        try {
            // Usar el ringtone de llamada por defecto
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)

            // Para Huawei, forzar volumen alto
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
            audioManager.setStreamVolume(AudioManager.STREAM_RING, maxVolume, 0)

            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Vibración intensa como llamada
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        val vibrationPattern = longArrayOf(0, 1000, 1000) // Vibrar 1s, pausar 1s, repetir

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(vibrationPattern, 0)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(vibrationPattern, 0)
        }
    }

    private fun stopAlarm() {
        ringtone?.stop()
        vibrator?.cancel()
    }
}

@Composable
fun CriticalCallScreen(
    modifier: Modifier = Modifier,
    message: String,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(64.dp))
                Text(
                    text = "Chapelotas",
                    fontSize = 28.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Alerta Crítica",
                    fontSize = 18.sp,
                    color = Color.Gray
                )
            }

            Text(
                text = message,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = onReject,
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Ignorar", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Ignorar", color = Color.White)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Entendido", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Entendido", color = Color.White)
                }
            }
        }
    }
}