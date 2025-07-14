package com.chapelotas.app.data.database.entities

enum class UserAction {
    OPENED,
    DISMISSED,
    SNOOZED,
    MARKED_DONE,
    IGNORED
}

enum class EventResolutionStatus {
    PENDING,
    COMPLETED,
    MISSED_ACKNOWLEDGED
}

enum class ConflictSeverity {
    LOW,
    MEDIUM,
    HIGH
}

enum class EventDistance(val displayName: String, val minutesBefore: Int) {
    EN_OFI("En la oficina", 10),
    CERCA("Cerca (caminando)", 30),
    LEJOS("Lejos (transporte)", 60);

    companion object {
        fun fromString(value: String?): EventDistance {
            if (value == null) return CERCA
            return entries.find { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: CERCA
        }
    }
}

enum class NotificationType {
    EVENT_REMINDER,
    CRITICAL_ALERT,
    PREPARATION_TIP,
    CONFLICT_ALERT,
    DAILY_SUMMARY,
    TOMORROW_SUMMARY
}

enum class ConflictType {
    OVERLAPPING,
    TOO_CLOSE,
    SAME_LOCATION
}

enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
}