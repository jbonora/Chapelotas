package com.chapelotas.app.presentation.ui

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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

/**
 * Actividad de alerta crítica que simula una llamada entrante
 * No se puede ignorar fácilmente
 */
@AndroidEntryPoint
class CriticalAlertActivity : ComponentActivity() {
    
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configurar la pantalla para que se muestre incluso con el teléfono bloqueado
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            // Para APIs antiguas, usar los flags deprecados
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        
        // Estos flags funcionan en todas las versiones
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
        
        // Obtener el mensaje de la notificación
        val message = intent.getStringExtra("message") ?: "¡EVENTO CRÍTICO!"
        
        // Iniciar sonido y vibración
        startAlarm()
        
        setContent {
            CriticalAlertScreen(
                message = message,
                onDismiss = {
                    stopAlarm()
                    finish()
                },
                onSnooze = {
                    stopAlarm()
                    // TODO: Implementar snooze
                    finish()
                }
            )
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }
    
    private fun startAlarm() {
        // Sonido
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Vibración
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 1000, 500, 1000, 500, 1000),
                    0 // Repetir desde el índice 0
                )
            )
        } else {
            // Para APIs antiguas
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
    
    // Efecto de parpadeo
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            isBlinking = !isBlinking
        }
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
            // Título
            Text(
                text = "🚨 ALERTA CRÍTICA 🚨",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            // Mensaje
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(24.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
            }
            
            // Botones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Botón Snooze
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
                    Text(
                        text = "POSPONER\n5 MIN",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                
                // Botón Dismiss
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
                    Text(
                        text = "ENTENDIDO",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            
            // Texto de advertencia
            Text(
                text = "⚠️ ESTE EVENTO FUE MARCADO COMO CRÍTICO ⚠️",
                fontSize = 14.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}