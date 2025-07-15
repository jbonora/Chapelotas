package com.chapelotas.app.domain.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter // Import the filter operator
import kotlinx.coroutines.flow.map    // Import the map operator
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Event Bus centralizado para comunicación entre componentes
 * Resuelve el problema de comunicación desconectada
 */
@Singleton
class ChapelotasEventBus @Inject constructor() {

    private val _events = MutableSharedFlow<ChapelotasEvent>(
        replay = 0,  // No replay eventos viejos
        extraBufferCapacity = 64  // Buffer para eventos rápidos
    )
    val events: SharedFlow<ChapelotasEvent> = _events.asSharedFlow()

    /**
     * Emite un evento a todos los suscriptores
     */
    suspend fun emit(event: ChapelotasEvent) {
        _events.emit(event)
    }

    /**
     * Intenta emitir un evento sin suspender (fire and forget)
     */
    fun tryEmit(event: ChapelotasEvent): Boolean {
        return _events.tryEmit(event)
    }
}

/**
 * Eventos del sistema Chapelotas
 * Todos los componentes pueden escuchar estos eventos
 */
sealed class ChapelotasEvent {
    val timestamp: LocalDateTime = LocalDateTime.now(ZoneId.systemDefault())

    // ===== EVENTOS DEL CALENDARIO =====
    data class NewEventDetected(
        val eventId: String,
        val title: String,
        val startTime: LocalDateTime
    ) : ChapelotasEvent()
    data class CalendarSyncCompleted(
        val newEvents: Int,
        val updatedEvents: Int,
        val deletedEvents: Int
    ) : ChapelotasEvent()
    data class EventResolved(
        val eventId: String,
        val resolution: com.chapelotas.app.data.database.entities.EventResolutionStatus
    ) : ChapelotasEvent()

    data class EventUpdated(
        val eventId: String,
        val field: String,  // "title", "time", "location", etc.
        val oldValue: Any?,
        val newValue: Any?
    ) : ChapelotasEvent()

    data class EventDeleted(
        val eventId: String,
        val title: String
    ) : ChapelotasEvent()

    // ===== EVENTOS DE NOTIFICACIONES =====
    data class NotificationScheduled(
        val notificationId: Long,
        val eventId: String,
        val scheduledTime: LocalDateTime,
        val type: com.chapelotas.app.data.database.entities.NotificationType
    ) : ChapelotasEvent()

    data class NotificationShown(
        val notificationId: Long,
        val eventId: String,
        val message: String
    ) : ChapelotasEvent()

    data class NotificationSnoozed(
        val notificationId: Long,
        val eventId: String,
        val snoozeMinutes: Int,
        val newTime: LocalDateTime
    ) : ChapelotasEvent()

    data class NotificationDismissed(
        val notificationId: Long,
        val eventId: String,
        val wasEffective: Boolean
    ) : ChapelotasEvent()

    data class NotificationActionTaken(
        val notificationId: Long,
        val action: com.chapelotas.app.data.database.entities.UserAction,
        val responseTimeSeconds: Long
    ) : ChapelotasEvent()

    // ===== EVENTOS DEL MONO =====
    data class MonkeyCheckStarted(
        val checkNumber: Long
    ) : ChapelotasEvent()

    data class MonkeyCheckCompleted(
        val checkNumber: Long,
        val notificationsShown: Int,
        val nextCheckTime: LocalDateTime
    ) : ChapelotasEvent()

    data class MonkeyError(
        val error: String,
        val willRetry: Boolean,
        val retryInSeconds: Int
    ) : ChapelotasEvent()

    // ===== EVENTOS DE CONFLICTOS =====
    data class ConflictDetected(
        val conflictId: Long,
        val event1Id: String,
        val event2Id: String,
        val type: com.chapelotas.app.data.database.entities.ConflictType,
        val severity: com.chapelotas.app.data.database.entities.ConflictSeverity
    ) : ChapelotasEvent()

    data class ConflictResolved(
        val conflictId: Long,
        val resolution: String
    ) : ChapelotasEvent()

    // ===== EVENTOS DE USUARIO =====
    data class EventMarkedCritical(
        val eventId: String,
        val isCritical: Boolean,
        val reason: String? = null
    ) : ChapelotasEvent()

    data class DistanceUpdated(
        val eventId: String,
        val distance: com.chapelotas.app.data.database.entities.EventDistance,
        val previousDistance: com.chapelotas.app.data.database.entities.EventDistance
    ) : ChapelotasEvent()

    data class SarcasticModeToggled(
        val enabled: Boolean
    ) : ChapelotasEvent()

    // ===== EVENTOS DE IA =====
    data class AIAnalysisStarted(
        val type: String  // "daily", "new_event", "conflict"
    ) : ChapelotasEvent()

    data class AIAnalysisCompleted(
        val type: String,
        val success: Boolean,
        val itemsProcessed: Int
    ) : ChapelotasEvent()

    data class AIMessageGenerated(
        val eventId: String,
        val messageType: String,
        val message: String
    ) : ChapelotasEvent()

    // ===== EVENTOS DEL SISTEMA =====
    data class ServiceStarted(
        val serviceName: String
    ) : ChapelotasEvent()

    data class ServiceStopped(
        val serviceName: String,
        val reason: String
    ) : ChapelotasEvent()

    data class BatteryOptimizationDetected(
        val isOptimized: Boolean
    ) : ChapelotasEvent()

    data class PermissionGranted(
        val permission: String
    ) : ChapelotasEvent()

    data class PermissionDenied(
        val permission: String
    ) : ChapelotasEvent()

    // ===== EVENTOS DE RESÚMENES =====
    data class DailySummaryRequested(
        val isAutomatic: Boolean
    ) : ChapelotasEvent()

    data class DailySummaryGenerated(
        val summary: String,
        val eventsCount: Int,
        val criticalCount: Int
    ) : ChapelotasEvent()

    data class TomorrowSummaryRequested(
        val isAutomatic: Boolean
    ) : ChapelotasEvent()

    data class TomorrowSummaryGenerated(
        val summary: String,
        val eventsCount: Int,
        val firstEventTime: LocalDateTime?
    ) : ChapelotasEvent()
    // ===== EVENTOS DE NAVEGACIÓN =====
    data class NavigateToPlan(
        val eventId: String? = null
    ) : ChapelotasEvent()
}

/**
 * Extension functions para facilitar el filtrado de eventos
 */
inline fun <reified T : ChapelotasEvent> SharedFlow<ChapelotasEvent>.filterEvent(): kotlinx.coroutines.flow.Flow<T> {
    return this
        .filter { it is T } // Now correctly resolves to kotlinx.coroutines.flow.filter
        .map { it as T }    // Now correctly resolves to kotlinx.coroutines.flow.map
}

/**
 * Extension para logging automático de eventos
 */
fun ChapelotasEvent.toLogString(): String {
    return "${this::class.simpleName} at ${timestamp.format(java.time.format.DateTimeFormatter.ISO_LOCAL_TIME)}: $this"
}
