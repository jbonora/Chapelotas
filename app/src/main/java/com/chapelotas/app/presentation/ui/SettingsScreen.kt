package com.chapelotas.app.presentation.ui

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chapelotas.app.presentation.viewmodels.CalendarMonitorViewModel
import com.chapelotas.app.presentation.viewmodels.MainViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    mainViewModel: MainViewModel,
    calendarViewModel: CalendarMonitorViewModel
) {
    val dayPlan by mainViewModel.dayPlan.collectAsStateWithLifecycle()
    val isMonitoring by calendarViewModel.isMonitoring.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = { Text("⚙️ Ajustes", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        )

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sección de Monitoreo
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isMonitoring)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🐵 Estado del Mono",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = isMonitoring,
                            onCheckedChange = {
                                if (it) calendarViewModel.iniciarDia()
                                else Toast.makeText(context, "Reinicia la app para detener", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    Text(
                        text = if (isMonitoring) "El mono está vigilando activamente" else "El mono está durmiendo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Sección de Personalidad
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🎭 Personalidad",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingRowSwitch(
                        title = "Modo Sarcástico 😏",
                        subtitle = if (dayPlan?.sarcasticMode == true) {
                            "Chapelotas está en modo argentino"
                        } else {
                            "Chapelotas está en modo profesional"
                        },
                        checked = dayPlan?.sarcasticMode ?: true,
                        onCheckedChange = { mainViewModel.setSarcasticMode(it) }
                    )
                }
            }

            // Sección de Horarios
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⏰ Horarios de Trabajo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingRowSwitch(
                        title = "Modo 24 Horas 🦉",
                        subtitle = if (dayPlan?.is24hMode == true) {
                            "El mono está trabajando todo el día (activado por defecto)"
                        } else {
                            "El mono respeta los horarios de trabajo configurados"
                        },
                        checked = dayPlan?.is24hMode ?: true,
                        onCheckedChange = { mainViewModel.set24hMode(it) }
                    )

                    if (dayPlan?.is24hMode == false) {
                        Spacer(modifier = Modifier.height(16.dp))

                        SettingRowTime(
                            title = "Hora de Inicio",
                            time = dayPlan?.workStartTime ?: LocalTime.of(9, 0),
                            onTimeChange = { mainViewModel.setWorkStartTime(it) }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SettingRowTime(
                            title = "Hora de Fin",
                            time = dayPlan?.workEndTime ?: LocalTime.of(18, 0),
                            onTimeChange = { mainViewModel.setWorkEndTime(it) }
                        )
                    }
                }
            }

            // Sección de Pruebas
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🧪 Zona de Pruebas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { mainViewModel.testCriticalCall() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📞 Probar Llamada Crítica (10 seg)")
                    }

                    Text(
                        text = "Simula una alerta crítica en 10 segundos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Sección de Mantenimiento
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🧹 Mantenimiento",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    var showClearDialog by remember { mutableStateOf(false) }

                    Button(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Borrar todo el historial de chat")
                    }

                    Text(
                        text = "⚠️ Esto eliminará toda la conversación con Chapelotas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    if (showClearDialog) {
                        AlertDialog(
                            onDismissRequest = { showClearDialog = false },
                            title = { Text("¿Borrar historial?") },
                            text = {
                                Text("Se eliminará toda la conversación con Chapelotas. Esta acción no se puede deshacer.")
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        mainViewModel.clearChatHistory()
                                        showClearDialog = false
                                        Toast.makeText(context, "Historial borrado", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Borrar todo")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showClearDialog = false }) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingRowSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingRowTime(
    title: String,
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit
) {
    val context = LocalContext.current
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
        TextButton(
            onClick = {
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        onTimeChange(LocalTime.of(hour, minute))
                    },
                    time.hour,
                    time.minute,
                    true
                ).show()
            }
        ) {
            Text(text = time.format(timeFormatter))
        }
    }
}