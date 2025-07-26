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
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
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

            // --- INICIO DE LA MODIFICACIÓN ---
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
                    enabled = !settings.workHours24h // Se deshabilita si el toggle 24h está activo
                )
                Spacer(modifier = Modifier.width(16.dp))
                OutlinedTextField(
                    value = settings.workEndTime,
                    onValueChange = { viewModel.onSettingsChanged(settings.copy(workEndTime = it)) },
                    label = { Text("Fin") },
                    modifier = Modifier.weight(1f),
                    enabled = !settings.workHours24h // Se deshabilita si el toggle 24h está activo
                )
            }
            // --- FIN DE LA MODIFICACIÓN ---

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

            Text("Intervalo de avisos (minutos)", style = MaterialTheme.typography.titleMedium)
            SettingsSlider(
                label = "Durante el evento",
                value = settings.ongoingInterval,
                onValueChange = { viewModel.onSettingsChanged(settings.copy(ongoingInterval = it)) }
            )
            SettingsSlider(
                label = "Evento perdido (missed)",
                value = settings.missedInterval,
                onValueChange = { viewModel.onSettingsChanged(settings.copy(missedInterval = it)) }
            )

            Divider()

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Modo AI (Premium)")
                Switch(
                    checked = settings.aiMode,
                    onCheckedChange = { /* No-op */ },
                    enabled = false
                )
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