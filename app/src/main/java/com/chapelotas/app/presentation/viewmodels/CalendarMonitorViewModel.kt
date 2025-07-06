package com.chapelotas.app.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chapelotas.app.data.database.ChapelotasDatabase
import com.chapelotas.app.data.database.entities.*
import com.chapelotas.app.domain.entities.CalendarEvent
import com.chapelotas.app.domain.events.ChapelotasEvent
import com.chapelotas.app.domain.events.ChapelotasEventBus
import com.chapelotas.app.domain.events.filterEvent
import com.chapelotas.app.domain.repositories.CalendarRepository
import com.chapelotas.app.domain.usecases.MasterPlanController
import com.chapelotas.app.domain.usecases.UnifiedMonkeyService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * ViewModel para monitorear el calendario
 * ACTUALIZADO para usar Room en lugar de JSON
 */
@HiltViewModel
class CalendarMonitorViewModel @Inject constructor(
    private val database: ChapelotasDatabase,
    private val calendarRepository: CalendarRepository,
    private val masterPlanController: MasterPlanController,
    private val unifiedMonkey: UnifiedMonkeyService,
    private val eventBus: ChapelotasEventBus
) : ViewModel() {

    // Estado del plan del día (desde Room)
    val currentDayPlan: StateFlow<DayPlan?> = database.dayPlanDao()
        .observeTodayPlan()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Eventos de hoy (desde Room)
    val todayEvents: StateFlow<List<EventPlan>> = database.eventPlanDao()
        .observeTodayEvents()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Conflictos activos
    val activeConflicts: StateFlow<List<EventConflict>> = database.conflictDao()
        .observeActiveConflicts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Estado del mono
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    // Próximo check time
    private val _nextCheckTime = MutableStateFlow<LocalDateTime?>(null)
    val nextCheckTime: StateFlow<LocalDateTime?> = _nextCheckTime.asStateFlow()

    init {
        // Observar eventos del sistema
        observeSystemEvents()

        // Iniciar el día automáticamente después de un delay
        viewModelScope.launch {
            delay(2000) // Dar tiempo a que todo se inicialice
            if (!_isMonitoring.value) {
                iniciarDia()
            }
        }

        // Observar cambios en el calendario
        observarCalendario()
    }

    /**
     * Iniciar el día - Sincronizar calendario con Room y activar el mono
     */
    fun iniciarDia() {
        if (_isMonitoring.value) {
            Log.d("CalendarMonitor", "El mono ya está activo")
            return
        }

        viewModelScope.launch {
            try {
                Log.d("CalendarMonitor", "🌅 Iniciando el día...")

                // 1. Asegurar que existe el plan del día
                val dayPlan = database.dayPlanDao().getOrCreateTodayPlan(
                    userId = "user123", // TODO: Obtener de preferencias
                    userName = "Usuario" // TODO: Obtener de preferencias
                )

                // 2. Obtener eventos del calendario
                val calendarEvents = calendarRepository.getTodayEvents()
                Log.d("CalendarMonitor", "📅 Eventos del calendario: ${calendarEvents.size}")

                // 3. Sincronizar con Room
                syncCalendarWithRoom(calendarEvents, dayPlan)

                // 4. Análisis inicial con IA
                if (calendarEvents.isNotEmpty()) {
                    performInitialAIAnalysis(dayPlan)
                }

                // 5. Iniciar el mono
                unifiedMonkey.startMonkey()
                _isMonitoring.value = true

                Log.d("CalendarMonitor", "✅ Día iniciado exitosamente")

            } catch (e: Exception) {
                Log.e("CalendarMonitor", "Error iniciando el día", e)
                // Reintentar en 5 segundos
                delay(5000)
                iniciarDia()
            }
        }
    }

    /**
     * Sincronizar eventos del calendario con Room
     */
    private suspend fun syncCalendarWithRoom(
        calendarEvents: List<CalendarEvent>,
        dayPlan: DayPlan
    ) {
        database.withTransaction {
            // Obtener eventos existentes
            val existingEvents = database.eventPlanDao().getEventsByDate(dayPlan.date)
            val existingIds = existingEvents.map { it.calendarEventId }.toSet()

            // Agregar eventos nuevos
            val newEvents = calendarEvents.filter { it.id !in existingIds }

            for (event in newEvents) {
                val eventPlan = EventPlan(
                    eventId = "${event.id}_${dayPlan.date}",
                    dayDate = dayPlan.date,
                    calendarId = event.calendarId,
                    calendarEventId = event.id,
                    title = event.title,
                    description = event.description,
                    startTime = event.startTime,
                    endTime = event.endTime,
                    location = event.location,
                    isAllDay = event.isAllDay,
                    isCritical = event.isCritical,
                    distance = EventDistance.CERCA // Default
                )

                database.eventPlanDao().insert(eventPlan)

                // Crear notificaciones default
                createDefaultNotifications(eventPlan)

                // Emitir evento
                eventBus.emit(ChapelotasEvent.NewEventDetected(
                    eventId = eventPlan.eventId,
                    title = eventPlan.title,
                    startTime = eventPlan.startTime
                ))
            }

            // Marcar eventos eliminados
            val currentIds = calendarEvents.map { it.id }.toSet()
            val deletedEvents = existingEvents.filter { it.calendarEventId !in currentIds }

            for (event in deletedEvents) {
                database.eventPlanDao().deleteById(event.eventId)
                database.notificationDao().deleteByEventId(event.eventId)

                eventBus.emit(ChapelotasEvent.EventDeleted(
                    eventId = event.eventId,
                    title = event.title
                ))
            }
        }
    }

    /**
     * Crear notificaciones default para un evento
     */
    private suspend fun createDefaultNotifications(event: EventPlan) {
        val notificationMinutes = event.getNotificationMinutesList()

        val notifications = notificationMinutes.mapIndexed { index, minutes ->
            ScheduledNotification(
                eventId = event.eventId,
                scheduledTime = event.startTime.minusMinutes(minutes.toLong()),
                minutesBefore = minutes,
                type = if (event.isCritical && minutes <= 15)
                    NotificationType.CRITICAL_ALERT
                else
                    NotificationType.REMINDER,
                priority = when {
                    event.isCritical -> NotificationPriority.HIGH
                    index == 0 -> NotificationPriority.NORMAL // Primera notificación
                    else -> NotificationPriority.LOW
                }
            )
        }.filter {
            // Solo crear notificaciones futuras
            it.scheduledTime.isAfter(LocalDateTime.now())
        }

        database.notificationDao().insertAll(notifications)
    }

    /**
     * Análisis inicial con IA
     */
    private suspend fun performInitialAIAnalysis(dayPlan: DayPlan) {
        try {
            eventBus.emit(ChapelotasEvent.AIAnalysisStarted("daily"))

            // Aquí podríamos usar la IA para:
            // 1. Sugerir eventos críticos
            // 2. Detectar conflictos
            // 3. Optimizar tiempos de notificación

            // Por ahora solo actualizamos el timestamp
            database.dayPlanDao().updateLastAiAnalysis(dayPlan.date)

            eventBus.emit(ChapelotasEvent.AIAnalysisCompleted(
                type = "daily",
                success = true,
                itemsProcessed = todayEvents.value.size
            ))

        } catch (e: Exception) {
            Log.e("CalendarMonitor", "Error en análisis IA", e)
            eventBus.emit(ChapelotasEvent.AIAnalysisCompleted(
                type = "daily",
                success = false,
                itemsProcessed = 0
            ))
        }
    }

    /**
     * Usuario actualiza criticidad/distancia de un evento
     */
    fun actualizarEvento(eventoId: String, esCritico: Boolean, distancia: String) {
        viewModelScope.launch {
            try {
                val eventDistance = EventDistance.fromString(distancia)

                // Actualizar en Room
                database.eventPlanDao().updateCritical(eventoId, esCritico)
                database.eventPlanDao().updateDistance(eventoId, eventDistance)

                // Actualizar notificaciones basadas en la nueva distancia
                database.withTransaction {
                    // Eliminar notificaciones existentes no ejecutadas
                    val existingNotifications = database.notificationDao()
                        .getActiveNotificationsByEvent(eventoId)

                    existingNotifications.forEach {
                        database.notificationDao().delete(it)
                    }

                    // Crear nuevas notificaciones con los tiempos actualizados
                    val event = database.eventPlanDao().getEvent(eventoId) ?: return@withTransaction
                    createDefaultNotifications(event.copy(
                        isCritical = esCritico,
                        distance = eventDistance
                    ))
                }

                // Emitir eventos
                eventBus.emit(ChapelotasEvent.EventMarkedCritical(
                    eventId = eventoId,
                    isCritical = esCritico
                ))

                eventBus.emit(ChapelotasEvent.DistanceUpdated(
                    eventId = eventoId,
                    distance = eventDistance,
                    previousDistance = EventDistance.CERCA
                ))

                Log.d("CalendarMonitor", "✅ Evento actualizado: $eventoId")

            } catch (e: Exception) {
                Log.e("CalendarMonitor", "Error actualizando evento", e)
            }
        }
    }

    /**
     * Observar cambios en calendario
     */
    private fun observarCalendario() {
        viewModelScope.launch {
            calendarRepository.observeCalendarChanges().collect {
                Log.d("CalendarMonitor", "📅 Cambio detectado en calendario")

                // Re-sincronizar si estamos monitoreando
                if (_isMonitoring.value) {
                    currentDayPlan.value?.let { plan ->
                        val events = calendarRepository.getTodayEvents()
                        syncCalendarWithRoom(events, plan)
                    }
                }
            }
        }
    }

    /**
     * Observar eventos del sistema
     */
    private fun observeSystemEvents() {
        viewModelScope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is ChapelotasEvent.MonkeyCheckCompleted -> {
                        _nextCheckTime.value = event.nextCheckTime
                    }

                    is ChapelotasEvent.ConflictDetected -> {
                        // Los conflictos ya se muestran via Flow de Room
                        Log.d("CalendarMonitor", "⚠️ Conflicto detectado: ${event.conflictId}")
                    }

                    is ChapelotasEvent.NotificationShown -> {
                        Log.d("CalendarMonitor", "🔔 Notificación mostrada: ${event.eventId}")
                    }

                    else -> {
                        // Otros eventos
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // NO detener el mono - debe seguir corriendo en el servicio
        Log.d("CalendarMonitor", "ViewModel limpiado pero el mono sigue activo")
    }
}