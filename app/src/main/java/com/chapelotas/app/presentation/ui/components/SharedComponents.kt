package com.chapelotas.app.presentation.ui.components

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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.chapelotas.app.R
import com.chapelotas.app.domain.models.Sender
import com.chapelotas.app.domain.models.Task
import com.chapelotas.app.presentation.ui.home.util.getRelativeTimeText
import com.chapelotas.app.presentation.ui.home.util.toColor
import com.chapelotas.app.presentation.viewmodels.*
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(
    viewModel: AgendaViewModel,
    navController: NavController,
    items: List<DisplayableItem>,
    showMenu: Boolean,
    onShowMenuChange: (Boolean) -> Unit,
    isScrolled: Boolean
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val today = LocalDate.now()
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE d MMM", Locale("es", "ES"))
    val formattedDate = today.format(dateFormatter).replaceFirstChar { it.uppercase() }
    val pendingEventsCount = items.count { it is DisplayableItem.Event && !it.task.isFinished }
    val pendingTodosCount = items.count { it is DisplayableItem.Todo && !it.task.isFinished }

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Image(painter = painterResource(id = R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(34.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = formattedDate, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, fontSize = 16.sp)
                    AnimatedVisibility(visible = pendingEventsCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(top = 2.dp)) {
                            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(when { pendingEventsCount > 5 -> MaterialTheme.colorScheme.error; pendingEventsCount > 2 -> MaterialTheme.colorScheme.tertiary; else -> MaterialTheme.colorScheme.primary }))
                            Icon(imageVector = Icons.Default.Event, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "$pendingEventsCount ${if (pendingEventsCount == 1) "evento hoy" else "eventos hoy"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                    }
                    AnimatedVisibility(visible = pendingTodosCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(top = 1.dp)) {
                            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "$pendingTodosCount ${if (pendingTodosCount == 1) "tarea pendiente" else "tareas pendientes"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                    }
                    if (pendingEventsCount == 0 && pendingTodosCount == 0 && !uiState.isLoading) {
                        Text(text = "Sin pendientes ✨", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        },
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp), verticalAlignment = Alignment.CenterVertically) {
                if (uiState.isLoading) { CircularProgressIndicator(modifier = Modifier.padding(horizontal = 12.dp).size(20.dp), strokeWidth = 2.dp) }
                if (uiState.nextAlarmDuration != null && !uiState.isLoading) {
                    IconButton(onClick = {
                        uiState.nextAlarmDuration?.let { duration ->
                            val alarmTime = uiState.currentTime.plus(duration)
                            val formattedTime = alarmTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                            Toast.makeText(context, "Alarma programada para las $formattedTime", Toast.LENGTH_SHORT).show()
                        }
                    }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Alarm, contentDescription = "Alarma programada", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    }
                }
                val unreadCount = items.sumOf { if (it is DisplayableItem.Event) it.task.unreadMessageCount else 0 }
                if (unreadCount > 0) {
                    BadgedBox(badge = { Badge(containerColor = MaterialTheme.colorScheme.error) { Text(text = if (unreadCount > 9) "9+" else unreadCount.toString(), fontSize = 10.sp) } }, modifier = Modifier.size(40.dp)) {
                        IconButton(onClick = { Toast.makeText(context, "$unreadCount mensajes sin leer", Toast.LENGTH_SHORT).show() }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Outlined.Notifications, contentDescription = "Notificaciones", modifier = Modifier.size(22.dp))
                        }
                    }
                }
                IconButton(onClick = { onShowMenuChange(!showMenu) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(id = R.string.home_menu_more), modifier = Modifier.size(22.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { onShowMenuChange(false) }) {
                    DropdownMenuItem(text = { Text(stringResource(id = R.string.home_menu_settings)) }, onClick = { onShowMenuChange(false); navController.navigate("settings") }, leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) })
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.shadow(elevation = if (isScrolled) 6.dp else 0.dp, clip = false)
    )
}

@Composable
fun EventListItem(
    task: Task,
    viewModel: AgendaViewModel,
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
        Row(modifier = Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(4.dp).height(32.dp).background(task.status.toColor(), shape = CircleShape))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = task.title, textDecoration = if (task.isFinished) TextDecoration.LineThrough else null, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = if (task.isFinished) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = task.scheduledTime.format(timeFormatter), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = getRelativeTimeText(task = task), style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        try { context.startActivity(intent) } catch (e: Exception) { Toast.makeText(context, "No se pudo abrir el calendario.", Toast.LENGTH_SHORT).show() }
                    }
                }) { Icon(Icons.Default.DateRange, contentDescription = "Abrir en Calendario") }
            }
            if (!task.isFinished) {
                IconButton(onClick = { viewModel.acknowledgeTask(task.id) }) {
                    Icon(imageVector = if (task.isAcknowledged) Icons.Default.NotificationsActive else Icons.Outlined.NotificationsNone, contentDescription = "Aceptar Tarea", tint = if (task.isAcknowledged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                }
            }
            IconButton(onClick = { viewModel.toggleFinishStatus(task) }) {
                Icon(imageVector = if (task.isFinished) Icons.Filled.Replay else Icons.Outlined.RadioButtonUnchecked, contentDescription = if (task.isFinished) "Reabrir Tarea" else "Finalizar Tarea", tint = if (task.isFinished) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
            }
        }
        if (task.unreadMessageCount > 0) {
            Box(modifier = Modifier.fillMaxWidth().clickable { navController.navigate("task_detail/${task.id}") }.padding(start = 16.dp, bottom = 8.dp, end = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (task.unreadMessageCount == 1) "1 nuevo mensaje" else "${task.unreadMessageCount} nuevos mensajes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun TodoListItem(task: Task, onFinishClick: (Task) -> Unit, onDeleteClick: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable { onFinishClick(task) }.padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = if (task.isFinished) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked, contentDescription = "Finalizar ToDo", tint = if (task.isFinished) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = task.title, modifier = Modifier.weight(1f), textDecoration = if (task.isFinished) TextDecoration.LineThrough else null, style = MaterialTheme.typography.bodyLarge, color = if (task.isFinished) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface)
        if (!task.isFromCalendar) {
            IconButton(onClick = { onDeleteClick(task.id) }, modifier = Modifier.size(28.dp)) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Eliminar ToDo", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun TodoHeaderCard() {
    val context = LocalContext.current
    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Tareas Pendientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        TextButton(onClick = {
            val calIntent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
            val today = LocalDate.now(); val startTime = today.atTime(8, 0); val timeInMillis = startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            calIntent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, timeInMillis).putExtra(CalendarContract.EXTRA_EVENT_END_TIME, timeInMillis)
            try { context.startActivity(calIntent) } catch (e: Exception) { Toast.makeText(context, "No se encontró una app de calendario.", Toast.LENGTH_SHORT).show() }
        }) { Text("Nuevo") }
    }
}

@Composable
fun TaskInteractionPanel(
    task: Task,
    onAcknowledge: () -> Unit,
    onFinishOrReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(task.conversationLog.size) {
        if (task.conversationLog.isNotEmpty()) {
            listState.animateScrollToItem(task.conversationLog.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // --- INICIO DE LA CORRECCIÓN DEL CRASH ---
            itemsIndexed(
                items = task.conversationLog.toList(),
                key = { index, entry -> "msg_${entry.sender}_${entry.message.hashCode()}_$index" } // Clave única
            ) { index, entry ->
                when (entry.sender) {
                    Sender.USUARIO -> UserMessageBubble(message = entry.message)
                    Sender.CHAPELOTAS -> ChapelotasMessageBubble(message = entry.message)
                }
            }
            // --- FIN DE LA CORRECCIÓN DEL CRASH ---
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (task.isFinished) {
                    Button(
                        onClick = onFinishOrReset,
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = "Reabrir")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reabrir Tarea", fontSize = 16.sp)
                    }
                } else {
                    if (!task.isAcknowledged) {
                        OutlinedButton(
                            onClick = onAcknowledge,
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Text("Entendido", fontSize = 16.sp)
                        }
                    }
                    Button(
                        onClick = onFinishOrReset,
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Icon(Icons.Default.Done, contentDescription = "Finalizar")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Finalizar", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ChapelotasMessageBubble(message: String) {
    // --- INICIO DE LA CORRECCIÓN DE DISEÑO DE BURBUJAS ---
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            modifier = Modifier.widthIn(max = 300.dp), // Ancho máximo para la burbuja
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
    // --- FIN DE LA CORRECCIÓN DE DISEÑO DE BURBUJAS ---
}

@Composable
fun UserMessageBubble(message: String) {
    // --- INICIO DE LA CORRECCIÓN DE DISEÑO DE BURBUJAS ---
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            modifier = Modifier.widthIn(max = 300.dp), // Ancho máximo para la burbuja
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp),
            color = MaterialTheme.colorScheme.primary,
            tonalElevation = 2.dp
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
    // --- FIN DE LA CORRECCIÓN DE DISEÑO DE BURBUJAS ---
}


@Composable
fun TitledBorderBox(title: String, isSpecialDay: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val backgroundColor = if (isSpecialDay) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    Box(modifier = modifier.padding(top = 8.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).background(backgroundColor).padding(16.dp)) { content() }
        Text(text = " $title ", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if (isSpecialDay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 12.dp).background(MaterialTheme.colorScheme.surface).align(Alignment.TopStart))
    }
}

@Composable
fun FreeTimeSectionForDay(schedule: DailySchedule, day: LocalDate) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Tus Huecos Libres", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        TimeBlockSection(title = "Mañana", block = schedule.morning, day = day)
        TimeBlockSection(title = "Almuerzo", block = schedule.lunch, day = day)
        TimeBlockSection(title = "Tarde", block = schedule.afternoon, day = day)
        TimeBlockSection(title = "Cena", block = schedule.dinner, day = day)
        TimeBlockSection(title = "Noche", block = schedule.night, day = day)
    }
}

@Composable
fun TimeBlockSection(title: String, block: TimeBlock, day: LocalDate) {
    Row(verticalAlignment = Alignment.Top) {
        Text(text = title, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(80.dp))
        Column {
            when (block.status) {
                BlockStatus.HAS_SLOTS, BlockStatus.FULLY_BOOKED -> {
                    if (block.items.isEmpty()) Text(text = if (block.status == BlockStatus.FULLY_BOOKED) "Ocupado." else "Sin huecos.", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = Color.Gray)
                    else block.items.forEach { item -> key(day.toString() + item.hashCode()) { when (item) { is ScheduleItem.Free -> FreeSlotView(slot = item.slot, day = day); is ScheduleItem.Busy -> BusySlotView(busy = item) } } }
                }
                BlockStatus.OUTSIDE_WORK_HOURS -> Text(text = "Fuera de horario.", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = Color.Gray)
                BlockStatus.IN_THE_PAST -> Text(text = "Pasado.", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = Color.Gray)
            }
        }
    }
}

@Composable
fun FreeSlotView(slot: TimeSlot, day: LocalDate) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm"); val context = LocalContext.current
    Row(modifier = Modifier.clickable {
        val beginTime = day.atTime(slot.start).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(); val endTime = day.atTime(slot.end).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI).putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime).putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
        try { context.startActivity(intent) } catch (e: Exception) { Toast.makeText(context, "No se encontró una app de calendario.", Toast.LENGTH_SHORT).show() }
    }, verticalAlignment = Alignment.CenterVertically) {
        val color = if (slot.durationMinutes < 60) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
        Text(text = "${slot.start.format(timeFormatter)} - ${slot.end.format(timeFormatter)}", style = MaterialTheme.typography.bodyMedium, color = color)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "(${slot.description})", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = color)
    }
}

@Composable
fun BusySlotView(busy: ScheduleItem.Busy) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm"); val context = LocalContext.current
    val modifier = if (busy.isFromCalendar && busy.calendarEventId != null) {
        Modifier.clickable {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, busy.calendarEventId); val intent = Intent(Intent.ACTION_VIEW, uri)
            try { context.startActivity(intent) } catch (e: Exception) { Toast.makeText(context, "No se pudo abrir el evento.", Toast.LENGTH_SHORT).show() }
        }
    } else Modifier
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text = "${busy.start.format(timeFormatter)} - ${busy.end.format(timeFormatter)}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = busy.description, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun PermissionScreen(onRequestPermission: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(horizontal = 32.dp)) {
            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
            Text(text = stringResource(id = R.string.home_permission_needed_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(text = stringResource(id = R.string.home_permission_needed_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Button(onClick = onRequestPermission) { Text(stringResource(id = R.string.home_permission_button)) }
        }
    }
}

@Composable
fun EmptyScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Image(painter = painterResource(id = R.drawable.ic_launcher_foreground), contentDescription = stringResource(id = R.string.app_name), modifier = Modifier.size(120.dp))
            Text(text = stringResource(id = R.string.home_empty_screen_title), style = MaterialTheme.typography.headlineSmall)
            Text(text = stringResource(id = R.string.home_empty_screen_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TravelTimeIndicator(task: Task) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.TimeToLeave, contentDescription = "Tiempo de viaje", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "${task.travelTimeMinutes} min de viaje (ida)", style = MaterialTheme.typography.labelMedium, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
fun BoxScope.ScrollIndicators(lazyListState: LazyListState) {
    val showScrollUpIndicator by remember { derivedStateOf { lazyListState.canScrollBackward } }
    AnimatedVisibility(visible = showScrollUpIndicator, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)) {
        Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Volver arriba", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f), CircleShape).size(32.dp))
    }
    val showScrollDownIndicator by remember { derivedStateOf { lazyListState.canScrollForward } }
    AnimatedVisibility(visible = showScrollDownIndicator, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)) {
        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Más tareas", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f), CircleShape).size(32.dp))
    }
}