package com.chapelotas.app.presentation.ui.home

import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.chapelotas.app.presentation.ui.home.components.EventListItem
import com.chapelotas.app.presentation.viewmodels.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
private fun getDayDisplayName(day: LocalDate): String {
    val today = LocalDate.now()
    val dayFormatter = DateTimeFormatter.ofPattern("EEEE d", Locale("es", "ES"))
    val dayName = day.format(dayFormatter).replaceFirstChar { it.titlecase() }

    return when (ChronoUnit.DAYS.between(today, day)) {
        0L -> "Hoy ($dayName)"
        1L -> "Mañana ($dayName)"
        else -> dayName
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SevenDaysScreen(
    navController: NavController,
    mainViewModel: MainViewModel = hiltViewModel(), // <-- AÑADIDO: Obtenemos el MainViewModel
    sevenDaysViewModel: SevenDaysViewModel = hiltViewModel()
) {
    val sevenDaysUiState by sevenDaysViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FreeTimeBlocksCard(
                scheduleByDay = sevenDaysUiState.scheduleByDay,
                daysToShow = sevenDaysUiState.weekDays
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        sevenDaysUiState.weekDays.forEach { day ->
            val tasksForDay = sevenDaysUiState.tasksByDay[day]?.sortedBy { it.scheduledTime } ?: emptyList()

            stickyHeader {
                val dayText = getDayDisplayName(day)
                Text(
                    text = dayText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (tasksForDay.isNotEmpty()) {
                items(tasksForDay, key = { it.id }) { task ->
                    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                        if (!task.isTodo) {
                            EventListItem(
                                task = task,
                                viewModel = mainViewModel, // <-- CORREGIDO: Pasamos el viewModel
                                navController = navController,
                                isHighlighted = false,
                                onHighlightConsumed = {}
                            )
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "Día sin eventos programados.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FreeTimeBlocksCard(
    scheduleByDay: Map<LocalDate, DailySchedule>,
    daysToShow: List<LocalDate>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Tus Huecos Libres (Próximos 7 Días)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            daysToShow.forEach { day ->
                val schedule = scheduleByDay[day]

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = getDayDisplayName(day),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (schedule != null) {
                        TimeBlockSection(title = "Por la mañana", block = schedule.morning, day = day)
                        TimeBlockSection(title = "Almuerzo", block = schedule.lunch, day = day)
                        TimeBlockSection(title = "Tarde", block = schedule.afternoon, day = day)
                        TimeBlockSection(title = "Cena", block = schedule.dinner, day = day)
                        TimeBlockSection(title = "Noche", block = schedule.night, day = day)
                    }

                    if (day != daysToShow.last()) {
                        HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeBlockSection(title: String, block: TimeBlock, day: LocalDate) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val context = LocalContext.current

    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(100.dp)
        )
        Column {
            when (block.status) {
                BlockStatus.HAS_SLOTS -> {
                    if (block.slots.isEmpty()) {
                        Text(
                            text = "Sin huecos de 30 min o más.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = Color.Gray
                        )
                    } else {
                        block.slots.forEach { slot ->
                            key(day.toString() + slot.start.toString()) {
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
                        }
                    }
                }
                BlockStatus.FULLY_BOOKED -> {
                    Text(
                        text = "Ocupado.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = Color.Gray
                    )
                }
                BlockStatus.OUTSIDE_WORK_HOURS -> {
                    Text(
                        text = "Fuera de tu horario laboral.",
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