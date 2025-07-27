package com.chapelotas.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chapelotas.app.presentation.ui.DebugScreen
import com.chapelotas.app.presentation.ui.SettingsScreen
import com.chapelotas.app.presentation.ui.home.HomeScreen
import com.chapelotas.app.presentation.ui.setup.SetupScreen
import com.chapelotas.app.presentation.ui.theme.ChapelotasTheme
import com.chapelotas.app.presentation.viewmodels.RootViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val rootViewModel: RootViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Muestra una pantalla de carga mientras el RootViewModel decide
        installSplashScreen().setKeepOnScreenCondition {
            rootViewModel.uiState.value.isLoading
        }

        setContent {
            ChapelotasTheme {
                val uiState by rootViewModel.uiState.collectAsState()

                if (uiState.isLoading) {
                    // Muestra un indicador de progreso durante la carga inicial
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    // Una vez cargado, decide qué flujo mostrar
                    AppNavigation(isFirstTimeUser = uiState.isFirstTimeUser)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(isFirstTimeUser: Boolean) {
    val navController = rememberNavController()
    // Decide la ruta inicial basada en si es la primera vez que el usuario abre la app.
    val startDestination = if (isFirstTimeUser) "setup_flow" else "main_flow"

    NavHost(navController = navController, startDestination = startDestination) {
        // Define un "sub-gráfico" de navegación para todo el flujo de configuración.
        composable(route = "setup_flow") {
            SetupScreen(
                onSetupComplete = {
                    // Una vez completado, navega al flujo principal y limpia el historial
                    // para que el usuario no pueda volver atrás con el botón de retroceso.
                    navController.navigate("main_flow") {
                        popUpTo("setup_flow") { inclusive = true }
                    }
                }
            )
        }
        // Define el "sub-gráfico" de la aplicación principal.
        composable(route = "main_flow") {
            MainNavigationContainer()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationContainer() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            AppBottomNavBar(navController = navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "today",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("today") {
                HomeScreen(navController = navController)
            }
            composable("debug") {
                DebugScreen()
            }
            composable("settings") {
                SettingsScreen(navController = navController)
            }
        }
    }
}

@Composable
fun AppBottomNavBar(navController: NavController) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Hoy") },
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