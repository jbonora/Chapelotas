package com.chapelotas.app.presentation.ui.setup

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chapelotas.app.battery.BatteryProtectionManager
import com.chapelotas.app.presentation.ui.SettingsScreen
import com.chapelotas.app.presentation.viewmodels.SetupStep
import com.chapelotas.app.presentation.viewmodels.SetupViewModel

/**
 * Contenedor principal para todo el flujo de configuración.
 * Observa el estado del ViewModel y muestra el paso actual.
 */
@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Launcher para el permiso de calendario
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            viewModel.nextStep()
        }
    )

    // Launcher para el permiso de notificaciones
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            viewModel.nextStep()
        }
    )

    // Efecto que se dispara cuando el estado cambia a "Finished"
    LaunchedEffect(uiState.currentStep) {
        if (uiState.currentStep == SetupStep.Finished) {
            onSetupComplete()
        }
    }

    // AnimatedContent hace que la transición entre pasos sea suave
    AnimatedContent(targetState = uiState.currentStep, label = "SetupStepAnimation") { step ->
        // Si el paso es la personalización de ajustes, muestra la pantalla de Settings.
        if (step == SetupStep.CustomizingSettings) {
            SettingsScreen(
                onSetupComplete = { viewModel.finishSetup() }
            )
        } else {
            // Para todos los demás pasos, muestra el contenedor estándar.
            Box(modifier = Modifier.fillMaxSize()) {
                when (step) {
                    SetupStep.Welcome -> WelcomeStep(onNext = { viewModel.nextStep() })

                    SetupStep.CalendarPermission -> PermissionStep(
                        title = "Permiso de Calendario",
                        text = "Para poder recordarte tus eventos, necesito permiso para leer el calendario de tu teléfono. Sin esto, no puedo funcionar.",
                        buttonText = "Dar Permiso de Calendario",
                        onAction = { calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR) }
                    )

                    SetupStep.NotificationPermission -> PermissionStep(
                        title = "Permiso de Notificaciones",
                        text = "También necesito permiso para enviarte notificaciones. De lo contrario, mis recordatorios no te llegarán.",
                        buttonText = "Dar Permiso de Notificaciones",
                        onAction = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.nextStep() // En versiones antiguas no se necesita, avanza directamente.
                            }
                        }
                    )

                    SetupStep.BatteryOptimization -> BatteryStep(
                        onNext = {
                            BatteryProtectionManager.showBatteryProtectionDialog(context) {
                                viewModel.nextStep()
                            }
                        }
                    )

                    SetupStep.NameInput -> NameInputStep(
                        name = uiState.userName,
                        onNameChange = { viewModel.onNameChange(it) },
                        onNext = { viewModel.nextStep() }
                    )

                    SetupStep.FinalSettings -> FinalSettingsStep(
                        userName = uiState.userName,
                        onUseDefaults = { viewModel.finishSetup() },
                        // --- LA LÍNEA CORREGIDA ---
                        // Ahora, al personalizar, llamamos a la función correcta.
                        onCustomize = { viewModel.startCustomization() }
                        // -------------------------
                    )

                    SetupStep.Finished, SetupStep.CustomizingSettings -> {
                        // Pantalla vacía mientras se navega o se muestra la otra pantalla.
                    }
                }
            }
        }
    }
}


// --- COMPONENTES VISUALES PARA CADA PASO (SIN CAMBIOS) ---

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    StepContainer(
        title = "¡Bienvenido a Chapelotas!",
        text = "Tu nuevo asistente personal (un poco sarcástico) está listo para empezar. Vamos a configurar algunas cosas rápidas.",
        buttonText = "Comenzar",
        onAction = onNext
    )
}

@Composable
private fun PermissionStep(title: String, text: String, buttonText: String, onAction: () -> Unit) {
    StepContainer(title = title, text = text, buttonText = buttonText, onAction = onAction)
}

@Composable
private fun BatteryStep(onNext: () -> Unit) {
    StepContainer(
        title = "Optimización de Batería",
        text = "Algunos teléfonos son muy agresivos para ahorrar batería y podrían 'dormirme'. Para asegurar que mis recordatorios lleguen siempre a tiempo, te guiaré para que me agregues a la lista de 'aplicaciones protegidas'.",
        buttonText = "Configurar Batería",
        onAction = onNext
    )
}

@Composable
private fun NameInputStep(name: String, onNameChange: (String) -> Unit, onNext: () -> Unit) {
    StepContainer(
        title = "Un último detalle...",
        text = "¿Cómo te llamas? Esto hará que los recordatorios sean un poco más personales.",
        buttonText = "Siguiente",
        onAction = onNext,
        content = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Tu nombre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Composable
private fun FinalSettingsStep(
    userName: String,
    onUseDefaults: () -> Unit,
    onCustomize: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Ajustes de Recordatorios",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "¡Ya casi terminamos, ${userName.ifBlank { "crack" }}! Puedo empezar a funcionar con mis ajustes recomendados, o puedes personalizarlos ahora. Siempre podrás cambiarlos más tarde desde el menú de Configuración.",
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onUseDefaults,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Usar Ajustes por Defecto y Empezar")
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onCustomize,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Personalizar Ahora")
        }
    }
}

@Composable
private fun StepContainer(
    title: String,
    text: String,
    buttonText: String,
    onAction: () -> Unit,
    content: @Composable () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = title, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text(text = text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        content()
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
            Text(buttonText)
        }
    }
}