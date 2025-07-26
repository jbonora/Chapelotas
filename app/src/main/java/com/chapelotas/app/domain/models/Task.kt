package com.chapelotas.app.domain.models

import java.time.LocalDateTime

data class Task(
    val id: String,
    val title: String,
    val scheduledTime: LocalDateTime,
    val endTime: LocalDateTime? = null,
    val lastReminderAt: LocalDateTime? = null,
    val nextReminderAt: LocalDateTime? = null,
    val reminderCount: Int = 0,

    // --- LA VERDADERA FUENTE DE LA VERDAD (SIMPLIFICADA) ---
    val isAcknowledged: Boolean = false, // ¿El usuario presionó "Entendido"?
    val isFinished: Boolean = false      // ¿El usuario presionó "Done"?
)