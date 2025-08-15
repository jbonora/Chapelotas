package com.chapelotas.app.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chapelotas.app.R
import com.chapelotas.app.domain.models.InsistenceProfile
import com.chapelotas.app.presentation.viewmodels.SettingsViewModel
import com.chapelotas.app.presentation.viewmodels.SettingsUiState

/**
 * Un Composable reutilizable que muestra un ícono de información y abre un diálogo al hacer clic.
 */
@Composable
private fun InfoIconWithDialog(infoText: String) {
    var showDialog by remember { mutableStateOf(false) }

    Icon(
        imageVector = Icons.Outlined.Info,
        contentDescription = "Más información",
        modifier = Modifier
            .padding(start = 8.dp)
            .size(20.dp)
            .clickable { showDialog = true },
        tint = MaterialTheme.colorScheme.primary
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(id = R.string.settings_info_title)) },
            text = { Text(infoText) },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(id = R.string.settings_info_button_ok))
                }
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController? = null,
    onSetupComplete: (() -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings_title)) },
                navigationIcon = {
                    if (onSetupComplete == null) {
                        IconButton(onClick = { navController?.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.settings_back_button_description))
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
                    label = { Text(stringResource(id = R.string.settings_user_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { InfoIconWithDialog(stringResource(id = R.string.settings_user_name_info)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                PersonalitySelectorButton(
                    selectedPersonalityName = uiState.availablePersonalities[settings.personalityProfile] ?: "No seleccionada",
                    onClick = { navController?.navigate("personality_manager") }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text("Tiempos de Viaje", style = MaterialTheme.typography.titleMedium)
                SettingsSlider(
                    label = "Viaje a un lugar 'Cerca'",
                    value = settings.travelTimeNearbyMinutes,
                    onValueChange = { viewModel.onSettingsChanged(settings.copy(travelTimeNearbyMinutes = it)) },
                    range = 5f..60f,
                    steps = 11,
                    infoText = "Define cuántos minutos de viaje se deben reservar para un evento clasificado como 'cerca'."
                )
                SettingsSlider(
                    label = "Viaje a un lugar 'Lejos'",
                    value = settings.travelTimeFarMinutes,
                    onValueChange = { viewModel.onSettingsChanged(settings.copy(travelTimeFarMinutes = it)) },
                    range = 15f..180f,
                    steps = 33,
                    infoText = "Define cuántos minutos de viaje se deben reservar para un evento clasificado como 'lejos'."
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Horarios de Referencia", style = MaterialTheme.typography.titleMedium)
                    InfoIconWithDialog(stringResource(id = R.string.settings_work_hours_info))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Activar recordatorios 24h")
                        InfoIconWithDialog(stringResource(id = R.string.settings_24h_reminders_info))
                    }
                    Switch(
                        checked = settings.workHours24h,
                        onCheckedChange = { viewModel.onSettingsChanged(settings.copy(workHours24h = it)) }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    TimeTextField(
                        label = "Inicio Jornada",
                        value = settings.workStartTime,
                        onValueChange = { viewModel.onSettingsChanged(settings.copy(workStartTime = it)) }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    TimeTextField(
                        label = "Fin Jornada",
                        value = settings.workEndTime,
                        onValueChange = { viewModel.onSettingsChanged(settings.copy(workEndTime = it)) }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    TimeTextField(
                        label = "Inicio Almuerzo",
                        value = settings.lunchStartTime,
                        onValueChange = { viewModel.onSettingsChanged(settings.copy(lunchStartTime = it)) }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    TimeTextField(
                        label = "Fin Almuerzo",
                        value = settings.lunchEndTime,
                        onValueChange = { viewModel.onSettingsChanged(settings.copy(lunchEndTime = it)) }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    TimeTextField(
                        label = "Inicio Cena",
                        value = settings.dinnerStartTime,
                        onValueChange = { viewModel.onSettingsChanged(settings.copy(dinnerStartTime = it)) }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    TimeTextField(
                        label = "Fin Cena",
                        value = settings.dinnerEndTime,
                        onValueChange = { viewModel.onSettingsChanged(settings.copy(dinnerEndTime = it)) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Alarma Despertador Automática", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Crear alarma para el día siguiente")
                        InfoIconWithDialog(stringResource(id = R.string.settings_auto_alarm_info))
                    }
                    Switch(
                        checked = settings.autoCreateAlarm,
                        onCheckedChange = { viewModel.onSettingsChanged(settings.copy(autoCreateAlarm = it)) }
                    )
                }
                SettingsSlider(
                    label = "Antelación de la alarma",
                    value = settings.alarmOffsetMinutes,
                    onValueChange = { viewModel.onSettingsChanged(settings.copy(alarmOffsetMinutes = it)) },
                    range = 30f..180f,
                    steps = 29,
                    infoText = stringResource(id = R.string.settings_alarm_offset_info)
                )
                SettingsSlider(
                    label = "Duración del snooze",
                    value = settings.snoozeMinutes,
                    onValueChange = { viewModel.onSettingsChanged(settings.copy(snoozeMinutes = it)) },
                    range = 5f..30f,
                    steps = 4,
                    infoText = stringResource(id = R.string.settings_snooze_info)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(stringResource(id = R.string.settings_pre_event_reminders_section_title), style = MaterialTheme.typography.titleMedium)
                SettingsSlider(
                    label = stringResource(id = R.string.settings_first_reminder_label),
                    value = settings.firstReminder,
                    onValueChange = { viewModel.onSettingsChanged(settings.copy(firstReminder = it)) },
                    infoText = stringResource(id = R.string.settings_pre_event_reminders_info)
                )
                SettingsSlider(
                    label = stringResource(id = R.string.settings_second_reminder_label),
                    value = settings.secondReminder,
                    onValueChange = { viewModel.onSettingsChanged(settings.copy(secondReminder = it)) }
                )
                SettingsSlider(
                    label = stringResource(id = R.string.settings_third_reminder_label),
                    value = settings.thirdReminder,
                    onValueChange = { viewModel.onSettingsChanged(settings.copy(thirdReminder = it)) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(stringResource(id = R.string.settings_insistence_interval_section_title), style = MaterialTheme.typography.titleMedium)
                SettingsSlider(
                    label = stringResource(id = R.string.settings_low_urgency_interval_label),
                    value = settings.lowUrgencyInterval,
                    onValueChange = { viewModel.onSettingsChanged(settings.copy(lowUrgencyInterval = it)) },
                    infoText = stringResource(id = R.string.settings_insistence_interval_info)
                )
                SettingsSlider(
                    label = stringResource(id = R.string.settings_high_urgency_interval_label),
                    value = settings.highUrgencyInterval,
                    onValueChange = { viewModel.onSettingsChanged(settings.copy(highUrgencyInterval = it)) }
                )

                InsistenceProfileSelector(
                    selectedProfile = settings.insistenceSoundProfile,
                    onProfileSelected = { newProfile ->
                        viewModel.onSettingsChanged(settings.copy(insistenceSoundProfile = newProfile))
                    }
                )
            }
            if (onSetupComplete != null) {
                Button(
                    onClick = onSetupComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(stringResource(id = R.string.save_and_finish))
                }
            }
        }
    }
}

@Composable
fun PersonalitySelectorButton(
    selectedPersonalityName: String,
    onClick: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(id = R.string.settings_personality_section_title), style = MaterialTheme.typography.titleMedium)
            InfoIconWithDialog(infoText = stringResource(id = R.string.settings_personality_info))
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Personalidad activa:")
                Text(selectedPersonalityName, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RowScope.TimeTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    var text by remember(value) { mutableStateOf(value.filter { it.isDigit() }) }
    var isError by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            val filteredText = newText.filter { it.isDigit() }
            if (filteredText.length <= 4) {
                text = filteredText
                isError = false
                if (filteredText.length == 4) {
                    val hour = filteredText.substring(0, 2).toIntOrNull() ?: -1
                    val minute = filteredText.substring(2, 4).toIntOrNull() ?: -1
                    if (hour in 0..23 && minute in 0..59) {
                        onValueChange("${filteredText.substring(0, 2)}:${filteredText.substring(2, 4)}")
                    } else {
                        isError = true
                    }
                }
            }
        },
        label = { Text(label) },
        visualTransformation = TimeVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.weight(1f),
        singleLine = true,
        isError = isError
    )
}

private class TimeVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 4) text.text.substring(0..3) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 1) out += ":"
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 1) return offset
                if (offset <= 4) return offset + 1
                return 5
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 2) return offset
                if (offset <= 5) return offset - 1
                return 4
            }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsistenceProfileSelector(
    selectedProfile: String,
    onProfileSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val profiles = listOf(
        InsistenceProfile.NORMAL,
        InsistenceProfile.MEDIUM,
        InsistenceProfile.LOW
    )
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Comportamiento de Insistencia", style = MaterialTheme.typography.bodyLarge)
            InfoIconWithDialog(infoText = stringResource(id = R.string.settings_insistence_profile_info))
        }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedProfile,
                onValueChange = {},
                readOnly = true,
                label = { Text("Perfil de Sonido") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                profiles.forEach { profile ->
                    DropdownMenuItem(
                        text = { Text(profile) },
                        onClick = {
                            onProfileSelected(profile)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: ClosedFloatingPointRange<Float> = 1f..60f,
    steps: Int = 59,
    infoText: String? = null
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.settings_slider_minutes_label, label, value),
                modifier = Modifier.weight(1f)
            )
            infoText?.let { InfoIconWithDialog(infoText = it) }
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = range,
            steps = steps
        )
    }
}