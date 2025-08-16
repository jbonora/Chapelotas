package com.chapelotas.app.presentation.ui.home

import android.content.ContentUris
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.chapelotas.app.domain.models.Task
import com.chapelotas.app.presentation.ui.home.components.EventListItem
import com.chapelotas.app.presentation.viewmodels.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

// --- NUEVO COMPONENTE VISUAL PARA EL CONTENEDOR DEL DÍA ---
@Composable
fun TitledBorderBox(
    title: String,
    isSpecialDay: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val backgroundColor = if (isSpecialDay) {
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    Box(modifier = modifier.padding(top = 8.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp) // Espacio para que el título se asiente
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .padding(16.dp)
        ) {
            // El contenido real se muestra aquí
            content()
        }

        // El título que "corta" el borde
        Text(
            text = " $title ", // Espacios para el padding visual
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (isSpecialDay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(start = 12.dp)
                .background(MaterialTheme.colorScheme.surface) // Fondo del color de la pantalla
                .align(Alignment.TopStart)
        )
    }
}

// --- FUNCIÓN AUXILIAR MODIFICADA ---
private fun getDayTitle(day: LocalDate, allDayTasks: List<Task>): String {
    val today = LocalDate.now()
    val dayFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "ES"))
    val dayName = day.format(dayFormatter).replaceFirstChar { it.titlecase() }

    val prefix = when (ChronoUnit.DAYS.between(today, day)) {
        0L -> "Hoy"
        1L -> "Mañana"
        else -> null
    }

    val allDayTitle = allDayTasks.joinToString(", ") { it.title }

    return buildString {
        if (prefix != null) {
            append(prefix)
            append(": ")
        }
        append(dayName)
        if (allDayTitle.isNotBlank()) {
            append(" - ")
            append(allDayTitle)
        }
    }
}

// --- PANTALLA PRINCIPAL REFACTORIZADA ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SevenDaysScreen(
    navController: NavController,
    mainViewModel: MainViewModel = hiltViewModel(),
    sevenDaysViewModel: SevenDaysViewModel = hiltViewModel()
) {
    val uiState by sevenDaysViewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(uiState.daysInfo, key = { it.date.toString() }) { dayInfo ->
            val isSpecialDay = dayInfo.allDayTasks.isNotEmpty()
            val title = getDayTitle(dayInfo.date, dayInfo.allDayTasks)

            TitledBorderBox(title = title, isSpecialDay = isSpecialDay) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // --- INICIO DE LA MODIFICACIÓN: REORDENAMIENTO ---

                    // 1. Sección de Tareas con Hora (AHORA PRIMERO)
                    if (dayInfo.timedTasks.isNotEmpty()) {
                        Text(
                            text = "Eventos Programados",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        dayInfo.timedTasks.forEach { task ->
                            EventListItem(
                                task = task,
                                viewModel = mainViewModel,
                                navController = navController,
                                isHighlighted = false,
                                onHighlightConsumed = {}
                            )
                        }
                    } else if (dayInfo.allDayTasks.isEmpty()) {
                        // Mensaje solo si no hay eventos de ningún tipo
                        Text(
                            text = "Día sin eventos programados.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                    }

                    // Divisor entre secciones
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // 2. Sección de Huecos Libres (AHORA SEGUNDO)
                    FreeTimeSectionForDay(schedule = dayInfo.schedule, day = dayInfo.date)

                    // --- FIN DE LA MODIFICACIÓN ---
                }
            }
        }
    }
}

@Composable
fun FreeTimeSectionForDay(schedule: DailySchedule, day: LocalDate) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Tus Huecos Libres",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        TimeBlockSection(title = "Mañana", block = schedule.morning, day = day)
        TimeBlockSection(title = "Almuerzo", block = schedule.lunch, day = day)
        TimeBlockSection(title = "Tarde", block = schedule.afternoon, day = day)
        TimeBlockSection(title = "Cena", block = schedule.dinner, day = day)
        TimeBlockSection(title = "Noche", block = schedule.night, day = day)
    }
}

@Composable
private fun TimeBlockSection(title: String, block: TimeBlock, day: LocalDate) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = title,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(80.dp)
        )
        Column {
            when (block.status) {
                BlockStatus.HAS_SLOTS, BlockStatus.FULLY_BOOKED -> {
                    if (block.items.isEmpty()) {
                        Text(
                            text = if (block.status == BlockStatus.FULLY_BOOKED) "Ocupado." else "Sin huecos.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = Color.Gray
                        )
                    } else {
                        block.items.forEach { item ->
                            key(day.toString() + item.hashCode()) {
                                when (item) {
                                    is ScheduleItem.Free -> FreeSlotView(slot = item.slot, day = day)
                                    is ScheduleItem.Busy -> BusySlotView(busy = item)
                                }
                            }
                        }
                    }
                }
                BlockStatus.OUTSIDE_WORK_HOURS -> {
                    Text(
                        text = "Fuera de horario.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = Color.Gray
                    )
                }
                BlockStatus.IN_THE_PAST -> {
                    Text(
                        text = "Pasado.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun FreeSlotView(slot: TimeSlot, day: LocalDate) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val context = LocalContext.current

    Row(
        modifier = Modifier.clickable {
            val beginTime = day.atTime(slot.start).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endTime = day.atTime(slot.end).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val intent = Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "No se encontró una app de calendario.", Toast.LENGTH_SHORT).show()
            }
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        val color = if (slot.durationMinutes < 60) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.onSurface
        }

        Text(
            text = "${slot.start.format(timeFormatter)} - ${slot.end.format(timeFormatter)}",
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "(${slot.description})",
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
            color = color
        )
    }
}

@Composable
private fun BusySlotView(busy: ScheduleItem.Busy) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val context = LocalContext.current
    val modifier = if (busy.isFromCalendar && busy.calendarEventId != null) {
        Modifier.clickable {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, busy.calendarEventId)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "No se pudo abrir el evento en el calendario.", Toast.LENGTH_SHORT).show()
            }
        }
    } else {
        Modifier
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${busy.start.format(timeFormatter)} - ${busy.end.format(timeFormatter)}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = busy.description,
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
            color = Color.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}