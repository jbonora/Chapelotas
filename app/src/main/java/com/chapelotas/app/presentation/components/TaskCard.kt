package com.chapelotas.app.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chapelotas.app.domain.models.Task
import com.chapelotas.app.domain.models.TaskStatus
import com.chapelotas.app.domain.time.TimeRemaining
import com.chapelotas.app.presentation.viewmodels.DisplayableItem
import java.time.format.DateTimeFormatter

@Composable
fun TaskCard(
    item: DisplayableItem,
    onTaskClick: (Task) -> Unit,
    onAcknowledge: (String) -> Unit,
    onToggleFinish: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    when (item) {
        is DisplayableItem.Event -> EventCard(
            event = item,
            onTaskClick = onTaskClick,
            onAcknowledge = onAcknowledge,
            onToggleFinish = onToggleFinish,
            modifier = modifier
        )
        is DisplayableItem.Todo -> TodoCard(
            todo = item,
            onTaskClick = onTaskClick,
            onToggleFinish = onToggleFinish,
            modifier = modifier
        )
        is DisplayableItem.TodoHeader -> TodoHeaderCard(modifier = modifier)
    }
}

@Composable
private fun EventCard(
    event: DisplayableItem.Event,
    onTaskClick: (Task) -> Unit,
    onAcknowledge: (String) -> Unit,
    onToggleFinish: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    val task = event.task
    val timeRemaining = event.timeRemaining

    // Animación de color basada en urgencia
    val backgroundColor by animateColorAsState(
        targetValue = when {
            task.isFinished -> Color(0xFFE8F5E9)
            task.status == TaskStatus.DELAYED -> Color(0xFFFFEBEE)
            task.status == TaskStatus.ONGOING && !task.isAcknowledged -> Color(0xFFFFF3E0)
            task.status == TaskStatus.ONGOING -> Color(0xFFFFFDE7)
            timeRemaining != null && timeRemaining.totalMinutes < 30 -> Color(0xFFFFF8E1)
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(300),
        label = "background_color"
    )

    // Animación de pulso para tareas urgentes no reconocidas
    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseAnimation.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val shouldPulse = task.status == TaskStatus.ONGOING && !task.isAcknowledged

    Card(
        onClick = { onTaskClick(task) },
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (shouldPulse) pulseAlpha else 1f),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Título de la tarea
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            textDecoration = if (task.isFinished) TextDecoration.LineThrough else null
                        ),
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Tiempo con actualización en tiempo real
                    TimeDisplay(
                        formattedTime = event.formattedTime,
                        timeRemaining = timeRemaining,
                        status = task.status,
                        isFinished = task.isFinished
                    )

                    // Indicadores de estado
                    if (task.unreadMessageCount > 0 && !task.isFinished) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White
                        ) {
                            Text(
                                text = "${task.unreadMessageCount} mensaje${if (task.unreadMessageCount > 1) "s" else ""} sin leer",
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Estado visual
                StatusIndicator(
                    status = task.status,
                    isFinished = task.isFinished,
                    isAcknowledged = task.isAcknowledged
                )
            }

            // Botones de acción
            if (!task.isFinished && (task.status == TaskStatus.ONGOING || task.status == TaskStatus.DELAYED)) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!task.isAcknowledged) {
                        OutlinedButton(
                            onClick = { onAcknowledge(task.id) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Entendido")
                        }
                    }
                    Button(
                        onClick = { onToggleFinish(task) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Finalizar")
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeDisplay(
    formattedTime: String,
    timeRemaining: TimeRemaining?,
    status: TaskStatus,
    isFinished: Boolean
) {
    val timeColor = when {
        isFinished -> Color.Gray
        status == TaskStatus.DELAYED -> MaterialTheme.colorScheme.error
        status == TaskStatus.ONGOING -> Color(0xFFFF6F00)
        timeRemaining != null && timeRemaining.totalMinutes < 30 -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = formattedTime,
        style = MaterialTheme.typography.bodyMedium,
        color = timeColor,
        fontWeight = if (status == TaskStatus.ONGOING || status == TaskStatus.DELAYED)
            FontWeight.Bold else FontWeight.Normal
    )
}

@Composable
private fun StatusIndicator(
    status: TaskStatus,
    isFinished: Boolean,
    isAcknowledged: Boolean
) {
    val (icon, color) = when {
        isFinished -> "✓" to Color(0xFF4CAF50)
        status == TaskStatus.DELAYED -> "!" to Color(0xFFFF5252)
        status == TaskStatus.ONGOING && !isAcknowledged -> "⚠" to Color(0xFFFF9800)
        status == TaskStatus.ONGOING -> "▶" to Color(0xFF2196F3)
        else -> "" to Color.Transparent
    }

    if (icon.isNotEmpty()) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                fontSize = 18.sp,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TodoCard(
    todo: DisplayableItem.Todo,
    onTaskClick: (Task) -> Unit,
    onToggleFinish: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    val task = todo.task

    Card(
        onClick = { onTaskClick(task) },
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isFinished) Color(0xFFE8F5E9)
            else if (task.isStarted) Color(0xFFE3F2FD)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = task.isFinished,
                    onCheckedChange = { onToggleFinish(task) }
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textDecoration = if (task.isFinished) TextDecoration.LineThrough else null
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = todo.formattedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (task.unreadMessageCount > 0 && !task.isFinished) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = Color.White
                ) {
                    Text(text = task.unreadMessageCount.toString())
                }
            }
        }
    }
}

@Composable
private fun TodoHeaderCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    ) {
        Text(
            text = "Tareas Pendientes",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}