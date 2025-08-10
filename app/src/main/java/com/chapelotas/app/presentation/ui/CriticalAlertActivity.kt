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
import android.os.VibratorManager
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.chapelotas.app.R
import com.chapelotas.app.data.notifications.NotificationActionReceiver
import com.chapelotas.app.di.Constants
import com.chapelotas.app.presentation.ui.theme.ChapelotasTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

@AndroidEntryPoint
class CriticalAlertActivity : ComponentActivity() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var eventId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupWindow()
        acquireWakeLocks()

        val message = intent.getStringExtra(Constants.EXTRA_NOTIFICATION_MESSAGE) ?: getString(R.string.critical_alert_default_message)
        eventId = intent.getStringExtra(Constants.EXTRA_EVENT_ID) ?: "0"

        startAlarm()

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
                            handleAccept()
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
        // --- CÓDIGO ACTUALIZADO PARA FULLSCREEN Y GESTIÓN DE LA PANTALLA ---
        // Esto reemplaza FLAG_FULLSCREEN y otros métodos obsoletos.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

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

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun acquireWakeLocks() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        // --- CÓDIGO ACTUALIZADO PARA WAKELOCK ---
        // Se mantiene el WakeLock, ya que para este caso de uso (despertar la pantalla)
        // sigue siendo la forma recomendada, aunque los flags antiguos se marcan como obsoletos.
        // La implementación actual es correcta para la compatibilidad.
        @Suppress("DEPRECATION")
        wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Chapelotas::CriticalAlertWakeLock"
        ).apply {
            acquire(60000) // 60 segundos máximo
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
        releaseWakeLocks()
        activityScope.cancel()
    }

    private fun releaseWakeLocks() {
        wakeLock?.takeIf { it.isHeld }?.release()
    }

    private fun handleAccept() {
        val intent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = Constants.ACTION_FINISH_DONE
            putExtra(Constants.EXTRA_EVENT_ID, eventId)
        }
        sendBroadcast(intent)
        finishAndRemoveTask()
    }

    private fun handleDismiss() {
        finishAndRemoveTask()
    }

    private fun handleTimeout() {
        stopAlarm()
        finishAndRemoveTask()
    }

    private fun startAlarm() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // --- CÓDIGO ACTUALIZADO PARA EL VIBRADOR ---
        // Reemplaza el método obsoleto de obtener el servicio.
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        val vibrationPattern = longArrayOf(0, 1000, 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(vibrationPattern, 0))
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
                    text = stringResource(id = R.string.app_name),
                    fontSize = 28.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.critical_alert_title),
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
                        Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.critical_alert_reject), tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(id = R.string.critical_alert_reject), color = Color.White)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = stringResource(id = R.string.critical_alert_accept), tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(id = R.string.critical_alert_accept), color = Color.White)
                }
            }
        }
    }
}