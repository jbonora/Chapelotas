package com.chapelotas.app

// Imports de Android
import android.Manifest
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

// Imports de AndroidX
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope  // ← ESTE ES EL QUE FALTABA

// Imports de Compose
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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

// Imports de tu app
import com.chapelotas.app.data.notifications.ChapelotasNotificationService
import com.chapelotas.app.domain.entities.CalendarEvent
import com.chapelotas.app.domain.entities.EventoConNotificaciones
import com.chapelotas.app.domain.entities.MasterPlan
import com.chapelotas.app.presentation.viewmodels.MainViewModel
import com.chapelotas.app.presentation.viewmodels.MainUiState
import com.chapelotas.app.presentation.viewmodels.CalendarMonitorViewModel
import com.chapelotas.app.ui.theme.ChapelotasTheme

// Imports de Dagger/Hilt
import dagger.hilt.android.AndroidEntryPoint

// Imports de Kotlin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val calendarMonitorViewModel: CalendarMonitorViewModel by viewModels()

    private var hasCheckedBatteryOptimization = false
    private var hasCheckedHuaweiSettings = false

    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onPermissionsGranted()
            // Iniciar el mono automáticamente cuando se dan permisos
            calendarMonitorViewModel.iniciarDia()

            // Después de permisos de calendario, verificar batería
            checkBatteryOptimization()
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

                // Estados del monitor
                val isMonitoring by calendarMonitorViewModel.isMonitoring.collectAsStateWithLifecycle()
                val currentPlan by calendarMonitorViewModel.currentPlan.collectAsStateWithLifecycle()

                // Iniciar el mono automáticamente si tenemos permisos
                LaunchedEffect(uiState.requiresPermissions) {
                    if (!uiState.requiresPermissions && !isMonitoring) {
                        delay(1000) // Pequeña espera para que todo se inicialice
                        calendarMonitorViewModel.iniciarDia()
                    }
                }

                // Verificar optimizaciones después de tener permisos
                LaunchedEffect(uiState.requiresPermissions) {
                    if (!uiState.requiresPermissions && !hasCheckedBatteryOptimization) {
                        delay(2000) // Esperar un poco más
                        checkBatteryOptimization()
                    }
                }

                MainScreen(
                    uiState = uiState,
                    todayEvents = todayEvents,
                    dailySummary = dailySummary,
                    tomorrowSummary = tomorrowSummary,
                    isMonitoring = isMonitoring,
                    currentPlan = currentPlan,
                    onRequestPermissions = {
                        calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                    },
                    onLoadDailySummary = viewModel::loadDailySummary,
                    onLoadTomorrowSummary = viewModel::loadTomorrowSummary,
                    onScheduleNotifications = viewModel::scheduleNotifications,
                    onToggleEventCritical = viewModel::toggleEventCritical,
                    onDismissError = viewModel::clearError,
                    onUpdateEventDistance = calendarMonitorViewModel::actualizarEvento
                )
            }
        }
    }

    /**
     * Verificar si estamos excluidos de optimización de batería
     */
    private fun checkBatteryOptimization() {
        if (hasCheckedBatteryOptimization) return
        hasCheckedBatteryOptimization = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = packageName

            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                // Mostrar diálogo explicativo
                AlertDialog.Builder(this)
                    .setTitle("🔋 Permiso Importante")
                    .setMessage(
                        "Para que Chapelotas funcione correctamente y no te pierdas ningún evento, " +
                                "necesita estar excluido de la optimización de batería.\n\n" +
                                "Esto NO afectará significativamente tu batería."
                    )
                    .setPositiveButton("Configurar") { _, _ ->
                        requestBatteryOptimization()
                    }
                    .setNegativeButton("Ahora no") { _, _ ->
                        // En Huawei es especialmente importante
                        if (Build.MANUFACTURER.lowercase() == "huawei") {
                            checkHuaweiSpecificSettings()
                        }
                    }
                    .setCancelable(false)
                    .show()
            } else {
                // Ya tenemos permiso de batería, verificar Huawei
                if (Build.MANUFACTURER.lowercase() == "huawei") {
                    checkHuaweiSpecificSettings()
                }
            }
        }
    }

    /**
     * Llevar al usuario a la configuración de optimización de batería
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestBatteryOptimization() {
        val intent = Intent().apply {
            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            data = Uri.parse("package:$packageName")
        }

        try {
            startActivity(intent)
            // Después de batería, verificar Huawei
            if (Build.MANUFACTURER.lowercase() == "huawei") {
                lifecycleScope.launch {
                    delay(1000)
                    checkHuaweiSpecificSettings()
                }
            }
        } catch (e: Exception) {
            // Si falla, abrir configuración general de batería
            val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(fallbackIntent)
        }
    }

    /**
     * Configuración específica para Huawei
     */
    private fun checkHuaweiSpecificSettings() {
        if (hasCheckedHuaweiSettings) return
        hasCheckedHuaweiSettings = true

        AlertDialog.Builder(this)
            .setTitle("📱 Configuración Huawei")
            .setMessage(
                "Huawei requiere pasos adicionales para que Chapelotas funcione perfectamente:\n\n" +
                        "1. Inicio automático\n" +
                        "2. Ejecución en segundo plano\n" +
                        "3. Bloqueo de aplicaciones\n\n" +
                        "¿Deseas configurarlo ahora? (Muy recomendado)"
            )
            .setPositiveButton("Configurar") { _, _ ->
                openHuaweiSettings()
            }
            .setNegativeButton("Omitir") { _, _ ->
                // Mostrar advertencia
                AlertDialog.Builder(this)
                    .setTitle("⚠️ Advertencia")
                    .setMessage(
                        "Sin esta configuración, Huawei podría cerrar Chapelotas y " +
                                "podrías perderte notificaciones importantes.\n\n" +
                                "Puedes configurarlo luego desde Ajustes → Apps → Chapelotas"
                    )
                    .setPositiveButton("Entendido", null)
                    .show()
            }
            .show()
    }

    /**
     * Abrir configuración específica de Huawei
     */
    private fun openHuaweiSettings() {
        try {
            // Intentar abrir gestor de inicio automático
            val intent = Intent().apply {
                component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            }
            startActivity(intent)

            // Mostrar instrucciones
            AlertDialog.Builder(this)
                .setTitle("📋 Instrucciones Huawei")
                .setMessage(
                    "1. Busca 'Chapelotas' en la lista\n" +
                            "2. Activa 'Permitir inicio automático'\n" +
                            "3. Vuelve y ve a 'Batería' → 'Inicio de aplicaciones'\n" +
                            "4. Busca 'Chapelotas' y selecciona 'Gestión manual'\n" +
                            "5. Activa todas las opciones"
                )
                .setPositiveButton("Entendido", null)
                .show()

        } catch (e: Exception) {
            try {
                // Plan B: Configuración de optimización de batería de Huawei
                val intent = Intent().apply {
                    component = ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity"
                    )
                }
                startActivity(intent)
            } catch (e2: Exception) {
                // Plan C: Abrir configuración general de la app
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)

                // Mostrar mensaje de ayuda
                AlertDialog.Builder(this)
                    .setTitle("ℹ️ Configuración Manual")
                    .setMessage(
                        "Ve a:\n" +
                                "1. Batería → Inicio de aplicaciones\n" +
                                "2. Busca Chapelotas\n" +
                                "3. Selecciona 'Gestión manual'\n" +
                                "4. Activa todas las opciones"
                    )
                    .setPositiveButton("OK", null)
                    .show()
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
    isMonitoring: Boolean,
    currentPlan: MasterPlan?,
    onRequestPermissions: () -> Unit,
    onLoadDailySummary: () -> Unit,
    onLoadTomorrowSummary: () -> Unit,
    onScheduleNotifications: (Boolean) -> Unit,
    onToggleEventCritical: (Long, Boolean) -> Unit,
    onDismissError: () -> Unit,
    onUpdateEventDistance: (String, Boolean, String) -> Unit
) {
    var showTomorrowSummary by remember { mutableStateOf(false) }
    var isSarcasticMode by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔔 Chapelotas", fontWeight = FontWeight.Bold)
                        // Indicador de estado siempre visible
                        Spacer(Modifier.width(8.dp))
                        Badge(
                            containerColor = if (isMonitoring) Color.Green else Color.Gray
                        ) {
                            Text(
                                if (isMonitoring) "🐵" else "💤",
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    // Switch modo sarcástico
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
                        // Card de estado del sistema - Siempre visible
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isMonitoring)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            if (isMonitoring) "🐵 Mono Vigilando" else "⚠️ Mono Activándose...",
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "Próximo checkeo: ${currentPlan?.proximoCheckeo ?: "--:--"}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    currentPlan?.eventosCriticosPendientes?.let { criticos ->
                                        if (criticos.isNotEmpty()) {
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                "⚠️ Eventos críticos pendientes: ${criticos.size}",
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }

                                    // Mostrar si el plan está cargado
                                    if (currentPlan != null) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "📅 ${currentPlan.eventosHoy.size} eventos monitoreados",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }

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
                                    Icon(Icons.Default.DateRange, contentDescription = null)
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
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
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
                            EventCardEnhanced(
                                event = event,
                                planEvent = currentPlan?.eventosHoy?.find { it.id == event.id.toString() },
                                onToggleCritical = {
                                    onToggleEventCritical(event.id, !event.isCritical)
                                },
                                onUpdateDistance = { distance ->
                                    onUpdateEventDistance(
                                        event.id.toString(),
                                        event.isCritical,
                                        distance
                                    )
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
                                    Column(
                                        modifier = Modifier.padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "🎉 ¡No tenés eventos hoy!",
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = if (isMonitoring)
                                                "El mono sigue vigilando por si aparece algo 🐵"
                                            else
                                                "Activando vigilancia...",
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
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

// ... El resto del código sigue igual (EventCardEnhanced, EventCard, PermissionRequestScreen)
// Los incluyo por completitud:

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCardEnhanced(
    event: CalendarEvent,
    planEvent: EventoConNotificaciones?,
    onToggleCritical: () -> Unit,
    onUpdateDistance: (String) -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    var showDistanceDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                event.isCritical -> Color(0xFFFFEBEE)
                planEvent?.conflicto != null -> Color(0xFFFFF3E0) // Amarillo si hay conflicto
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Fila principal
            Row(
                modifier = Modifier.fillMaxWidth(),
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

                    // Ubicación con chip de distancia
                    event.location?.let {
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📍 $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            planEvent?.distancia?.let { dist ->
                                Spacer(Modifier.width(8.dp))
                                AssistChip(
                                    onClick = { showDistanceDialog = true },
                                    label = {
                                        Text(
                                            when(dist) {
                                                "lejos" -> "🚗 Lejos"
                                                "cerca" -> "🚶 Cerca"
                                                else -> "🏢 En la ofi"
                                            },
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }
                    }

                    // Duración y avisos
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⏱️ ${event.durationInMinutes} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        planEvent?.avisosSugeridos?.let { avisos ->
                            Text(
                                text = "🔔 ${avisos.joinToString(", ")} min antes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Mostrar conflicto si existe
                    planEvent?.conflicto?.let { conflicto ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = "⚠️ ${conflicto.mensaje}",
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
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
                        Icons.Default.Warning,
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

    // Dialog para cambiar distancia
    if (showDistanceDialog) {
        AlertDialog(
            onDismissRequest = { showDistanceDialog = false },
            title = { Text("¿Qué tan lejos está?") },
            text = {
                Column {
                    listOf(
                        "en la ofi" to "🏢 En la oficina",
                        "cerca" to "🚶 Cerca (caminando)",
                        "lejos" to "🚗 Lejos (necesito transporte)"
                    ).forEach { (value, label) ->
                        TextButton(
                            onClick = {
                                onUpdateDistance(value)
                                showDistanceDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDistanceDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// EventCard original para compatibilidad (cuando no hay plan)
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
                    Icons.Default.Warning,
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
            Icons.Default.DateRange,
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