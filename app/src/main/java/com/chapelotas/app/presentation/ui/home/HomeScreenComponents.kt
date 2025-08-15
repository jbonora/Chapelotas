package com.chapelotas.app.presentation.ui.home.components

import android.content.ContentUris
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.chapelotas.app.R
import com.chapelotas.app.domain.models.Sender
import com.chapelotas.app.domain.models.Task
import com.chapelotas.app.presentation.ui.home.util.getRelativeTimeText
import com.chapelotas.app.presentation.ui.home.util.toColor
import com.chapelotas.app.presentation.viewmodels.DisplayableItem
import com.chapelotas.app.presentation.viewmodels.MainViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TopBarAlarmIndicator() {
    Icon(
        imageVector = Icons.Default.Alarm,
        contentDescription = "Alarma programada",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(24.dp)
    )
}


@Composable
fun TaskList(
    navController: NavController,
    items: List<DisplayableItem>,
    viewModel: MainViewModel,
    lazyListState: LazyListState,
    highlightedTaskId: String?,
    onHighlightConsumed: () -> Unit
) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(
                items = items,
                key = { index, item ->
                    // Usar el índice como parte principal de la key garantiza unicidad absoluta
                    when (item) {
                        is DisplayableItem.Event -> "event_${index}_${item.task.id}"
                        is DisplayableItem.Todo -> "todo_${index}_${item.task.id}"
                        is DisplayableItem.TodoHeader -> "header_$index"
                    }
                },
                contentType = { _, item ->
                    when (item) {
                        is DisplayableItem.Event -> "event"
                        is DisplayableItem.Todo -> "todo"
                        is DisplayableItem.TodoHeader -> "header"
                    }
                }
            ) { _, item ->  // Ignoramos el index en el contenido
                when (item) {
                    is DisplayableItem.Event -> EventListItem(
                        task = item.task,
                        viewModel = viewModel,
                        navController = navController,
                        isHighlighted = item.task.id == highlightedTaskId,
                        onHighlightConsumed = onHighlightConsumed
                    )
                    is DisplayableItem.TodoHeader -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tareas Pendientes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = {
                                val calIntent = Intent(Intent.ACTION_INSERT)
                                    .setData(CalendarContract.Events.CONTENT_URI)

                                val today = LocalDate.now()
                                val startTime = today.atTime(8, 0)
                                val timeInMillis = startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                                calIntent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, timeInMillis)
                                calIntent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, timeInMillis)

                                try {
                                    context.startActivity(calIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No se encontró una app de calendario.", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Text("Nuevo")
                            }
                        }
                    }
                    is DisplayableItem.Todo -> TodoListItem(
                        task = item.task,
                        onFinishClick = { viewModel.toggleFinishStatus(item.task) },
                        onDeleteClick = { viewModel.deleteTask(item.task.id) }
                    )
                }
            }
        }
        ScrollIndicators(lazyListState)
    }
}


@Composable
fun EventListItem(
    task: Task,
    viewModel: MainViewModel,
    navController: NavController,
    isHighlighted: Boolean,
    onHighlightConsumed: () -> Unit,
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val context = LocalContext.current
    val highlightColor = MaterialTheme.colorScheme.tertiaryContainer
    val defaultColor = MaterialTheme.colorScheme.surfaceVariant

    val animatedColor by animateColorAsState(
        targetValue = if (isHighlighted) highlightColor else defaultColor,
        animationSpec = tween(400),
        label = "highlight_color"
    )

    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            delay(1600)
            onHighlightConsumed()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate("task_detail/${task.id}") },
        colors = CardDefaults.cardColors(containerColor = animatedColor),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(32.dp)
                        .background(task.status.toColor(), shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = task.title,
                        textDecoration = if (task.isFinished) TextDecoration.LineThrough else null,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (task.isFinished) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = task.scheduledTime.format(timeFormatter),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = getRelativeTimeText(task = task),
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (task.travelTimeMinutes > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TravelTimeIndicator(task = task)
                    }
                }
            }

            if (task.isFromCalendar && task.calendarEventId != null) {
                IconButton(onClick = {
                    task.calendarEventId.let { eventId ->
                        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No se pudo abrir el calendario.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Abrir en Calendario")
                }
            }

            if (!task.isFinished) {
                IconButton(onClick = { viewModel.acknowledgeTask(task.id) }) {
                    Icon(
                        imageVector = if (task.isAcknowledged) Icons.Default.NotificationsActive else Icons.Outlined.NotificationsNone,
                        contentDescription = "Aceptar Tarea",
                        tint = if (task.isAcknowledged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }

            IconButton(onClick = { viewModel.toggleFinishStatus(task) }) {
                Icon(
                    imageVector = if (task.isFinished) Icons.Filled.Replay else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (task.isFinished) "Reabrir Tarea" else "Finalizar Tarea",
                    tint = if (task.isFinished) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
            }
        }

        // Indicador de mensajes sin leer - ahora DENTRO de la Card y clickeable
        if (task.unreadMessageCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("task_detail/${task.id}") }
                    .padding(start = 16.dp, bottom = 8.dp, end = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (task.unreadMessageCount == 1) "1 nuevo mensaje" else "${task.unreadMessageCount} nuevos mensajes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}


@Composable
fun TaskControlPanel(
    task: Task,
    onAcknowledge: () -> Unit,
    onFinishOrReset: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(task.conversationLog.size) {
        if (task.conversationLog.isNotEmpty()) {
            listState.animateScrollToItem(task.conversationLog.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text("Historial de Interacciones", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(
                items = task.conversationLog.toList(),
                key = { index, entry ->
                    // Usar índice + contenido para garantizar unicidad
                    "msg_${index}_${entry.sender}_${entry.message.hashCode()}"
                }
            ) { index, entry ->
                when (entry.sender) {
                    Sender.USUARIO -> UserMessageBubble(message = entry.message)
                    Sender.CHAPELOTAS -> ChapelotasMessageBubble(message = entry.message)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (task.isFinished) {
                Button(onClick = onFinishOrReset, modifier = Modifier.weight(1f).height(50.dp)) {
                    Text("Reabrir Tarea", fontSize = 16.sp)
                }
            } else {
                if (!task.isAcknowledged) {
                    OutlinedButton(onClick = onAcknowledge, modifier = Modifier.weight(1f).height(50.dp)) {
                        Text("Entendido", fontSize = 16.sp)
                    }
                }
                Button(onClick = onFinishOrReset, modifier = Modifier.weight(1f).height(50.dp)) {
                    Text("Finalizar", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun ChapelotasMessageBubble(message: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp))
                // ✅ CORRECCIÓN: Se cambió el color de fondo para que contraste.
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun UserMessageBubble(message: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp))
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}


@Composable
fun TodoListItem(
    task: Task,
    onFinishClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onFinishClick() }
            .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (task.isFinished) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = "Finalizar ToDo",
            tint = if (task.isFinished) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = task.title,
            modifier = Modifier.weight(1f),
            textDecoration = if (task.isFinished) TextDecoration.LineThrough else null,
            style = MaterialTheme.typography.bodyLarge,
            color = if (task.isFinished) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
        )
        if (!task.isFromCalendar) {
            IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Eliminar ToDo",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun PermissionScreen(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(
                Icons.Default.DateRange,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(id = R.string.home_permission_needed_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.home_permission_needed_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRequestPermission) {
                Text(stringResource(id = R.string.home_permission_button))
            }
        }
    }
}

@Composable
fun EmptyScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = stringResource(id = R.string.app_name),
                modifier = Modifier.size(120.dp)
            )
            Text(
                text = stringResource(id = R.string.home_empty_screen_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(id = R.string.home_empty_screen_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BoxScope.ScrollIndicators(lazyListState: LazyListState) {
    val showScrollUpIndicator by remember { derivedStateOf { lazyListState.canScrollBackward } }
    AnimatedVisibility(
        visible = showScrollUpIndicator,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = "Volver arriba",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f), CircleShape).size(32.dp)
        )
    }

    val showScrollDownIndicator by remember { derivedStateOf { lazyListState.canScrollForward } }
    AnimatedVisibility(
        visible = showScrollDownIndicator,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Más tareas",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f), CircleShape).size(32.dp)
        )
    }
}

@Composable
fun TravelTimeIndicator(task: Task) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.TimeToLeave,
            contentDescription = "Tiempo de viaje",
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "${task.travelTimeMinutes} min de viaje (ida)",
            style = MaterialTheme.typography.labelMedium,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}