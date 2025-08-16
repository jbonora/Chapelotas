package com.chapelotas.app.presentation.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.chapelotas.app.battery.BatteryProtectionManager
import com.chapelotas.app.domain.models.Task
import com.chapelotas.app.presentation.ui.components.* // <-- ¡IMPORTANTE!
import com.chapelotas.app.presentation.viewmodels.*
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    navController: NavController,
    highlightedTaskId: String?,
    onHighlightConsumed: () -> Unit,
    onScrollChanged: (Boolean) -> Unit,
    viewModel: AgendaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showNewDayDialog by viewModel.showNewDayDialog.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> if (isGranted) viewModel.onPermissionGranted() }
    )

    if (showNewDayDialog) {
        AlertDialog(onDismissRequest = { viewModel.onNewDayRefreshDismissed() },
            icon = { Icon(imageVector = Icons.Default.Refresh, contentDescription = "Nuevo día") },
            title = { Text(text = "¡Ha comenzado un nuevo día!") },
            text = { Text(text = "¿Querés actualizar las tareas para ver las de hoy?") },
            confirmButton = { TextButton(onClick = { viewModel.onNewDayRefreshAccepted() }) { Text("Actualizar") } },
            dismissButton = { TextButton(onClick = { viewModel.onNewDayRefreshDismissed() }) { Text("Ahora no") } }
        )
    }

    if (uiState.showBatteryProtectionDialog) {
        LaunchedEffect(Unit) {
            BatteryProtectionManager.showBatteryProtectionDialog(context) {
                viewModel.onBatteryProtectionDialogDismissed()
            }
        }
    }

    // --- INICIO DE LA CORRECCIÓN ---
    // Lógica completamente nueva para encontrar y desplazarse a la tarjeta del día correcto.
    LaunchedEffect(highlightedTaskId, uiState.daysInfo) {
        if (highlightedTaskId.isNullOrBlank() || uiState.daysInfo.isEmpty()) {
            return@LaunchedEffect
        }

        // 1. Encontrar la fecha del día que contiene la tarea resaltada.
        val targetDayDate = uiState.daysInfo.find { dayInfo ->
            dayInfo.timedTasks.any { it.id == highlightedTaskId } ||
                    dayInfo.allDayTasks.any { it.id == highlightedTaskId } ||
                    (dayInfo.date == LocalDate.now() && uiState.todayItems.any { it.id == highlightedTaskId })
        }?.date

        if (targetDayDate != null) {
            val today = LocalDate.now()
            // 2. Calcular el índice de la tarjeta de ese día en la LazyColumn.
            val targetIndex = if (targetDayDate.isEqual(today)) {
                // Si es hoy, la tarjeta siempre es la primera (índice 0).
                0
            } else {
                // Si es un día futuro, encontrar su posición en la lista de días futuros.
                val futureDays = uiState.daysInfo.filter { it.date.isAfter(today) }
                val futureDayIndex = futureDays.indexOfFirst { it.date == targetDayDate }
                // El índice final es la posición en la lista futura + 1 (para contar la tarjeta de "Hoy").
                if (futureDayIndex != -1) futureDayIndex + 1 else -1
            }

            // 3. Si encontramos un índice válido, nos desplazamos a él.
            if (targetIndex != -1) {
                try {
                    lazyListState.animateScrollToItem(targetIndex)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    // --- FIN DE LA CORRECCIÓN ---

    val isScrolled by remember { derivedStateOf { lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0 } }
    val canScrollDown by remember { derivedStateOf { lazyListState.canScrollForward } }
    LaunchedEffect(canScrollDown) { onScrollChanged(canScrollDown) }
    LaunchedEffect(uiState.daysInfo.size) {
        if (uiState.daysInfo.isNotEmpty()) { delay(100); onScrollChanged(lazyListState.canScrollForward) }
    }

    Scaffold(
        topBar = {
            HomeTopAppBar(viewModel = viewModel, navController = navController, items = uiState.todayItems, showMenu = showMenu, onShowMenuChange = { showMenu = it }, isScrolled = isScrolled)
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                uiState.needsPermission -> PermissionScreen { calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR) }
                uiState.daysInfo.isEmpty() && uiState.todayItems.isEmpty() -> EmptyScreen()
                else -> {
                    LazyColumn(state = lazyListState, contentPadding = PaddingValues(all = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        val todayInfo = uiState.daysInfo.firstOrNull { it.date == LocalDate.now() }
                        if (todayInfo != null) {
                            item(key = "today_section") {
                                TitledBorderBox(title = getDayTitle(todayInfo.date, todayInfo.allDayTasks), isSpecialDay = todayInfo.allDayTasks.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (uiState.todayItems.isNotEmpty()) {
                                            uiState.todayItems.forEach { item ->
                                                when (item) {
                                                    is DisplayableItem.Event -> EventListItem(task = item.task, viewModel = viewModel, navController = navController, isHighlighted = item.task.id == highlightedTaskId, onHighlightConsumed = onHighlightConsumed)
                                                    is DisplayableItem.TodoHeader -> TodoHeaderCard()
                                                    is DisplayableItem.Todo -> TodoListItem(task = item.task, onFinishClick = viewModel::toggleFinishStatus, onDeleteClick = viewModel::deleteTask)
                                                }
                                            }
                                        } else {
                                            Text("No hay tareas programadas para hoy.", fontStyle = FontStyle.Italic, modifier = Modifier.padding(vertical = 8.dp))
                                        }
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                        FreeTimeSectionForDay(schedule = todayInfo.schedule, day = todayInfo.date)
                                    }
                                }
                            }
                        }

                        val futureDays = uiState.daysInfo.filter { it.date.isAfter(LocalDate.now()) }
                        items(futureDays, key = { it.date.toString() }) { dayInfo ->
                            TitledBorderBox(title = getDayTitle(dayInfo.date, dayInfo.allDayTasks), isSpecialDay = dayInfo.allDayTasks.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (dayInfo.timedTasks.isNotEmpty()) {
                                        Text("Eventos Programados", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        dayInfo.timedTasks.forEach { task ->
                                            EventListItem(task = task, viewModel = viewModel, navController = navController, isHighlighted = task.id == highlightedTaskId, onHighlightConsumed = onHighlightConsumed)
                                        }
                                    } else if (dayInfo.allDayTasks.isEmpty()) {
                                        Text("Día sin eventos programados.", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                    FreeTimeSectionForDay(schedule = dayInfo.schedule, day = dayInfo.date)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getDayTitle(day: LocalDate, allDayTasks: List<Task>): String {
    val today = LocalDate.now(); val dayFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "ES")); val dayName = day.format(dayFormatter).replaceFirstChar { it.titlecase() }; val prefix = when (ChronoUnit.DAYS.between(today, day)) { 0L -> "Hoy"; 1L -> "Mañana"; else -> null }; val allDayTitle = allDayTasks.joinToString(", ") { it.title }; return buildString { if (prefix != null) { append(prefix); append(": ") }; append(dayName); if (allDayTitle.isNotBlank()) { append(" - "); append(allDayTitle) } }
}