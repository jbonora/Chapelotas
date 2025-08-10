package com.chapelotas.app.presentation.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import com.chapelotas.app.MainActivity
import com.chapelotas.app.data.alarms.AlarmReceiver
import com.chapelotas.app.data.alarms.SystemAlarmWorker
import com.chapelotas.app.domain.debug.DebugLog
import com.chapelotas.app.domain.models.Task
import com.chapelotas.app.presentation.ui.theme.ChapelotasTheme
import com.chapelotas.app.presentation.viewmodels.AlarmViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {
    private val viewModel: AlarmViewModel by viewModels()
    @Inject
    lateinit var debugLog: DebugLog

    private lateinit var ringtone: Ringtone
    private lateinit var vibrator: Vibrator
    private lateinit var audioManager: AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private var ringtoneRunnable: Runnable? = null
    private var volumeRunnable: Runnable? = null
    private var originalVolume: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar para mostrar sobre pantalla de bloqueo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        startAlarmSound()
        startVibration()

        val alarmTime = intent.getStringExtra("alarm_time") ?: LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        val isSarcasticMode = intent.getBooleanExtra("sarcastic_mode", true)

        setContent {
            val todayTasks by viewModel.todayTasks.collectAsState()
            ChapelotasTheme {
                AlarmScreen(
                    alarmTime = alarmTime,
                    todayTasks = todayTasks,
                    isSarcasticMode = isSarcasticMode,
                    onStop = { stopAlarm() },
                    onSnooze = { snoozeAlarm() }
                )
            }
        }
    }

    private fun startAlarmSound() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)

        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                isLooping = true
            }
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        }

        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        var currentVolume = maxVolume / 4
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, currentVolume, 0)

        ringtone.play()

        volumeRunnable = object : Runnable {
            override fun run() {
                if (currentVolume < maxVolume && ringtone.isPlaying) {
                    currentVolume = minOf(currentVolume + 1, maxVolume)
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, currentVolume, 0)
                    handler.postDelayed(this, 2000)
                }
            }
        }
        handler.postDelayed(volumeRunnable!!, 2000)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            ringtoneRunnable = object : Runnable {
                override fun run() {
                    if (!ringtone.isPlaying) {
                        ringtone.play()
                    }
                    handler.postDelayed(this, 1000)
                }
            }
            handler.postDelayed(ringtoneRunnable!!, 1000)
        }
    }

    private fun startVibration() {
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val pattern = longArrayOf(0, 1000, 1000)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, 0)
        }
    }

    private fun stopAlarm() {
        cleanUp()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun snoozeAlarm() {
        cleanUp()

        val alarmManager = getSystemService<AlarmManager>()
        val snoozeMinutes = intent.getIntExtra("snooze_minutes", 10)
        val snoozeTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000)

        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("is_snooze", true)
            putExtra("alarm_time", LocalTime.now().plusMinutes(snoozeMinutes.toLong()).toString())
            // Pasar los extras de nuevo para el siguiente snooze
            putExtra("sarcastic_mode", getIntent().getBooleanExtra("sarcastic_mode", true))
            putExtra("snooze_minutes", snoozeMinutes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            SystemAlarmWorker.ALARM_REQUEST_CODE + 1, // ID diferente para snooze
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager?.setAlarmClock(
            AlarmManager.AlarmClockInfo(snoozeTime, pendingIntent),
            pendingIntent
        )

        Toast.makeText(this, "Alarma pospuesta $snoozeMinutes minutos", Toast.LENGTH_SHORT).show()
        debugLog.add("⏰ Alarma pospuesta $snoozeMinutes minutos")

        finish()
    }

    private fun cleanUp() {
        if (::ringtone.isInitialized && ringtone.isPlaying) {
            ringtone.stop()
        }
        if (::vibrator.isInitialized) {
            vibrator.cancel()
        }
        if(::audioManager.isInitialized) {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0)
        }
        volumeRunnable?.let { handler.removeCallbacks(it) }
        ringtoneRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanUp()
    }
}

@Composable
fun AlarmScreen(
    alarmTime: String,
    todayTasks: List<Task> = emptyList(),
    isSarcasticMode: Boolean = false,
    onStop: () -> Unit,
    onSnooze: () -> Unit
) {
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    var snoozeCount by remember { mutableStateOf(0) }
    val pendingTasks = todayTasks.filter { !it.isFinished }

    val greetingMessages = if (isSarcasticMode) {
        listOf(
            "Oh, mira quién decidió despertar...",
            "¿Ya? ¿En serio? Bueno, levántate...",
            "Las tareas no se hacen solas, campeón",
            "Otro día, otra oportunidad de procrastinar"
        )
    } else {
        listOf(
            "¡Buenos días! ☀️",
            "¡Hora de empezar el día!",
            "¡Arriba! Un gran día te espera"
        )
    }

    // --- INICIO DE LA MODIFICACIÓN ---
    // Se elige la frase una sola vez y se guarda para evitar que cambie
    val greetingMessage = remember { greetingMessages.random() }
    // --- FIN DE LA MODIFICACIÓN ---

    val taskMessage = when (pendingTasks.size) {
        0 -> if (isSarcasticMode) "¡Increíble! No tienes tareas. ¿Qué haces despierto?" else "¡Día libre! No hay tareas pendientes 🎉"
        1 -> "1 tarea te espera: ${pendingTasks[0].title}"
        else -> "${pendingTasks.size} tareas pendientes hoy"
    }

    val snoozeMessages = if (isSarcasticMode) {
        when (snoozeCount) {
            0 -> "Snooze = Procrastinación. Tú decides..."
            1 -> "¿Otra vez? Las tareas siguen esperando..."
            2 -> "Esto se está volviendo un hábito..."
            else -> "Ok, claramente no es tu día..."
        }
    } else {
        "Descansa 10 minutos más"
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            // --- MODIFICACIÓN: Reducir espaciado para dar más lugar ---
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = currentTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                // --- MODIFICACIÓN: Usar la frase guardada y ajustar tamaño ---
                text = greetingMessage,
                fontSize = 20.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = taskMessage,
                        fontSize = 18.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    if (pendingTasks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val firstTask = pendingTasks.first()
                        Text(
                            text = "Primera: ${firstTask.scheduledTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onStop,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(80.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (pendingTasks.isEmpty()) "CERRAR" else "VER TAREAS",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = {
                    snoozeCount++
                    onSnooze()
                },
                modifier = Modifier.fillMaxWidth(0.85f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text(snoozeMessages)
            }
        }
    }
}