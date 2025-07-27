package com.chapelotas.app.presentation.ui.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.chapelotas.app.battery.BatteryProtectionManager
import com.chapelotas.app.domain.models.Task
import com.chapelotas.app.domain.models.TaskStatus
import com.chapelotas.app.presentation.viewmodels.MainViewModel
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val viewModel: MainViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tasks by viewModel.todayTasks.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    if (uiState.showBatteryProtectionDialog) {
        LaunchedEffect(Unit) {
            BatteryProtectionManager.showBatteryProtectionDialog(context) {
                viewModel.onBatteryProtectionDialogDismissed()
            }
        }
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.onPermissionGranted()
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🐵 Chapelotas")
                        Spacer(modifier = Modifier.width(8.dp))
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Más"
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Configuración") },
                            onClick = {
                                showMenu = false
                                navController.navigate("settings")
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = null
                                )
                            })
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when {
                uiState.needsPermission -> {
                    PermissionScreen(
                        onRequestPermission = {
                            calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                        }
                    )
                }
                tasks.isEmpty() && !uiState.isLoading -> {
                    EmptyScreen()
                }
                else -> {
                    TaskList(
                        tasks = tasks,
                        onAcknowledgeClick = { taskId -> viewModel.acknowledgeTask(taskId) },
                        onFinishClick = { task -> viewModel.toggleFinishStatus(task) }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskList(
    modifier: Modifier = Modifier,
    tasks: List<Task>,
    onAcknowledgeClick: (String) -> Unit,
    onFinishClick: (Task) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tasks, key = { it.id }) { task ->
            TaskCard(
                task = task,
                onAcknowledgeClick = { onAcknowledgeClick(task.id) },
                onFinishClick = { onFinishClick(task) }
            )
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    onAcknowledgeClick: () -> Unit,
    onFinishClick: () -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val dayFormatter = DateTimeFormatter.ofPattern("EEEE d", Locale("es", "ES"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = task.status.toColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleLarge,
                    textDecoration = if (task.isFinished) TextDecoration.LineThrough else null,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    Icon(
                        imageVector = if (task.isAcknowledged) Icons.Filled.Check else Icons.Outlined.Check,
                        contentDescription = "Aceptar Tarea",
                        tint = if (task.isAcknowledged) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onAcknowledgeClick
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (task.isFinished) Icons.Filled.CheckCircle else Icons.Outlined.Check,
                        contentDescription = "Finalizar Tarea",
                        tint = if (task.isFinished) Color(0xFF00C853) else Color.Gray.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onFinishClick
                            )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${task.scheduledTime.format(dayFormatter).replaceFirstChar { it.titlecase() }} a las ${task.scheduledTime.format(timeFormatter)} hs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = getRelativeTimeText(task),
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TaskStatus.toColor(): Color {
    return when (this) {
        TaskStatus.FINISHED -> MaterialTheme.colorScheme.surfaceVariant
        // --- LA LÍNEA CORREGIDA ---
        // Se elimina la referencia a 'MISSED', ya que 'DELAYED' cubre todos los casos de tareas pasadas
        TaskStatus.DELAYED -> MaterialTheme.colorScheme.errorContainer
        TaskStatus.ONGOING -> MaterialTheme.colorScheme.primaryContainer
        TaskStatus.UPCOMING -> MaterialTheme.colorScheme.surface
    }
}

fun getRelativeTimeText(task: Task): String {
    val now = LocalDateTime.now()
    return when (task.status) {
        TaskStatus.FINISHED -> "Terminada"
        TaskStatus.UPCOMING -> {
            val duration = Duration.between(now, task.scheduledTime)
            when {
                duration.toDays() > 0 -> "Faltan ${duration.toDays()}d ${duration.toHours() % 24}h"
                duration.toHours() > 0 -> "Faltan ${duration.toHours()}h ${duration.toMinutes() % 60}m"
                else -> "Faltan ${duration.toMinutes() + 1}m"
            }
        }
        TaskStatus.ONGOING -> "En curso"
        // --- LA LÍNEA CORREGIDA ---
        // Se elimina la referencia a 'MISSED'
        TaskStatus.DELAYED -> "Demorada"
    }
}

@Composable
fun PermissionScreen(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.DateRange,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Necesito acceso a tu calendario",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Para poder recordarte tus eventos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRequestPermission) {
                Text("Dar permiso")
            }
        }
    }
}

@Composable
fun EmptyScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "🎉",
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = "¡No tenés eventos hoy!",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Disfrutá tu día libre",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}