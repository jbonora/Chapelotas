package com.chapelotas.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chapelotas.app.di.Constants
import com.chapelotas.app.presentation.ui.SettingsScreen
import com.chapelotas.app.presentation.ui.TaskDetailScreen
import com.chapelotas.app.presentation.ui.TomorrowScreen
// import com.chapelotas.app.presentation.ui.DebugScreen  // Comentado si da error
import com.chapelotas.app.presentation.ui.home.HomeScreen
import com.chapelotas.app.presentation.ui.home.SevenDaysScreen
import com.chapelotas.app.presentation.ui.setup.SetupScreen
import com.chapelotas.app.presentation.ui.theme.ChapelotasTheme
import com.chapelotas.app.presentation.viewmodels.RootViewEvent
import com.chapelotas.app.presentation.viewmodels.RootViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val rootViewModel: RootViewModel by viewModels()

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationContainer(
    rootNavController: NavController,
    highlightedTaskId: String?,
    onHighlightConsumed: () -> Unit
) {
    val navController = rememberNavController()
    var hasContentBelow by remember { mutableStateOf(true) } // Iniciar en true para mostrar línea por defecto

    Scaffold(
        bottomBar = {
            AppBottomNavBar(
                navController = navController,
                hasContentBelow = hasContentBelow
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "today",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("today") {
                HomeScreen(
                    navController = navController,
                    highlightedTaskId = highlightedTaskId,
                    onHighlightConsumed = onHighlightConsumed,
                    onScrollChanged = { canScrollDown ->
                        hasContentBelow = canScrollDown
                    }
                )
            }
            composable("seven_days") {
                SevenDaysScreen(
                    navController = navController
                    // Comentado hasta que actualices SevenDaysScreen:
                    // onScrollChanged = { scrolled -> isScrolled = scrolled }
                )
            }
            composable("tomorrow_debug") {
                TomorrowScreen()
            }
            composable("debug") {
                // Si DebugScreen no existe, usar un placeholder temporal:
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Debug Screen")
                }
                // DebugScreen()  // Descomentar cuando exista
            }
            composable("settings") {
                SettingsScreen(navController = navController)
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
    hasContentBelow: Boolean = false  // true = hay flechita abajo = mostrar línea
) {
    // Animación suave igual que las flechitas
    val lineAlpha by animateFloatAsState(
        targetValue = if (hasContentBelow) 1f else 0f,
        animationSpec = tween(200),
        label = "line_alpha"
    )

    Column {
        // Línea superior que aparece cuando hay flechita de scroll abajo
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

            NavigationBarItem(
                icon = {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = "Hoy"
                    )
                },
                label = { Text("Hoy") },
                selected = currentRoute == "today",
                onClick = {
                    navController.navigate("today") {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
            NavigationBarItem(
                icon = {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "7 Días"
                    )
                },
                label = { Text("7 Días") },
                selected = currentRoute == "seven_days",
                onClick = {
                    navController.navigate("seven_days") {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
            NavigationBarItem(
                icon = {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = "Mañana"
                    )
                },
                label = { Text("Mañana") },
                selected = currentRoute == "tomorrow_debug",
                onClick = {
                    navController.navigate("tomorrow_debug") {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
            NavigationBarItem(
                icon = {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = "Debug"
                    )
                },
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