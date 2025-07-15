package com.chapelotas.app.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chapelotas.app.data.database.entities.ChatThread
import com.chapelotas.app.presentation.viewmodels.ChatListViewModel
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel,
    onThreadClick: (String) -> Unit
) {
    val threads by viewModel.activeThreads.collectAsStateWithLifecycle()
    val unreadCount by viewModel.totalUnreadCount.collectAsStateWithLifecycle()

    Column {
        // TopBar estilo WhatsApp
        TopAppBar(
            title = {
                Text(
                    "Chapelotas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary
            ),
            actions = {
                if (unreadCount > 0) {
                    Badge(
                        modifier = Modifier.padding(end = 16.dp),
                        containerColor = MaterialTheme.colorScheme.error
                    ) {
                        Text(unreadCount.toString())
                    }
                }
            }
        )

        if (threads.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        "No hay conversaciones aún",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = threads,
                    key = { it.threadId }
                ) { thread ->
                    ChatThreadItem(
                        thread = thread,
                        onClick = { onThreadClick(thread.threadId) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatThreadItem(
    thread: ChatThread,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar con emoji según tipo
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    when (thread.threadType) {
                        "GENERAL" -> MaterialTheme.colorScheme.primaryContainer
                        "EVENT" -> if (thread.status == "COMPLETED")
                            MaterialTheme.colorScheme.surfaceVariant
                        else
                            MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.tertiaryContainer
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (thread.threadType) {
                    "GENERAL" -> "🐵"
                    "EVENT" -> when {
                        thread.status == "COMPLETED" -> "✅"
                        thread.eventTime?.isBefore(LocalDateTime.now(ZoneId.systemDefault())) == true -> "⏰"
                        else -> "📅"
                    }
                    "DAILY_SUMMARY" -> "📋"
                    else -> "💬"
                },
                fontSize = 24.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Contenido del chat
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Título
                Text(
                    text = thread.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Hora
                Text(
                    text = formatTime(thread.lastMessageTime),
                    fontSize = 12.sp,
                    color = if (thread.unreadCount > 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Último mensaje
                Text(
                    text = thread.lastMessage.ifEmpty { "Sin mensajes" },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Badge de no leídos
                if (thread.unreadCount > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = thread.unreadCount.toString(),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Indicador de estado para eventos
            if (thread.threadType == "EVENT" && thread.eventTime != null) {
                val now = LocalDateTime.now(ZoneId.systemDefault())
                val statusText = when {
                    thread.status == "COMPLETED" -> "Completado"
                    thread.eventTime.isBefore(now) -> "Evento pasado"
                    Duration.between(now, thread.eventTime).toHours() < 1 -> "¡Próximamente!"
                    else -> "En ${formatDuration(now, thread.eventTime)}"
                }

                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    color = when {
                        thread.status == "COMPLETED" -> MaterialTheme.colorScheme.primary
                        thread.eventTime.isBefore(now) -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.secondary
                    },
                    fontWeight = if (thread.eventTime.isBefore(now) && thread.status != "COMPLETED")
                        FontWeight.Bold
                    else
                        FontWeight.Normal
                )
            }
        }
    }
}

private fun formatTime(time: LocalDateTime): String {
    val now = LocalDateTime.now(ZoneId.systemDefault())
    val today = now.toLocalDate()
    val yesterday = today.minusDays(1)

    return when (time.toLocalDate()) {
        today -> time.format(DateTimeFormatter.ofPattern("HH:mm"))
        yesterday -> "Ayer"
        else -> time.format(DateTimeFormatter.ofPattern("dd/MM"))
    }
}

private fun formatDuration(from: LocalDateTime, to: LocalDateTime): String {
    val duration = Duration.between(from, to)
    return when {
        duration.toMinutes() < 60 -> "${duration.toMinutes()} min"
        duration.toHours() < 24 -> "${duration.toHours()}h"
        else -> "${duration.toDays()}d"
    }
}