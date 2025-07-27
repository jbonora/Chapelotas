package com.chapelotas.app.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chapelotas.app.domain.debug.DebugLog
import com.chapelotas.app.domain.models.Task
import com.chapelotas.app.domain.models.TaskStatus
import com.chapelotas.app.presentation.viewmodels.DebugViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    viewModel: DebugViewModel = hiltViewModel()
) {
    val allTasks by viewModel.allTasks.collectAsState()
    val logs by DebugLog.logs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("🐛 Vista de Debug") })
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            Text("Fuente de la Verdad (Tasks en BD)", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)) {
                items(allTasks, key = { it.id }) { task ->
                    DebugTaskCard(task)
                }
            }

            Text("Caja Negra (Logs en tiempo real)", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black).padding(8.dp)) {
                items(logs) { log ->
                    Text(
                        text = log,
                        color = Color.Green,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DebugTaskCard(task: Task) {
    val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss")

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(task.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text("ID: ${task.id}", fontSize = 10.sp, color = Color.Gray)
            Divider(modifier = Modifier.padding(vertical = 4.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Estado: ${task.status.name}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Aceptado: ${if (task.isAcknowledged) "✅" else "❌"}", fontSize = 12.sp)
                Text("Terminado: ${if (task.isFinished) "✅" else "❌"}", fontSize = 12.sp)
                Text("Avisos: ${task.reminderCount}", fontSize = 12.sp)
            }

            task.lastReminderAt?.let {
                Text(
                    "Último aviso: ${it.format(dateTimeFormatter)}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // --- LÓGICA CORREGIDA ---
            // Si la tarea está terminada, muestra "Terminada".
            // Si no, muestra el próximo aviso si existe.
            if (task.isFinished) {
                Text(
                    "Próximo aviso: Terminada",
                    fontSize = 12.sp,
                    color = Color(0xFF00C853), // Un verde para indicar éxito
                    fontWeight = FontWeight.Bold
                )
            } else {
                task.nextReminderAt?.let {
                    Text(
                        "Próximo aviso: ${it.format(dateTimeFormatter)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            // ------------------------
        }
    }
}