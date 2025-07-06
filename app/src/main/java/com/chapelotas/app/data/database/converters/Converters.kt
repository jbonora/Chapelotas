package com.chapelotas.app.data.database.converters

import androidx.room.TypeConverter
import com.chapelotas.app.data.database.entities.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Converters para que Room pueda guardar tipos custom
 */
class Converters {

    // LocalDateTime
    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? {
        return dateTime?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    @TypeConverter
    fun toLocalDateTime(dateTimeString: String?): LocalDateTime? {
        return dateTimeString?.let {
            LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        }
    }

    // LocalDate
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? {
        return dateString?.let {
            LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE)
        }
    }

    // EventDistance
    @TypeConverter
    fun fromEventDistance(distance: EventDistance): String {
        return distance.name
    }

    @TypeConverter
    fun toEventDistance(distanceString: String): EventDistance {
        return EventDistance.valueOf(distanceString)
    }

    // NotificationType
    @TypeConverter
    fun fromNotificationType(type: NotificationType): String {
        return type.name
    }

    @TypeConverter
    fun toNotificationType(typeString: String): NotificationType {
        return NotificationType.valueOf(typeString)
    }

    // NotificationPriority
    @TypeConverter
    fun fromNotificationPriority(priority: NotificationPriority): String {
        return priority.name
    }

    @TypeConverter
    fun toNotificationPriority(priorityString: String): NotificationPriority {
        return NotificationPriority.valueOf(priorityString)
    }

    // ConflictType
    @TypeConverter
    fun fromConflictType(type: ConflictType): String {
        return type.name
    }

    @TypeConverter
    fun toConflictType(typeString: String): ConflictType {
        return ConflictType.valueOf(typeString)
    }

    // ConflictSeverity
    @TypeConverter
    fun fromConflictSeverity(severity: ConflictSeverity): String {
        return severity.name
    }

    @TypeConverter
    fun toConflictSeverity(severityString: String): ConflictSeverity {
        return ConflictSeverity.valueOf(severityString)
    }

    // UserAction
    @TypeConverter
    fun fromUserAction(action: UserAction?): String? {
        return action?.name
    }

    @TypeConverter
    fun toUserAction(actionString: String?): UserAction? {
        return actionString?.let { UserAction.valueOf(it) }
    }
}