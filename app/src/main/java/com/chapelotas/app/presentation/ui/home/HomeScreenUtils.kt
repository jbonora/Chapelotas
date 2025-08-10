// presentation/ui/home/util/HomeScreenUtils.kt
package com.chapelotas.app.presentation.ui.home.util


import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.chapelotas.app.R
import com.chapelotas.app.domain.models.Task
import com.chapelotas.app.domain.models.TaskStatus
import java.time.Duration
import java.time.LocalDateTime


@Composable
fun TaskStatus.toColor(): Color {
    return when (this) {
        TaskStatus.FINISHED -> Color.Gray
        TaskStatus.DELAYED -> MaterialTheme.colorScheme.error
        TaskStatus.ONGOING -> MaterialTheme.colorScheme.primary
        TaskStatus.UPCOMING -> MaterialTheme.colorScheme.secondary
        TaskStatus.TODO, TaskStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary
    }
}

@Composable
fun getRelativeTimeText(task: Task): String {
    if (task.isFinished) return stringResource(id = R.string.home_task_status_finished)
    val now = LocalDateTime.now()
    return when (task.status) {
        TaskStatus.UPCOMING -> {
            val duration = Duration.between(now, task.scheduledTime)
            when {
                duration.toMinutes() < 1 -> "Ahora"
                duration.toHours() < 1 -> stringResource(id = R.string.home_task_status_upcoming_minutes, duration.toMinutes() + 1)
                duration.toDays() < 1 -> stringResource(id = R.string.home_task_status_upcoming_hours, duration.toHours(), duration.toMinutes() % 60)
                else -> stringResource(id = R.string.home_task_status_upcoming_days, duration.toDays(), duration.toHours() % 24)
            }
        }
        TaskStatus.ONGOING -> stringResource(id = R.string.home_task_status_ongoing)
        TaskStatus.DELAYED -> stringResource(id = R.string.home_task_status_delayed)
        else -> ""
    }
}