package com.chapelotas.app

import android.Manifest
import android.app.AlertDialog as AndroidAlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import com.chapelotas.app.domain.events.ChapelotasEvent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log // ---> IMPORT AÑADIDO <---
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chapelotas.app.domain.entities.ChapelotasNotification
import com.chapelotas.app.presentation.ui.PlanScreen
import com.chapelotas.app.presentation.ui.SettingsScreen
import com.chapelotas.app.presentation.ui.TodayScreen
import com.chapelotas.app.presentation.viewmodels.CalendarMonitorViewModel
import com.chapelotas.app.presentation.viewmodels.MainViewModel
import com.chapelotas.app.ui.theme.ChapelotasTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.LaunchedEffect

sealed class Screen(val route: String, val icon: ImageVector, val label: String) {
    object Today : Screen("today", Icons.Default.Email, "Hoy")
    object Plan : Screen("plan", Icons.Default.DateRange, "Plan del Día")
    object Settings : Screen("settings", Icons.Default.Settings, "Ajustes")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val calendarMonitorViewModel: CalendarMonitorViewModel by viewModels()
    private val prefs by lazy { getSharedPreferences("chapelotas_dialog_prefs", Context.MODE_PRIVATE) }

    private fun checkDrawOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                // Mostramos un diálogo para guiar al usuario
                AndroidAlertDialog.Builder(this)
                    .setTitle("Permiso Adicional Necesario")
                    .setMessage("Para que las alertas críticas puedan aparecer inmediatamente, Chapelotas necesita permiso para mostrarse sobre otras aplicaciones.")
                    .setPositiveButton("Dar Permiso") { _, _ ->
                        startActivity(intent)
                    }
                    .setNegativeButton("Ahora no", null)
                    .setCancelable(false)
                    .show()
            }
        }
    }
    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onPermissionsGranted()
            calendarMonitorViewModel.iniciarDia()
            checkBatteryOptimization()
            checkFullScreenIntentPermission()
            checkDrawOverlayPermission()
        } else {
            Toast.makeText(this, "Chapelotas no puede funcionar sin acceso al calendario.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("ChapelotasDebug", "MainActivity - ON_CREATE: La actividad principal se está creando.")
        super.onCreate(savedInstanceState)
        val startRoute = intent.getStringExtra("navigation_route") ?: Screen.Today.route
        viewModel.handleIntent(intent)
        setContent {
            ChapelotasTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val navController = rememberNavController()
                val notificationToShow by viewModel.notificationToShow.collectAsStateWithLifecycle()

                if (notificationToShow != null) {
                    NotificationDialog(
                        notification = notificationToShow!!,
                        onDismiss = { viewModel.dismissNotificationDialog() },
                        onMarkAsDone = { viewModel.markNotificationAsDone(it) },
                        onSnooze = { id, min -> viewModel.snoozeNotification(id, min) }
                    )
                }

                when {
                    uiState.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    uiState.requiresPermissions -> {
                        PermissionRequestScreen(onRequestPermissions = {
                            calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                        })
                    }
                    else -> {
                        calendarMonitorViewModel.iniciarDia()
                        MainAppScaffold(navController, startRoute, viewModel, calendarMonitorViewModel)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.handleIntent(intent)
        setIntent(intent)
    }

    private fun checkBatteryOptimization() {
        if (prefs.getBoolean("has_checked_battery", false)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = packageName
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                AndroidAlertDialog.Builder(this)
                    .setTitle("🔋 Permiso MUY Importante")
                    .setMessage("Para que Chapelotas pueda trabajar en segundo plano (y ser hincha pelotas), necesita que la saques de la 'Optimización de Batería'. Sin esto, el sistema la dormirá.")
                    .setPositiveButton("Configurar") { _, _ -> requestBatteryOptimization() }
                    .setNegativeButton("Ahora no", null)
                    .setCancelable(false)
                    .show()
            }
            prefs.edit().putBoolean("has_checked_battery", true).apply()
        }
    }

    private fun requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply { data = Uri.parse("package:$packageName") }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }

    private fun checkFullScreenIntentPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.canUseFullScreenIntent()) {
                AndroidAlertDialog.Builder(this)
                    .setTitle("🚨 Permiso Final (y muy importante)")
                    .setMessage("Para que las alertas críticas funcionen como una llamada que no puedes ignorar, Chapelotas necesita un permiso especial para mostrarse sobre otras apps. Sin esto, las alertas críticas serán solo notificaciones normales.")
                    .setPositiveButton("Dar Permiso") { _, _ ->
                        val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        startActivity(intent)
                    }
                    .setNegativeButton("Más tarde", null)
                    .setCancelable(false)
                    .show()
            }
        }
    }
}

@Composable
fun MainAppScaffold(
    navController: NavHostController,
    startRoute: String,
    viewModel: MainViewModel,
    calendarMonitorViewModel: CalendarMonitorViewModel
) {
    val navItems = listOf(Screen.Today, Screen.Plan, Screen.Settings)
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is ChapelotasEvent.NavigateToPlan -> {
                    navController.navigate(Screen.Plan.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                else -> { /* Ignorar otros eventos */ }
            }
        }
    }
    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                navItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(innerPadding)  // ← Usar innerPadding, no padding
        ) {
            composable(Screen.Today.route) {
                TodayScreen(viewModel = viewModel)  // ← Usar viewModel, no mainViewModel
            }
            composable(Screen.Plan.route) {
                PlanScreen(
                    mainViewModel = viewModel,  // ← Usar viewModel
                    calendarMonitorViewModel = calendarMonitorViewModel
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    mainViewModel = viewModel,  // ← Usar viewModel
                    calendarViewModel = calendarMonitorViewModel
                )
            }
        }
    }
}

@Composable
fun NotificationDialog(
    notification: ChapelotasNotification,
    onDismiss: () -> Unit,
    onMarkAsDone: (Long) -> Unit,
    onSnooze: (Long, Int) -> Unit
) {
    val notificationId = notification.id.toLongOrNull() ?: 0L
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mensaje de Chapelotas") },
        text = { Text(notification.message) },
        confirmButton = {
            TextButton(
                onClick = {
                    if (notificationId != 0L) onMarkAsDone(notificationId)
                    onDismiss()
                }
            ) { Text("Ya lo hice") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    if (notificationId != 0L) onSnooze(notificationId, 15)
                    onDismiss()
                }) { Text("En 15 min") }
                TextButton(onClick = onDismiss) { Text("Entendido") }
            }
        }
    )
}

@Composable
fun PermissionRequestScreen(onRequestPermissions: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Text("Chapelotas necesita acceso a tu calendario", fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text("Para poder recordarte tus eventos y ser tu asistente personal insistente 😊", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onRequestPermissions, modifier = Modifier.fillMaxWidth()) { Text("Dar Permisos") }
    }
}