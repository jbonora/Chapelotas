package com.chapelotas.app.presentation.ui.home

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.chapelotas.app.R
import com.chapelotas.app.battery.BatteryProtectionManager
import com.chapelotas.app.presentation.ui.home.components.*
import com.chapelotas.app.presentation.viewmodels.DisplayableItem
import com.chapelotas.app.presentation.viewmodels.MainViewModel
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    highlightedTaskId: String?,
    onHighlightConsumed: () -> Unit,
    onScrollChanged: (Boolean) -> Unit = {},
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val items by viewModel.todayItems.collectAsStateWithLifecycle()
    val showNewDayDialog by viewModel.showNewDayDialog.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) viewModel.onPermissionGranted()
        }
    )

    // Formatear la fecha actual - versión corta
    val today = LocalDate.now()
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE d MMM", Locale("es", "ES"))
    val formattedDate = today.format(dateFormatter).replaceFirstChar { it.uppercase() }

    // Contar eventos y tareas pendientes por separado
    val pendingEventsCount = items.count {
        it is DisplayableItem.Event && !it.task.isFinished
    }
    val pendingTodosCount = items.count {
        it is DisplayableItem.Todo && !it.task.isFinished
    }

    // NUEVO: Diálogo de nuevo día
    if (showNewDayDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onNewDayRefreshDismissed() },
            icon = {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Nuevo día",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "¡Ha comenzado un nuevo día!",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = "¿Querés actualizar las tareas para ver las de hoy?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onNewDayRefreshAccepted() }
                ) {
                    Text("Actualizar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onNewDayRefreshDismissed() }
                ) {
                    Text("Ahora no")
                }
            }
        )
    }

    if (uiState.showBatteryProtectionDialog) {
        LaunchedEffect(Unit) {
            BatteryProtectionManager.showBatteryProtectionDialog(context) {
                viewModel.onBatteryProtectionDialogDismissed()
            }
        }
    }

    LaunchedEffect(highlightedTaskId, items) {
        if (!highlightedTaskId.isNullOrBlank() && items.isNotEmpty()) {
            val index = items.indexOfFirst {
                it is DisplayableItem.Event && it.task.id == highlightedTaskId
            }
            if (index >= 0) {  // Cambié != -1 por >= 0 para ser más explícito
                try {
                    lazyListState.animateScrollToItem(index)
                } catch (e: Exception) {
                    // Si hay algún problema con el scroll, lo ignoramos
                    e.printStackTrace()
                }
            }
        }
    }

    // Detectar si el usuario hizo scroll para la sombra del TopBar
    val isScrolled by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0
        }
    }

    // EXACTAMENTE la misma lógica que usa ScrollIndicators para la flechita HACIA ABAJO
    val canScrollDown by remember {
        derivedStateOf {
            lazyListState.canScrollForward
        }
    }

    // Comunicar el estado al BottomBar
    LaunchedEffect(canScrollDown) {
        onScrollChanged(canScrollDown)
    }

    // También actualizar cuando se cargan los items por primera vez
    LaunchedEffect(items.size) {
        if (items.isNotEmpty()) {
            kotlinx.coroutines.delay(100) // Pequeño delay para que el layout se calcule
            onScrollChanged(lazyListState.canScrollForward)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        // Logo de la app sin contenedor extra (ya es redondeado)
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.size(34.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // Información del día más completa
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                fontSize = 16.sp
                            )

                            // Mostrar eventos pendientes
                            AnimatedVisibility(visible = pendingEventsCount > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    pendingEventsCount > 5 -> MaterialTheme.colorScheme.error
                                                    pendingEventsCount > 2 -> MaterialTheme.colorScheme.tertiary
                                                    else -> MaterialTheme.colorScheme.primary
                                                }
                                            )
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$pendingEventsCount ${if (pendingEventsCount == 1) "evento hoy" else "eventos hoy"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Mostrar ToDos pendientes
                            AnimatedVisibility(visible = pendingTodosCount > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(top = 1.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondary)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$pendingTodosCount ${if (pendingTodosCount == 1) "tarea pendiente" else "tareas pendientes"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Si no hay nada pendiente
                            if (pendingEventsCount == 0 && pendingTodosCount == 0 && !uiState.isLoading) {
                                Text(
                                    text = "Sin pendientes ✨",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                },
                actions = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Indicador de estado (loading)
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }

                        // Indicador de alarma (si hay una programada)
                        if (uiState.nextAlarmDuration != null && !uiState.isLoading) {
                            IconButton(
                                onClick = {
                                    Toast.makeText(context, "Próxima alarma programada", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.Alarm,
                                    contentDescription = "Alarma programada",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Botón de notificaciones (si hay mensajes sin leer)
                        val unreadCount = items.sumOf {
                            if (it is DisplayableItem.Event) it.task.unreadMessageCount else 0
                        }
                        if (unreadCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ) {
                                        Text(
                                            text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                            fontSize = 10.sp
                                        )
                                    }
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        Toast.makeText(context, "$unreadCount mensajes sin leer", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Notifications,
                                        contentDescription = "Notificaciones",
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        // Menú de opciones
                        IconButton(
                            onClick = { showMenu = !showMenu },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(id = R.string.home_menu_more),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.home_menu_settings)) },
                                onClick = {
                                    showMenu = false
                                    navController.navigate("settings")
                                },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.shadow(
                    elevation = if (isScrolled) 6.dp else 0.dp,
                    clip = false
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
        ) {
            when {
                uiState.needsPermission -> PermissionScreen {
                    calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                }
                items.isEmpty() && !uiState.isLoading -> EmptyScreen()
                else -> TaskList(
                    navController = navController,
                    items = items,
                    viewModel = viewModel,
                    lazyListState = lazyListState,
                    highlightedTaskId = highlightedTaskId,
                    onHighlightConsumed = onHighlightConsumed
                )
            }
        }
    }
}