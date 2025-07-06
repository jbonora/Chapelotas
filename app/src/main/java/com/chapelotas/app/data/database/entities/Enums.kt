package com.chapelotas.app.data.database.entities

/**
 * Enums compartidos para la base de datos
 */

enum class EventDistance(val displayName: String, val minutesBefore: Int) {
    EN_OFI("En la oficina", 10),
    CERCA("Cerca (caminando)", 30),
    LEJOS("Lejos (transporte)", 60);

    companion object {
        fun fromString(value: String): EventDistance {
            return when(value.lowercase()) {
                "en la ofi" -> EN_OFI
                "cerca" -> CERCA
                "lejos" -> LEJOS
                else -> CERCA
            }
        }
    }
}

enum class NotificationType {
    REMINDER,
    CRITICAL_ALERT,
    PREPARATION_TIP,
    CONFLICT_ALERT,
    DAILY_SUMMARY,
    TOMORROW_SUMMARY
}

enum class ConflictType {
    OVERLAPPING,    // Eventos que se superponen
    TOO_CLOSE,      // Eventos muy seguidos (sin tiempo de traslado)
    SAME_LOCATION   // Mismo lugar al mismo tiempo
}

enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
}