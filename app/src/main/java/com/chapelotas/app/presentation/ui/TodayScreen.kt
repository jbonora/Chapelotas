package com.chapelotas.app.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chapelotas.app.data.database.entities.ConversationLog
import com.chapelotas.app.presentation.viewmodels.MainViewModel
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(
    viewModel: MainViewModel
) {
    val conversationHistory: List<ConversationLog> by viewModel.conversationHistory.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Auto-scroll al último mensaje cuando cambia el historial
    LaunchedEffect(conversationHistory.size) {
        if (conversationHistory.isNotEmpty()) {
            listState.animateScrollToItem(conversationHistory.size - 1)
        }
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            reverseLayout = false,
            state = listState
        ) {
            items(conversationHistory) { log ->
                MessageCard(
                    log = log,
                    onMarkAsDone = {
                        scope.launch {
                            viewModel.handleEventAction(log.eventId ?: "", "done")
                        }
                    },
                    onSnooze = {
                        scope.launch {
                            viewModel.handleEventAction(log.eventId ?: "", "snooze")
                        }
                    },
                    onViewEvent = {
                        scope.launch {
                            viewModel.handleEventAction(log.eventId ?: "", "view")
                        }
                    }
                )
            }

            if (conversationHistory.isEmpty() && !uiState.isLoading) {
                item {
                    Column(
                        modifier = Modifier.fillParentMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Aún no hay nada que reportar.",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Chapelotas está vigilando y te avisará cuando haya algo que hacer. 🐵",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageCard(
    log: ConversationLog,
    onMarkAsDone: () -> Unit,
    onSnooze: () -> Unit,
    onViewEvent: () -> Unit
) {
    val cardAlignment = if (log.role == "user") Alignment.CenterEnd else Alignment.CenterStart
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // Detectar si es un mensaje de acción del usuario
    val isActionMessage = log.content.startsWith("Acción:")

    // Detectar si el mensaje de la AI menciona un evento
    val hasEventMention = log.eventId != null && log.role == "assistant" && !isActionMessage

    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .align(cardAlignment),
            colors = if (log.role == "user") {
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            } else {
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Encabezado con hora
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (log.role == "user") "Tú" else "🐵 Chapelotas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = log.timestamp.format(timeFormatter),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Contenido del mensaje
                Text(
                    text = log.content,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Botones de acción si es un mensaje sobre un evento
                if (hasEventMention) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botón Ver Evento
                        TextButton(
                            onClick = onViewEvent,
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = "Ver evento",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ver", style = MaterialTheme.typography.labelSmall)
                        }

                        // Botón Posponer
                        TextButton(
                            onClick = onSnooze,
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Posponer",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("En 15min", style = MaterialTheme.typography.labelSmall)
                        }

                        // Botón Listo
                        FilledTonalButton(
                            onClick = onMarkAsDone,
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Marcar como hecho",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Listo", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}