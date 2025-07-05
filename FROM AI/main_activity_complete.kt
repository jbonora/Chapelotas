package com.chapelotas.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chapelotas.app.domain.entities.CalendarEvent
import com.chapelotas.app.presentation.viewmodels.MainViewModel
import com.chapelotas.app.presentation.ui.theme.ChapelotasTheme
import dagger.hilt.android.AndroidEntryPoint
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val viewModel: MainViewModel by viewModels()
    
    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onPermissionsGranted()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            ChapelotasTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val todayEvents by viewModel.todayEvents.collectAsStateWithLifecycle()
                val dailySummary by viewModel.dailySummary.collectAsStateWithLifecycle()
                val tomorrowSummary by viewModel.tomorrowSummary.collectAsStateWithLifecycle()
                
                MainScreen(
                    uiState = uiState,
                    todayEvents = todayEvents,
                    dailySummary = dailySummary,
                    tomorrowSummary = tomorrowSummary,
                    onRequestPermissions = {
                        calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                    },
                    onLoadDailySummary = viewModel::loadDailySummary,
                    onLoadTomorrowSummary = viewModel::loadTomorrowSummary,
                    onScheduleNotifications = viewModel::scheduleNotifications,
                    onToggleEventCritical = viewModel::toggleEventCritical,
                    onDismissError = viewModel::clearError
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    todayEvents: List<CalendarEvent>,
    dailySummary: String,
    tomorrowSummary: String,
    onRequestPermissions: () -> Unit,
    onLoadDailySummary: () -> Unit,
    onLoadTomorrowSummary: () -> Unit,
    onScheduleNotifications: (Boolean) -> Unit,
    onToggleEventCritical: (Long, Boolean) -> Unit,
    onDismissError: () -> Unit
) {
    var showTomorrowSummary by remember { mutableStateOf(false) }
    var isSarcasticMode by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "🔔 Chapelotas",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    // Switch para modo sarcástico
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("😏", fontSize = 20.sp)
                        Switch(
                            checked = isSarcasticMode,
                            onCheckedChange = { isSarcasticMode = it },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // Solicitar permisos
                uiState.requiresPermissions -> {
                    PermissionRequestScreen(onRequestPermissions)
                }
                
                // Cargando
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                // Contenido principal
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Botones de resumen
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        showTomorrowSummary = false
                                        onLoadDailySummary()
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = !uiState.isLoadingSummary
                                ) {
                                    Icon(Icons.Default.Today, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Resumen Hoy")
                                }
                                
                                Button(
                                    onClick = {
                                        showTomorrowSummary = true
                                        onLoadTomorrowSummary()
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = !uiState.isLoadingSummary
                                ) {
                                    Icon(Icons.Default.Tomorrow, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Mañana")
                                }
                            }
                        }
                        
                        // Mostrar resumen
                        if (dailySummary.isNotEmpty() || tomorrowSummary.isNotEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (showTomorrowSummary) 
                                            MaterialTheme.colorScheme.tertiaryContainer 
                                        else 
                                            MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = if (showTomorrowSummary) "📅 Resumen de Mañana" else "📋 Resumen de Hoy",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = if (showTomorrowSummary) tomorrowSummary else dailySummary,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Botón programar notificaciones
                        item {
                            Button(
                                onClick = { onScheduleNotifications(false) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Programar Notificaciones del Día")
                            }
                        }
                        
                        // Título eventos
                        if (todayEvents.isNotEmpty()) {
                            item {
                                Text(
                                    "📆 Eventos de Hoy",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                        
                        // Lista de eventos
                        items(todayEvents) { event ->
                            EventCard(
                                event = event,
                                onToggleCritical = { 
                                    onToggleEventCritical(event.id, !event.isCritical) 
                                }
                            )
                        }
                        
                        // Mensaje si no hay eventos
                        if (todayEvents.isEmpty() && !uiState.isLoadingSummary) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Text(
                                        text = "🎉 ¡No tenés eventos hoy!",
                                        modifier = Modifier.padding(32.dp),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Mostrar error si hay
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = onDismissError) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}

@Composable
fun EventCard(
    event: CalendarEvent,
    onToggleCritical: () -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (event.isCritical) 
                Color(0xFFFFEBEE) 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Hora y título
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (event.isCritical) {
                        Text("🚨 ", fontSize = 20.sp)
                    }
                    Text(
                        text = event.startTime.format(timeFormatter),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(" - ")
                    Text(
                        text = event.title,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Ubicación
                event.location?.let {
                    Text(
                        text = "📍 $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                // Duración
                Text(
                    text = "⏱️ ${event.durationInMinutes} minutos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Botón crítico
            IconButton(
                onClick = onToggleCritical,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (event.isCritical) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Icon(
                    Icons.Default.PriorityHigh,
                    contentDescription = if (event.isCritical) "Quitar crítico" else "Marcar crítico",
                    tint = if (event.isCritical) 
                        MaterialTheme.colorScheme.onError 
                    else 
                        MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun PermissionRequestScreen(onRequestPermissions: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(Modifier.height(24.dp))
        
        Text(
            "Chapelotas necesita acceso a tu calendario",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            "Para poder recordarte tus eventos y ser tu asistente personal insistente 😊",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(Modifier.height(32.dp))
        
        Button(
            onClick = onRequestPermissions,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Dar Permisos")
        }
    }
}

// Necesitamos agregar estos datos en MainUiState - los pegué acá temporalmente
data class MainUiState(
    val isLoading: Boolean = false,
    val isLoadingSummary: Boolean = false,
    val isFirstTimeUser: Boolean = false,
    val requiresPermissions: Boolean = false,
    val hasEventsToday: Boolean = false,
    val hasEventsTomorrow: Boolean = false,
    val criticalEventsCount: Int = 0,
    val lastScheduledCount: Int = 0,
    val availableCalendars: Map<Long, String> = emptyMap(),
    val error: String? = null
)