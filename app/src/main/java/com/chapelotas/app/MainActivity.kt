package com.chapelotas.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chapelotas.app.di.Constants
import com.chapelotas.app.domain.debug.DebugLog
import com.chapelotas.app.domain.repositories.PreferencesRepository
import com.chapelotas.app.presentation.ui.AgendaScreen // ¡NUEVO! Importa la nueva pantalla
import com.chapelotas.app.presentation.ui.PersonalityManagerScreen
import com.chapelotas.app.presentation.ui.SettingsScreen
import com.chapelotas.app.presentation.ui.TaskDetailScreen
import com.chapelotas.app.presentation.ui.TomorrowScreen
import com.chapelotas.app.presentation.ui.setup.SetupScreen
import com.chapelotas.app.presentation.ui.theme.ChapelotasTheme
import com.chapelotas.app.presentation.viewmodels.RootViewEvent
import com.chapelotas.app.presentation.viewmodels.RootViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val rootViewModel: RootViewModel by viewModels()

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var debugLog: DebugLog

    private var heartbeatJob: Job? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.getStringExtra(Constants.EXTRA_TASK_ID_TO_HIGHLIGHT)?.let { taskId ->
            if (taskId.isNotBlank()) {
                rootViewModel.onNavigateTo("main_flow")
                rootViewModel.onHighlightTask(taskId)
                intent.removeExtra(Constants.EXTRA_TASK_ID_TO_HIGHLIGHT)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().setKeepOnScreenCondition {
            rootViewModel.uiState.value.isLoading
        }

        handleIntent(intent)

        setContent {
            ChapelotasTheme {
                val uiState by rootViewModel.uiState.collectAsState()

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    AppNavigation(
                        isFirstTimeUser = uiState.isFirstTimeUser,
                        rootViewModel = rootViewModel
                    )
                }
            }
        }
        startActivityHeartbeat()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            preferencesRepository.updateLastActivityTime(System.currentTimeMillis())
            debugLog.add("📱 ACTIVITY: App en primer plano")
        }
        if (heartbeatJob?.isActive != true) {
            startActivityHeartbeat()
        }
    }

    override fun onPause() {
        super.onPause()
        lifecycleScope.launch {
            debugLog.add("📱 ACTIVITY: App en background")
        }
    }

    override fun onDestroy() {
        heartbeatJob?.cancel()
        super.onDestroy()
    }

    private fun startActivityHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = lifecycleScope.launch {
            while (isActive) {
                try {
                    delay(30_000)
                    preferencesRepository.updateLastActivityTime(System.currentTimeMillis())
                    if (System.currentTimeMillis() % 300_000 < 30_000) {
                        debugLog.add("💓 ACTIVITY: Heartbeat - App activa")
                    }
                } catch (e: Exception) {
                    debugLog.add("❌ ACTIVITY: Error en heartbeat: ${e.message}")
                }
            }
        }
        debugLog.add("💓 ACTIVITY: Heartbeat iniciado")
    }
}

@Composable
fun AppNavigation(isFirstTimeUser: Boolean, rootViewModel: RootViewModel) {
    val navController = rememberNavController()
    val startDestination = if (isFirstTimeUser) "setup_flow" else "main_flow"

    var highlightedTaskId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        rootViewModel.eventFlow.collectLatest { event ->
            when (event) {
                is RootViewEvent.NavigateTo -> {
                    navController.navigate(event.route) {
                        if (event.route == "main_flow") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                        launchSingleTop = true
                    }
                }
                is RootViewEvent.HighlightTask -> {
                    highlightedTaskId = event.taskId
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(route = "setup_flow") {
            SetupScreen(
                onSetupComplete = { rootViewModel.onNavigateTo("main_flow") }
            )
        }
        composable(route = "main_flow") {
            MainNavigationContainer(
                rootNavController = navController,
                highlightedTaskId = highlightedTaskId,
                onHighlightConsumed = { highlightedTaskId = null }
            )
        }
    }
}

@Composable
fun MainNavigationContainer(
    rootNavController: NavController,
    highlightedTaskId: String?,
    onHighlightConsumed: () -> Unit
) {
    val navController = rememberNavController()
    var hasContentBelow by remember { mutableStateOf(true) }

    Scaffold(
        bottomBar = {
            // La barra de navegación ahora se ha simplificado
            AppBottomNavBar(
                navController = navController,
                hasContentBelow = hasContentBelow
            )
        }
    ) { paddingValues ->
        // --- CAMBIO #1: La ruta de inicio ahora es "agenda" ---
        NavHost(
            navController = navController,
            startDestination = "agenda",
            modifier = Modifier.padding(paddingValues)
        ) {
            // --- CAMBIO #2: Se reemplazan "today" y "seven_days" por "agenda" ---
            composable("agenda") {
                AgendaScreen(
                    navController = navController,
                    highlightedTaskId = highlightedTaskId,
                    onHighlightConsumed = onHighlightConsumed,
                    onScrollChanged = { canScrollDown ->
                        hasContentBelow = canScrollDown
                    }
                )
            }
            // El resto de las rutas se mantienen igual
            composable("tomorrow_debug") {
                TomorrowScreen()
            }
            composable("debug") {
                // Aquí va tu pantalla de Debug
            }
            composable("settings") {
                SettingsScreen(navController = navController)
            }
            composable("personality_manager") {
                PersonalityManagerScreen(navController = navController)
            }
            composable(
                route = "task_detail/{taskId}",
                arguments = listOf(navArgument("taskId") { type = NavType.StringType })
            ) {
                TaskDetailScreen(navController = navController)
            }
        }
    }
}


@Composable
fun AppBottomNavBar(
    navController: NavController,
    hasContentBelow: Boolean = false
) {
    val lineAlpha by animateFloatAsState(
        targetValue = if (hasContentBelow) 1f else 0f,
        animationSpec = tween(200),
        label = "line_alpha"
    )

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = lineAlpha * 0.25f))
        )

        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            tonalElevation = 0.dp
        ) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            // --- CAMBIO #3: El nuevo y único botón para la vista principal ---
            NavigationBarItem(
                icon = { Icon(Icons.Default.DateRange, contentDescription = "Agenda") },
                label = { Text("Agenda") },
                selected = currentRoute == "agenda",
                onClick = {
                    navController.navigate("agenda") {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )

            // Se mantienen los otros botones que no se fusionaron
            NavigationBarItem(
                icon = { Icon(Icons.Default.Build, contentDescription = "Debug") },
                label = { Text("Debug") },
                selected = currentRoute == "debug",
                onClick = {
                    navController.navigate("debug") {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}