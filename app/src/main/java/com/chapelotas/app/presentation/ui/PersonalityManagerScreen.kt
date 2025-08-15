package com.chapelotas.app.presentation.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chapelotas.app.presentation.viewmodels.PersonalityManagerViewModel
import com.chapelotas.app.presentation.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalityManagerScreen(
    navController: NavController,
    managerViewModel: PersonalityManagerViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val managerState by managerViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            managerViewModel.importPersonality(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestionar Personalidades") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = { /* TODO: Lógica de descarga futura */ },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Descargar nuevas")
                }
                Spacer(modifier = Modifier.height(8.dp))
                FloatingActionButton(
                    onClick = { filePickerLauncher.launch("application/json") }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Importar desde archivo")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(managerState.personalities.toList(), key = { it.first }) { (id, displayName) ->
                val isSelected = settingsState.settings.personalityProfile == id
                PersonalityListItem(
                    name = displayName,
                    isSelected = isSelected,
                    onSelected = {
                        val newSettings = settingsState.settings.copy(personalityProfile = id)
                        settingsViewModel.onSettingsChanged(newSettings)
                    }
                )
            }
        }
    }
}

@Composable
fun PersonalityListItem(
    name: String,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelected),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = name, style = MaterialTheme.typography.bodyLarge)
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Seleccionada",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}