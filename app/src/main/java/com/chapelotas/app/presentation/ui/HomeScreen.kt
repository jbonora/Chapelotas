package com.chapelotas.app.presentation.ui.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.chapelotas.app.battery.BatteryProtectionManager
import com.chapelotas.app.domain.models.Task
import com.chapelotas.app.presentation.viewmodels.MainViewModel
import kotlinx.coroutines.delay
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    task: Task,
    onAcknowledgeClick: () -> Unit,
    onFinishClick: () -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val dayFormatter = DateTimeFormatter.ofPattern("EEEE d", Locale("es", "ES"))

    var relativeTimeText by remember { mutableStateOf(getRelativeTimeText(task)) }
    LaunchedEffect(task) {
        while (true) {
            relativeTimeText = getRelativeTimeText(task)
            delay(1000)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = getStatusColor(task))
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
                    text = relativeTimeText,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun getRelativeTimeText(task: Task): String {
    val now = LocalDateTime.now()
    val endTime = task.endTime ?: task.scheduledTime.plusHours(1)

    return when {
        task.isFinished -> "Terminada"
        now.isBefore(task.scheduledTime) -> {
            val duration = Duration.between(now, task.scheduledTime)
            if (duration.toDays() > 0) "Faltan ${duration.toDays()}d ${duration.toHours() % 24}h"
            else if (duration.toHours() > 0) "Faltan ${duration.toHours()}h ${duration.toMinutes() % 60}m"
            else "Faltan ${duration.toMinutes() + 1}m"
        }
        now.isAfter(endTime) -> {
            val duration = Duration.between(endTime, now)
            if (duration.toMinutes() < 1) return if (task.isAcknowledged) "Demorada" else "Omitida"
            if (duration.toDays() > 0) "Pasaron ${duration.toDays()}d ${duration.toHours() % 24}h"
            else if (duration.toHours() > 0) "Pasaron ${duration.toHours()}h ${duration.toMinutes() % 60}m"
            else "Pasaron ${duration.toMinutes()}m"
        }
        else -> "En curso"
    }
}

@Composable
fun getStatusColor(task: Task): Color {
    val now = LocalDateTime.now()
    val endTime = task.endTime ?: task.scheduledTime.plusHours(1)

    return when {
        task.isFinished -> MaterialTheme.colorScheme.surfaceVariant
        now.isAfter(endTime) -> MaterialTheme.colorScheme.errorContainer
        task.isAcknowledged -> MaterialTheme.colorScheme.secondaryContainer
        now.isAfter(task.scheduledTime) -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
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