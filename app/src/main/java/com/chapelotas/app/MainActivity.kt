package com.chapelotas.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chapelotas.app.presentation.ui.DebugScreen
import com.chapelotas.app.presentation.ui.SettingsScreen
import com.chapelotas.app.presentation.ui.home.HomeScreen
import com.chapelotas.app.presentation.ui.theme.ChapelotasTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChapelotasTheme {
                // Lógica de permisos que se aplica a toda la app
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted -> }
                )

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                // Inicia el contenedor de navegación
                MainNavigationContainer()
            }
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
            // Aquí se define qué pantalla mostrar para cada ruta
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