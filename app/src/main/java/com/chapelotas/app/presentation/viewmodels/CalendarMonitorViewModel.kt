package com.chapelotas.app.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chapelotas.app.data.database.ChapelotasDatabase
import com.chapelotas.app.data.database.entities.ConversationLog
import com.chapelotas.app.data.database.entities.EventResolutionStatus
import com.chapelotas.app.domain.entities.CalendarEvent
import com.chapelotas.app.domain.events.ChapelotasEvent
import com.chapelotas.app.domain.events.ChapelotasEventBus
import com.chapelotas.app.domain.repositories.AIRepository
import com.chapelotas.app.domain.repositories.CalendarRepository
import com.chapelotas.app.domain.repositories.PreferencesRepository
import com.chapelotas.app.domain.usecases.CalendarSyncUseCase
import com.chapelotas.app.domain.usecases.InitializeChatThreadsUseCase
import com.chapelotas.app.domain.usecases.MigrateToMonkeyAgendaUseCase
import com.chapelotas.app.domain.usecases.UnifiedMonkeyService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import com.chapelotas.app.data.database.entities.EventConflict
import com.chapelotas.app.data.database.entities.ScheduledNotification
import kotlinx.coroutines.flow.map
import com.chapelotas.app.data.database.entities.EventDistance
import com.chapelotas.app.data.database.entities.EventPlan

@HiltViewModel
class CalendarMonitorViewModel @Inject constructor(
    private val database: ChapelotasDatabase,
    private val eventBus: ChapelotasEventBus,
    private val calendarRepository: CalendarRepository,
    private val calendarSync: CalendarSyncUseCase,
    private val migrateToMonkeyAgendaUseCase: MigrateToMonkeyAgendaUseCase,
    private val initializeChatThreadsUseCase: InitializeChatThreadsUseCase,
    private val unifiedMonkey: UnifiedMonkeyService,
    private val preferencesRepository: PreferencesRepository,
    private val aiRepository: AIRepository
) : ViewModel() {

    companion object {
        private const val TAG = "Vigía-ViewModel"
    }

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private val _eventBusFlow = MutableStateFlow<ChapelotasEvent?>(null)
    val eventBusFlow: StateFlow<ChapelotasEvent?> = _eventBusFlow.asStateFlow()

    // Estados para la UI
    private val _todayEvents = MutableStateFlow<List<EventPlan>>(emptyList())
    val todayEvents: StateFlow<List<EventPlan>> = _todayEvents.asStateFlow()

    private val _activeConflicts = MutableStateFlow<List<EventConflict>>(emptyList())
    val activeConflicts: StateFlow<List<EventConflict>> = _activeConflicts.asStateFlow()

    private val _activeNotifications = MutableStateFlow<List<ScheduledNotification>>(emptyList())
    val activeNotifications: StateFlow<List<ScheduledNotification>> = _activeNotifications.asStateFlow()

    private val _nextCheckTime = MutableStateFlow<LocalDateTime?>(null)
    val nextCheckTime: StateFlow<LocalDateTime?> = _nextCheckTime.asStateFlow()


    init {
        observarEventos()
        observarCalendario()
        observarDatos()
    }

    fun iniciarDia() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🚀 Iniciando día...")

                val today = LocalDate.now().toString()

                // Detectar el tipo de inicio
                val lastRun = preferencesRepository.getLastSuccessfulRun()
                val todayInitialized = preferencesRepository.isTodayInitialized(today)
                val alarmsConfigured = preferencesRepository.areAlarmsConfigured()

                val startupType = when {
                    lastRun == null -> "FIRST_INSTALL"
                    !alarmsConfigured -> "ALARMS_LOST"
                    todayInitialized && _isMonitoring.value -> "ALREADY_RUNNING"
                    !todayInitialized -> "NEW_DAY"
                    else -> "RESUME"
                }

                Log.d(TAG, """
                    📊 Tipo de inicio: $startupType
                    - Última ejecución: ${lastRun?.let { "${(System.currentTimeMillis() - it) / 1000 / 60} minutos atrás" } ?: "NUNCA"}
                    - Hoy inicializado: $todayInitialized
                    - Alarmas configuradas: $alarmsConfigured
                    - Monitoring activo: ${_isMonitoring.value}
                """.trimIndent())

                when (startupType) {
                    "ALREADY_RUNNING" -> {
                        Log.d(TAG, "✅ Ya está todo funcionando")
                        return@launch
                    }

                    "FIRST_INSTALL" -> {
                        Log.d(TAG, "🎉 Primera instalación detectada")
                        initializeFirstTime()
                    }

                    "ALARMS_LOST" -> {
                        Log.d(TAG, "⚠️ Alarmas perdidas (force stop/reboot)")
                        showRecoveryNotification()
                        reconfigureEverything()
                    }

                    "NEW_DAY" -> {
                        Log.d(TAG, "🌅 Nuevo día")
                        initializeNewDay()
                    }

                    "RESUME" -> {
                        Log.d(TAG, "▶️ Resumiendo operaciones")
                        resumeOperations()
                    }
                }

                // Marcar como inicializado
                preferencesRepository.setTodayInitialized(today)
                preferencesRepository.setLastSuccessfulRun(System.currentTimeMillis())
                preferencesRepository.setAlarmsConfigured(true)

                _isMonitoring.value = true

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al iniciar", e)
                eventBus.tryEmit(ChapelotasEvent.MonkeyError(
                    error = e.message ?: "Error desconocido",
                    willRetry = true,
                    retryInSeconds = 60
                ))
            }
        }
    }

    private suspend fun initializeFirstTime() {
        database.dayPlanDao().getOrCreateTodayPlan()
        initializeChatThreadsUseCase()
        calendarSync.syncTodayEvents()

        // Programar notificaciones para eventos
        programarNotificacionesDelDia()

        showWelcomeMessage()
        unifiedMonkey.scheduleNextAlarm()
    }

    private suspend fun reconfigureEverything() {
        database.dayPlanDao().getOrCreateTodayPlan()
        migrateToMonkeyAgendaUseCase()
        calendarSync.syncTodayEvents()

        // Programar notificaciones para eventos
        programarNotificacionesDelDia()

        unifiedMonkey.scheduleNextAlarm()
    }
    private fun observarDatos() {
        // Observar eventos del día
        viewModelScope.launch {
            database.eventPlanDao().observeTodayEvents()
                .collect { events ->
                    _todayEvents.value = events
                }
        }

        // Observar conflictos activos
        viewModelScope.launch {
            database.conflictDao().observeActiveConflicts()
                .collect { conflicts ->
                    _activeConflicts.value = conflicts
                }
        }

        // Observar notificaciones activas
        viewModelScope.launch {
            database.notificationDao().observeActiveNotifications()
                .collect { notifications ->
                    _activeNotifications.value = notifications
                }
        }
    }
    fun actualizarCriticidad(eventId: String, isCritical: Boolean) {
        viewModelScope.launch {
            database.eventPlanDao().updateCritical(eventId, isCritical)
            Log.d(TAG, "Criticidad actualizada: $eventId = $isCritical")
        }
    }

    fun actualizarDistancia(eventId: String, distanceName: String) {
        viewModelScope.launch {
            try {
                val distance = EventDistance.valueOf(distanceName)
                database.eventPlanDao().updateDistance(eventId, distance)
                Log.d(TAG, "Distancia actualizada: $eventId = $distance")
            } catch (e: Exception) {
                Log.e(TAG, "Error actualizando distancia", e)
            }
        }
    }

    fun revivirEvento(eventId: String) {
        viewModelScope.launch {
            database.eventPlanDao().updateResolutionStatus(
                eventId,
                EventResolutionStatus.PENDING
            )
            Log.d(TAG, "Evento revivido: $eventId")
        }
    }



    private suspend fun initializeNewDay() {
        database.dayPlanDao().getOrCreateTodayPlan()
        migrateToMonkeyAgendaUseCase()
        initializeChatThreadsUseCase()
        calendarSync.syncTodayEvents()

        // Programar notificaciones para eventos
        programarNotificacionesDelDia()

        refreshEventStates()
        unifiedMonkey.scheduleNextAlarm()

        // Mostrar resumen del día
        showDailySummary()
    }

    private suspend fun resumeOperations() {
        val nextAlarm = database.dayPlanDao().getTodayPlan()?.nextAlarmTime

        if (nextAlarm == null || nextAlarm.isBefore(LocalDateTime.now(ZoneId.systemDefault()))) {
            unifiedMonkey.scheduleNextAlarm()
        }
    }

    private suspend fun programarNotificacionesDelDia() {
        val todayEvents = database.eventPlanDao().getEventsByDate(LocalDate.now())
        todayEvents.forEach { event ->
            if (event.resolutionStatus != EventResolutionStatus.COMPLETED) {
                unifiedMonkey.planNotificationsForEvent(event)
                Log.d(TAG, "Notificaciones programadas para: ${event.title}")
            }
        }
    }

    private suspend fun showRecoveryNotification() {
        val message = "🔧 Chapelotas se recuperó de un cierre forzado. Re-configurando alarmas..."

        database.conversationLogDao().insert(
            ConversationLog(
                timestamp = LocalDateTime.now(ZoneId.systemDefault()),
                role = "assistant",
                content = message,
                threadId = "general"
            )
        )
    }

    private suspend fun showWelcomeMessage() {
        val welcomeMessage = "👋 ¡Hola! Soy Chapelotas, tu secretaria personal. Estoy lista para ayudarte a gestionar tu día."

        database.conversationLogDao().insert(
            ConversationLog(
                timestamp = LocalDateTime.now(ZoneId.systemDefault()),
                role = "assistant",
                content = welcomeMessage,
                threadId = "general"
            )
        )

        // Mostrar resumen del día también
        showDailySummary()
    }

    private suspend fun showDailySummary() {
        val todayEvents = database.eventPlanDao().getEventsByDate(LocalDate.now())

        // Convertir EventPlan a CalendarEvent
        val calendarEvents = todayEvents.map { eventPlan ->
            CalendarEvent(
                id = eventPlan.calendarEventId,
                title = eventPlan.title,
                description = eventPlan.description,
                startTime = eventPlan.startTime,
                endTime = eventPlan.endTime,
                location = eventPlan.location,
                isAllDay = eventPlan.isAllDay,
                calendarId = eventPlan.calendarId,
                calendarName = "",
                isCritical = eventPlan.isCritical
            )
        }

        val dayPlan = database.dayPlanDao().getTodayPlan()
        val dailySummary = aiRepository.generateDailySummary(
            calendarEvents,
            dayPlan?.sarcasticMode ?: false
        )

        database.conversationLogDao().insert(
            ConversationLog(
                timestamp = LocalDateTime.now(ZoneId.systemDefault()),
                role = "assistant",
                content = dailySummary,
                threadId = "general"
            )
        )
    }

    fun detenerMonitoreo() {
        viewModelScope.launch {
            Log.d(TAG, "Deteniendo monitoreo...")
            _isMonitoring.value = false
            // No cancelamos las alarmas programadas
        }
    }

    private fun observarEventos() {
        viewModelScope.launch {
            eventBus.events.collect { event ->
                _eventBusFlow.value = event
                when (event) {
                    is ChapelotasEvent.ServiceStarted -> {
                        Log.d(TAG, "✅ Servicio iniciado: ${event.serviceName}")
                    }
                    is ChapelotasEvent.CalendarSyncCompleted -> {
                        Log.d(TAG, "📅 Sincronización completada: ${event.newEvents} nuevos, ${event.updatedEvents} actualizados")
                    }
                    is ChapelotasEvent.MonkeyCheckCompleted -> {
                        Log.d(TAG, "🐵 Mono completó chequeo: ${event.notificationsShown} notificaciones")
                        _nextCheckTime.value = event.nextCheckTime
                    }
                    is ChapelotasEvent.MonkeyError -> {
                        Log.e(TAG, "❌ Error del mono: ${event.error}")
                    }
                    else -> {  // AGREGAR ESTA RAMA ELSE
                        Log.d(TAG, "📬 Evento: $event")
                    }
                }
            }
        }
    }

    private fun observarCalendario() {
        viewModelScope.launch {
            calendarRepository.observeCalendarChanges()
                .collect {
                    Log.d(TAG, "📅 Cambio detectado en el calendario")
                    calendarSync.syncTodayEvents()

                    // Programar notificaciones para eventos nuevos/actualizados
                    programarNotificacionesDelDia()

                    refreshEventStates()
                    unifiedMonkey.scheduleNextAlarm()
                }
        }
    }

    private suspend fun refreshEventStates() {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val pastEvents = database.eventPlanDao().getPastUnresolvedEvents(now)

        pastEvents.forEach { event ->
            database.eventPlanDao().updateResolutionStatus(
                event.eventId,
                EventResolutionStatus.MISSED_ACKNOWLEDGED
            )
            Log.d(TAG, "Evento '${event.title}' marcado como perdido")
        }

        if (pastEvents.isNotEmpty()) {
            Log.d(TAG, "Estados de eventos actualizados")
        }
    }
}