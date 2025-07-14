package com.chapelotas.app.data.database.converters

import android.util.Log
import androidx.room.TypeConverter
import com.chapelotas.app.data.database.entities.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class Converters {

    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME // Para LocalTime

    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? {
        return dateTime?.format(dateTimeFormatter)
    }

    @TypeConverter
    fun toLocalDateTime(dateTimeString: String?): LocalDateTime? {
        return dateTimeString?.let {
            try { LocalDateTime.parse(it, dateTimeFormatter) }
            catch (e: DateTimeParseException) { null }
        }
    }

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.format(dateFormatter)
    }

    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? {
        return dateString?.let {
            try { LocalDate.parse(it, dateFormatter) }
            catch (e: DateTimeParseException) { null }
        }
    }

    // --- NUEVO CONVERSOR PARA LOCALTIME ---
    @TypeConverter
    fun fromLocalTime(time: LocalTime?): String? {
        return time?.format(timeFormatter)
    }

    @TypeConverter
    fun toLocalTime(timeString: String?): LocalTime? {
        return timeString?.let {
            try { LocalTime.parse(it, timeFormatter) }
            catch (e: DateTimeParseException) { null }
        }
    }

    // (El resto de los conversores de Enums sigue igual)
    @TypeConverter
    fun fromUserAction(action: UserAction?): String? = action?.name

    @TypeConverter
    fun toUserAction(name: String?): UserAction? = name?.let {
        try { UserAction.valueOf(it) } catch (e: IllegalArgumentException) { null }
    }

    @TypeConverter
    fun fromEventResolutionStatus(status: EventResolutionStatus?): String? = status?.name

    @TypeConverter
    fun toEventResolutionStatus(name: String?): EventResolutionStatus = name?.let {
        try { EventResolutionStatus.valueOf(it) } catch (e: IllegalArgumentException) { EventResolutionStatus.PENDING }
    } ?: EventResolutionStatus.PENDING

    @TypeConverter
    fun fromConflictSeverity(severity: ConflictSeverity?): String? = severity?.name

    @TypeConverter
    fun toConflictSeverity(name: String?): ConflictSeverity = name?.let {
        try { ConflictSeverity.valueOf(it) } catch (e: IllegalArgumentException) { ConflictSeverity.LOW }
    } ?: ConflictSeverity.LOW

    @TypeConverter
    fun fromEventDistance(distance: EventDistance?): String? = distance?.name

    @TypeConverter
    fun toEventDistance(name: String?): EventDistance = name?.let {
        EventDistance.entries.find { e -> e.name.equals(it, ignoreCase = true) } ?: EventDistance.CERCA
    } ?: EventDistance.CERCA

    @TypeConverter
    fun fromNotificationType(type: NotificationType?): String? = type?.name

    @TypeConverter
    fun toNotificationType(name: String?): NotificationType = name?.let {
        try { NotificationType.valueOf(it) } catch (e: IllegalArgumentException) { NotificationType.EVENT_REMINDER }
    } ?: NotificationType.EVENT_REMINDER

    @TypeConverter
    fun fromConflictType(type: ConflictType?): String? = type?.name

    @TypeConverter
    fun toConflictType(name: String?): ConflictType = name?.let {
        try { ConflictType.valueOf(it) } catch (e: IllegalArgumentException) { ConflictType.TOO_CLOSE }
    } ?: ConflictType.TOO_CLOSE

    @TypeConverter
    fun fromNotificationPriority(priority: NotificationPriority?): String? = priority?.name

    @TypeConverter
    fun toNotificationPriority(name: String?): NotificationPriority = name?.let {
        try { NotificationPriority.valueOf(it) } catch (e: IllegalArgumentException) { NotificationPriority.NORMAL }
    } ?: NotificationPriority.NORMAL
}