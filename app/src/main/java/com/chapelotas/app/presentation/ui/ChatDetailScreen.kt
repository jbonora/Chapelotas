package com.chapelotas.app.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chapelotas.app.data.database.entities.ConversationLog
import com.chapelotas.app.presentation.viewmodels.ChatDetailViewModel
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    viewModel: ChatDetailViewModel,
    onBackClick: () -> Unit
) {
    val thread by viewModel.thread.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val userInput by viewModel.userInput.collectAsStateWithLifecycle()
    val isTyping by viewModel.isTyping.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll cuando hay nuevos mensajes
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = thread?.title ?: "Chat",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (thread?.threadType == "EVENT" && thread?.eventTime != null) {
                            Text(
                                text = "Evento a las ${thread?.eventTime?.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Botones de acción rápida para eventos
                    if (thread?.threadType == "EVENT" && thread?.status != "COMPLETED") {
                        IconButton(onClick = { viewModel.markEventAsDone() }) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Marcar como hecho",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { viewModel.snoozeEvent() }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Posponer",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                value = userInput,
                onValueChange = viewModel::updateUserInput,
                onSend = viewModel::sendMessage,
                enabled = thread?.status != "COMPLETED" && thread?.status != "CANCELLED"
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFE5DDD5)) // Fondo estilo WhatsApp
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(message = message)
                }

                // Indicador de "escribiendo..."
                if (isTyping) {
                    item {
                        TypingIndicator()
                    }
                }

                // Espacio extra al final para que el último mensaje no quede tapado
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ConversationLog) {
    val isUser = message.role == "user"
    val isSystem = message.role == "system"
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser && !isSystem) {
            Spacer(modifier = Modifier.width(40.dp))
        }

        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    when {
                        isSystem -> MaterialTheme.colorScheme.tertiaryContainer
                        isUser -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surface
                    }
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = message.content,
                color = when {
                    isSystem -> MaterialTheme.colorScheme.onTertiaryContainer
                    isUser -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontSize = 15.sp
            )

            Text(
                text = message.timestamp.format(timeFormatter),
                color = when {
                    isSystem -> MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    isUser -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.End)
            )
        }

        if (isUser && !isSystem) {
            Spacer(modifier = Modifier.width(40.dp))
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Spacer(modifier = Modifier.width(40.dp))

        Card(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        if (enabled) "Escribe un mensaje" else "Chat cerrado",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                enabled = enabled,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { if (value.isNotBlank()) onSend() }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSend,
                enabled = enabled && value.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (enabled && value.isNotBlank())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar",
                    tint = if (enabled && value.isNotBlank())
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}