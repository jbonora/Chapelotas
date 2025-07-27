package com.chapelotas.app.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chapelotas.app.presentation.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController? = null,
    onSetupComplete: (() -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración de Recordatorios") },
                navigationIcon = {
                    if (onSetupComplete == null) {
                        IconButton(onClick = { navController?.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = settings.userName,
                    onValueChange = { viewModel.onSettingsChanged(settings.copy(userName = it)) },
                    label = { Text("Nombre del usuario") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Horario de Trabajo", style = MaterialTheme.typography.titleMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Recibir avisos las 24 horas")
                    Switch(
                        checked = settings.workHours24h,
                        onCheckedChange = { viewModel.onSettingsChanged(settings.copy(workHours24h = it)) }
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    OutlinedTextField(
                        value = settings.workStartTime,
                        onValueChange = { viewModel.onSettingsChanged(settings.copy(workStartTime = it)) },
                        label = { Text("Inicio") },
                        modifier = Modifier.weight(1f),
                        enabled = !settings.workHours24h
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    OutlinedTextField(
                        value = settings.workEndTime,
                        onValueChange = { viewModel.onSettingsChanged(settings.copy(workEndTime = it)) },
                        label = { Text("Fin") },
                        modifier = Modifier.weight(1f),
                        enabled = !settings.workHours24h
                    )
                }

                Text("Avisos previos al evento (minutos antes)", style = MaterialTheme.typography.titleMedium)
                SettingsSlider(
                    label = "Primer aviso",
                    value = settings.firstReminder,
                    onValueChange = { viewModel.onSettingsChanged(settings.copy(firstReminder = it)) }
                )
                SettingsSlider(
                    label = "Segundo aviso",
                    value = settings.secondReminder,
                    onValueChange = { viewModel.onSettingsChanged(settings.copy(secondReminder = it)) }
                )
                SettingsSlider(
                    label = "Tercer aviso",
                    value = settings.thirdReminder,
                    onValueChange = { viewModel.onSettingsChanged(settings.copy(thirdReminder = it)) }
                )

                Text("Intervalo de insistencia (minutos)", style = MaterialTheme.typography.titleMedium)
                // --- SLIDERS CORREGIDOS CON TU LÓGICA ---
                SettingsSlider(
                    label = "Baja insistencia (evento en curso aceptado)",
                    value = settings.lowUrgencyInterval,
                    onValueChange = { viewModel.onSettingsChanged(settings.copy(lowUrgencyInterval = it)) }
                )
                SettingsSlider(
                    label = "Alta insistencia (en curso no aceptado / demorado)",
                    value = settings.highUrgencyInterval,
                    onValueChange = { viewModel.onSettingsChanged(settings.copy(highUrgencyInterval = it)) }
                )
                // ------------------------------------------

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Modo Sarcástico")
                    Switch(
                        checked = settings.sarcasticMode,
                        onCheckedChange = { viewModel.onSettingsChanged(settings.copy(sarcasticMode = it)) }
                    )
                }
            }

            if (onSetupComplete != null) {
                Button(
                    onClick = onSetupComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Guardar y Finalizar Configuración")
                }
            }
        }
    }
}

@Composable
fun SettingsSlider(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Column {
        Text(text = "$label: $value min")
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 1f..60f,
            steps = 59
        )
    }
}