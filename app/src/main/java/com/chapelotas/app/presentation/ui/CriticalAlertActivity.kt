package com.chapelotas.app.presentation.ui

import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

/**
 * Actividad de alerta crítica - SIN INYECCIÓN para evitar conflictos
 */
@AndroidEntryPoint
class CriticalAlertActivity : ComponentActivity() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var notificationId: Long = 0
    private var eventId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar pantalla
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        // Obtener datos
        val message = intent.getStringExtra("message") ?: "¡EVENTO CRÍTICO!"
        eventId = intent.getLongExtra("event_id", 0).toString()
        notificationId = intent.getLongExtra("notification_id", 0)

        startAlarm()

        setContent {
            CriticalAlertScreen(
                message = message,
                onDismiss = {
                    stopAlarm()
                    handleDismiss()
                },
                onSnooze = {
                    stopAlarm()
                    handleSnooze()
                }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }

    private fun handleSnooze() {
        // Enviar broadcast al receiver
        val intent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(NotificationActionReceiver.EXTRA_EVENT_ID, eventId)
            putExtra(NotificationActionReceiver.EXTRA_SNOOZE_MINUTES, 5)
        }
        sendBroadcast(intent)
        finishAndRemoveTask()
    }

    private fun handleDismiss() {
        // Enviar broadcast al receiver
        val intent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DISMISS
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(NotificationActionReceiver.EXTRA_EVENT_ID, eventId)
        }
        sendBroadcast(intent)
        finishAndRemoveTask()
    }

    private fun startAlarm() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 1000, 500, 1000, 500, 1000),
                    0
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000), 0)
        }
    }

    private fun stopAlarm() {
        ringtone?.stop()
        vibrator?.cancel()
    }
}

@Composable
fun CriticalAlertScreen(
    message: String,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    var isBlinking by remember { mutableStateOf(true) }
    var secondsRemaining by remember { mutableStateOf(60) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            isBlinking = !isBlinking
        }
    }

    LaunchedEffect(Unit) {
        while (secondsRemaining > 0) {
            delay(1000)
            secondsRemaining--
        }
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isBlinking) Color.Red else Color(0xFFB71C1C))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = if (isBlinking) "🚨 ALERTA CRÍTICA 🚨" else "⚠️ ALERTA CRÍTICA ⚠️",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = message,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Auto-dismiss en $secondsRemaining segundos",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onSnooze,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Yellow,
                        contentColor = Color.Black
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("😴", fontSize = 24.sp)
                        Text("POSPONER", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("5 MIN", fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Green,
                        contentColor = Color.White
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("✅", fontSize = 24.sp)
                        Text("ENTENDIDO", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = "⚠️ ESTE EVENTO FUE MARCADO COMO CRÍTICO ⚠️\nNo te lo podés perder",
                    fontSize = 14.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}