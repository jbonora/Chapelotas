package com.chapelotas.app.presentation.ui

import android.content.ContentUris
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.chapelotas.app.domain.models.LocationContext
import com.chapelotas.app.domain.models.Task
import com.chapelotas.app.presentation.ui.components.TaskInteractionPanel
import com.chapelotas.app.presentation.viewmodels.TaskDetailViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    navController: NavController,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel de Control") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    uiState.task?.let { task ->
                        if (task.isFromCalendar && task.calendarEventId != null) {
                            IconButton(onClick = {
                                val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, task.calendarEventId)
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No se pudo abrir el calendario.", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Abrir en Calendario")
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        uiState.task?.let { task ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                // 1. Panel de Información (Más compacto)
                TaskHeader(task = task, viewModel = viewModel)

                HorizontalDivider()

                // 2. Panel de Interacción (Chat y Acciones)
                TaskInteractionPanel(
                    task = task,
                    onAcknowledge = { viewModel.acknowledgeTask() },
                    onFinishOrReset = { viewModel.toggleFinishStatus() },
                    modifier = Modifier.weight(1f) // Ocupa el espacio restante
                )
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

// --- NUEVOS COMPONENTES VISUALES ---
// Estos componentes reemplazan y rediseñan la antigua `TaskInfoCard`

@Composable
private fun TaskHeader(task: Task, viewModel: TaskDetailViewModel) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        CustomTravelTimeDialog(
            task = task,
            onDismiss = { showDialog = false },
            onConfirm = { travelTime ->
                viewModel.updateTaskLocation("custom", travelTime)
                showDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Título y hora
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textDecoration = if (task.isFinished) TextDecoration.LineThrough else null,
                color = if (task.isFinished) Color.Gray else MaterialTheme.colorScheme.onSurface
            )
            if (task.isAcknowledged && !task.isFinished) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Tarea Aceptada",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Fila de Información
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Default.Schedule, contentDescription = "Hora", tint = MaterialTheme.colorScheme.primary)
            Text(
                text = task.scheduledTime.format(timeFormatter),
                style = MaterialTheme.typography.bodyLarge
            )
            if (task.travelTimeMinutes > 0) {
                Text(
                    text = "Viaje: ${task.travelTimeMinutes * 2} min (total)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Selector de ubicación (usando Chips para un look más moderno)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = "Ubicación", tint = MaterialTheme.colorScheme.primary)
            LocationChip("Ofi", task.locationContext == LocationContext.OFFICE) { viewModel.updateTaskLocation(LocationContext.OFFICE) }
            LocationChip("Cerca", task.locationContext == LocationContext.NEARBY) { viewModel.updateTaskLocation(LocationContext.NEARBY) }
            LocationChip("Lejos", task.locationContext == LocationContext.FAR) { viewModel.updateTaskLocation(LocationContext.FAR) }
            IconButton(onClick = { showDialog = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Create, contentDescription = "Tiempo personalizado")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(text) },
        leadingIcon = if (isSelected) {
            { Icon(imageVector = Icons.Filled.Done, contentDescription = "Seleccionado", modifier = Modifier.size(FilterChipDefaults.IconSize)) }
        } else {
            null
        },
        shape = RoundedCornerShape(16.dp)
    )
}

// --- DIÁLOGO DE TIEMPO (Movido aquí) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTravelTimeDialog(task: Task, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var textValue by remember { mutableStateOf(if (task.travelTimeMinutes > 0) task.travelTimeMinutes.toString() else "") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Tiempo de Viaje (Ida)") },
        text = {
            Column {
                Text("Ingresa los minutos de viaje solo de ida para '${task.title}'.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = textValue, onValueChange = { textValue = it.filter { char -> char.isDigit() } }, label = { Text("Minutos") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onConfirm(textValue.toIntOrNull() ?: 0) }, enabled = textValue.isNotBlank()) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}