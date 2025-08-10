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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chapelotas.app.R
import com.chapelotas.app.battery.BatteryProtectionManager
import com.chapelotas.app.presentation.ui.SettingsScreen
import com.chapelotas.app.presentation.viewmodels.SetupStep
import com.chapelotas.app.presentation.viewmodels.SetupViewModel

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            viewModel.nextStep()
        }
    )

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { viewModel.nextStep() }
    )

    // Este LaunchedEffect ahora observa correctamente la bandera 'isFinished'.
    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) {
            onSetupComplete()
        }
    }

    AnimatedContent(targetState = uiState.currentStep, label = "SetupStepAnimation") { step ->
        // La pantalla de personalización se maneja correctamente.
        if (step == SetupStep.CustomizingSettings) {
            SettingsScreen(
                onSetupComplete = { viewModel.finishSetup() }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                when (step) {
                    SetupStep.Welcome -> WelcomeStep(onNext = { viewModel.nextStep() })

                    SetupStep.CalendarPermission -> PermissionStep(
                        title = stringResource(id = R.string.setup_calendar_permission_title),
                        text = stringResource(id = R.string.setup_calendar_permission_message),
                        buttonText = stringResource(id = R.string.setup_calendar_permission_button),
                        onAction = {
                            calendarPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_CALENDAR,
                                    Manifest.permission.WRITE_CALENDAR
                                )
                            )
                        }
                    )

                    SetupStep.NotificationPermission -> PermissionStep(
                        title = stringResource(id = R.string.setup_notification_permission_title),
                        text = stringResource(id = R.string.setup_notification_permission_message),
                        buttonText = stringResource(id = R.string.setup_notification_permission_button),
                        onAction = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.nextStep()
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
                        onCustomize = { viewModel.startCustomization() }
                    )

                    // La referencia a 'Finished' ha sido eliminada.
                    // El caso 'CustomizingSettings' ya se maneja arriba.
                    SetupStep.CustomizingSettings -> { /* No hacer nada aquí */ }
                }
            }
        }
    }
}


@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    StepContainer(
        title = stringResource(id = R.string.setup_welcome_title),
        text = stringResource(id = R.string.setup_welcome_message),
        buttonText = stringResource(id = R.string.setup_welcome_button),
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
        title = stringResource(id = R.string.setup_battery_optimization_title),
        text = stringResource(id = R.string.setup_battery_optimization_message),
        buttonText = stringResource(id = R.string.setup_battery_optimization_button),
        onAction = onNext
    )
}

@Composable
private fun NameInputStep(name: String, onNameChange: (String) -> Unit, onNext: () -> Unit) {
    StepContainer(
        title = stringResource(id = R.string.setup_name_input_title),
        text = stringResource(id = R.string.setup_name_input_message),
        buttonText = stringResource(id = R.string.next),
        onAction = onNext,
        content = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(stringResource(id = R.string.setup_name_input_label)) },
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
            text = stringResource(id = R.string.setup_final_settings_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(
                id = R.string.setup_final_settings_message,
                userName.ifBlank { stringResource(id = R.string.setup_user_name_placeholder) }
            ),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onUseDefaults,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(id = R.string.setup_final_settings_default_button))
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onCustomize,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(id = R.string.setup_final_settings_customize_button))
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