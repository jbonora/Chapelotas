package com.chapelotas.app.presentation.ui

import android.content.ContentUris
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
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
import com.chapelotas.app.presentation.ui.home.components.TaskControlPanel
import com.chapelotas.app.presentation.viewmodels.TaskDetailViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    navController: NavController,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    uiState.task?.let { task ->
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

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Detalle de Tarea") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    // --- INICIO DE LA CORRECCIÓN 1: Botón para ir al Calendario ---
                    actions = {
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
                    // --- FIN DE LA CORRECCIÓN 1 ---
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(1.dp)) } // Spacer para el padding superior
                item {
                    TaskInfoCard(
                        task = task,
                        onLocationSelected = { context ->
                            viewModel.updateTaskLocation(context)
                        },
                        onCustomTimeClicked = {
                            showDialog = true
                        }
                    )
                }
                item {
                    TaskControlPanel(
                        task = task,
                        onAcknowledge = { viewModel.acknowledgeTask() },
                        onFinishOrReset = { viewModel.toggleFinishStatus() }
                    )
                }
                item {
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp)
                    ) {
                        Text("Volver")
                    }
                }
            }
        }
    } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun TaskInfoCard(
    task: Task,
    onLocationSelected: (String) -> Unit,
    onCustomTimeClicked: () -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (task.isFinished) TextDecoration.LineThrough else null,
                    color = if (task.isFinished) Color.Gray else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                if(task.isAcknowledged && !task.isFinished){
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Tarea Aceptada",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = task.scheduledTime.format(timeFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (task.travelTimeMinutes > 0) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Viaje: ${task.travelTimeMinutes * 2} min (total)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("¿Dónde es el evento?", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))

            // --- INICIO DE LA CORRECCIÓN 2: Botones de Distancia ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                // Usamos SpaceBetween para que se distribuyan uniformemente
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quitamos el .weight(1f) para que cada botón tome su tamaño natural
                LocationButton("Ofi", task.locationContext == LocationContext.OFFICE, { onLocationSelected(LocationContext.OFFICE) })
                LocationButton("Cerca", task.locationContext == LocationContext.NEARBY, { onLocationSelected(LocationContext.NEARBY) })
                LocationButton("Lejos", task.locationContext == LocationContext.FAR, { onLocationSelected(LocationContext.FAR) })
                IconButton(onClick = onCustomTimeClicked) {
                    Icon(Icons.Default.Create, contentDescription = "Tiempo personalizado")
                }
            }
            // --- FIN DE LA CORRECCIÓN 2 ---
        }
    }
}

@Composable
fun LocationButton(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    // Usamos un OutlinedButton que se adapta mejor al contenido
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = MaterialTheme.shapes.medium,
        // Reducimos el padding horizontal para dar más espacio al texto
        contentPadding = PaddingValues(horizontal = 12.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = backgroundColor, contentColor = contentColor),
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha=0.5f))
    ) {
        Text(text, maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTravelTimeDialog(
    task: Task,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var textValue by remember { mutableStateOf(if (task.travelTimeMinutes > 0) task.travelTimeMinutes.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tiempo de Viaje (Ida)") },
        text = {
            Column {
                Text("Ingresa los minutos de viaje solo de ida para '${task.title}'.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it.filter { char -> char.isDigit() } },
                    label = { Text("Minutos") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minutes = textValue.toIntOrNull() ?: 0
                    onConfirm(minutes)
                },
                enabled = textValue.isNotBlank()
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}