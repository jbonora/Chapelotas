package com.chapelotas.app.presentation.ui

import android.content.ContentUris
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chapelotas.app.data.database.entities.EventDistance
import com.chapelotas.app.data.database.entities.EventPlan
import com.chapelotas.app.data.database.entities.EventResolutionStatus
import com.chapelotas.app.data.database.entities.NotificationPriority
import com.chapelotas.app.data.database.entities.ScheduledNotification
import com.chapelotas.app.presentation.viewmodels.CalendarMonitorViewModel
import com.chapelotas.app.presentation.viewmodels.MainViewModel
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    mainViewModel: MainViewModel,
    calendarMonitorViewModel: CalendarMonitorViewModel
) {
    val eventPlans by calendarMonitorViewModel.todayEvents.collectAsStateWithLifecycle()
    val activeConflicts by calendarMonitorViewModel.activeConflicts.collectAsStateWithLifecycle()
    val activeNotifications by calendarMonitorViewModel.activeNotifications.collectAsStateWithLifecycle()
    val isMonitoring by calendarMonitorViewModel.isMonitoring.collectAsStateWithLifecycle()
    val nextCheckTime by calendarMonitorViewModel.nextCheckTime.collectAsStateWithLifecycle()

    Column {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔔 Plan del Día", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    Badge(containerColor = if (isMonitoring) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer) {
                        Text(if (isMonitoring) "🐵" else "⚠️", fontSize = 10.sp)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (isMonitoring) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isMonitoring) "🐵 Mono Vigilando" else "⚠️ Mono Detenido", fontWeight = FontWeight.Bold)
                            val nextCheckText = nextCheckTime?.let {
                                val duration = Duration.between(LocalDateTime.now(ZoneId.systemDefault()), it)
                                if (duration.isNegative) "Ahora" else "en ${duration.toMinutes() + 1} min"
                            } ?: "--:--"
                            Text("Próximo aviso: $nextCheckText", style = MaterialTheme.typography.bodySmall)
                        }
                        if (activeConflicts.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("⚠️ Conflictos detectados: ${activeConflicts.size}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        if (eventPlans.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            val completedCount = eventPlans.count { it.resolutionStatus == EventResolutionStatus.COMPLETED }
                            val pendingCount = eventPlans.size - completedCount
                            Text("📅 $pendingCount pendientes, $completedCount completados", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (activeConflicts.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("⚠️ Conflictos Detectados", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.height(8.dp))
                            activeConflicts.forEach { conflict ->
                                Text(conflict.getUserMessage(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }
            }

            items(eventPlans) { eventPlan ->
                EventCardWithRoom(
                    eventPlan = eventPlan,
                    notificationsForEvent = activeNotifications.filter { it.eventId == eventPlan.eventId },
                    onToggleCritical = {
                        if (eventPlan.resolutionStatus != EventResolutionStatus.COMPLETED) {
                            calendarMonitorViewModel.actualizarCriticidad(eventPlan.eventId, !eventPlan.isCritical)
                        }
                    },
                    onApproveAiPlan = { mainViewModel.approveAiPlan(eventPlan.eventId) },
                    onUpdateDistance = { distanceName ->
                        if (eventPlan.resolutionStatus != EventResolutionStatus.COMPLETED) {
                            calendarMonitorViewModel.actualizarDistancia(eventPlan.eventId, distanceName)
                        }
                    },
                    onReviveEvent = {
                        calendarMonitorViewModel.revivirEvento(eventPlan.eventId)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCardWithRoom(
    eventPlan: EventPlan,
    notificationsForEvent: List<ScheduledNotification>,
    onToggleCritical: () -> Unit,
    onApproveAiPlan: () -> Unit,
    onUpdateDistance: (String) -> Unit,
    onReviveEvent: () -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    var showDistanceDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val isCompleted = eventPlan.resolutionStatus == EventResolutionStatus.COMPLETED
    val cardAlpha = if (isCompleted) 0.6f else 1f

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            eventPlan.isCritical -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            eventPlan.hasConflict -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "backgroundColor"
    )

    val openCalendar: () -> Unit = {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventPlan.calendarEventId)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir el calendario", Toast.LENGTH_SHORT).show()
        }
    }

    Card(
        onClick = if (!isCompleted) openCalendar else { {} },
        modifier = Modifier
            .fillMaxWidth()
            .alpha(cardAlpha),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completado",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp).padding(end = 4.dp)
                    )
                }
                if (eventPlan.aiPlanStatus == "PENDING_REVIEW" && !isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Sugerencia de IA",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp).padding(end = 4.dp)
                    )
                }
                if (eventPlan.isCritical && !isCompleted) Text("🚨 ", fontSize = 20.sp)
                Text(
                    text = "${eventPlan.startTime.format(timeFormatter)} - ${eventPlan.title}",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                )
            }

            eventPlan.location?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📍 $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (isCompleted) 0.6f else 1f
                        ),
                        maxLines = 2
                    )
                }
            }

            val nextNotification = notificationsForEvent
                .filter { !it.executed }
                .minByOrNull { it.scheduledTime }

            if (nextNotification != null && !isCompleted) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Notifications, contentDescription = "Próximo aviso", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    val duration = Duration.between(LocalDateTime.now(ZoneId.systemDefault()), nextNotification.scheduledTime)
                    val timeText = when {
                        duration.isNegative || duration.isZero -> "ahora"
                        duration.toMinutes() < 60 -> "en ${duration.toMinutes() + 1} min"
                        else -> "en ${duration.toHours()}h ${duration.toMinutes() % 60}m"
                    }
                    val priorityText = when (nextNotification.priority) {
                        NotificationPriority.CRITICAL -> "CRÍTICO"
                        NotificationPriority.HIGH -> "Alto"
                        else -> "Normal"
                    }
                    Text(
                        text = "Próximo aviso: $timeText (Prioridad: $priorityText)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCompleted) {
                    // Botón para revivir evento
                    TextButton(
                        onClick = onReviveEvent,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reactivar", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reactivar", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    // Controles normales para eventos no completados
                    AssistChip(
                        onClick = { showDistanceDialog = true },
                        label = { Text(eventPlan.distance.displayName, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(32.dp),
                        enabled = !isCompleted
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (eventPlan.aiPlanStatus == "PENDING_REVIEW") {
                        IconButton(
                            onClick = onApproveAiPlan,
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                            enabled = !isCompleted
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Aprobar Sugerencia", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    IconButton(
                        onClick = onToggleCritical,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (eventPlan.isCritical) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
                        ),
                        enabled = !isCompleted
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = if (eventPlan.isCritical) "Quitar crítico" else "Marcar crítico",
                            tint = if (eventPlan.isCritical) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    if (showDistanceDialog && !isCompleted) {
        AlertDialog(
            onDismissRequest = { showDistanceDialog = false },
            title = { Text("¿Qué tan lejos está?") },
            text = {
                Column {
                    EventDistance.values().forEach { distance ->
                        TextButton(
                            onClick = { onUpdateDistance(distance.name); showDistanceDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(distance.displayName)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDistanceDialog = false }) { Text("Cancelar") } }
        )
    }
}