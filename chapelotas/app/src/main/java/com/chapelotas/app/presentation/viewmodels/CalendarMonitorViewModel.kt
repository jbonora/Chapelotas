package com.chapelotas.app.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.chapelotas.app.data.database.ChapelotasDatabase
import com.chapelotas.app.data.database.entities.DayPlan
import com.chapelotas.app.data.database.entities.EventConflict
import com.chapelotas.app.data.database.entities.EventResolutionStatus
import com.chapelotas.app.data.database.entities.ConversationLog
import com.chapelotas.app.data.database.entities.EventDistance
import com.chapelotas.app.data.database.entities.EventPlan
import com.chapelotas.app.data.database.entities.ScheduledNotification
import com.chapelotas.app.domain.entities.CalendarEvent
import com.chapelotas.app.domain.events.ChapelotasEvent
import com.chapelotas.app.domain.events.ChapelotasEventBus
import com.chapelotas.app.domain.repositories.CalendarRepository
import com.chapelotas.app.domain.usecases.ProcessAIPlansUseCase
import com.chapelotas.app.domain.usecases.UnifiedMonkeyService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import com.chapelotas.app.domain.usecases.CalendarSyncUseCase
import com.chapelotas.app.domain.usecases.MigrateToMonkeyAgendaUseCase
import com.chapelotas.app.domain.usecases.InitializeChatThreadsUseCase


@HiltViewModel
class CalendarMonitorViewModel @Inject constructor(
    private val database: ChapelotasDatabase,
    private val calendarRepository: CalendarRepository,
    private val unifiedMonkey: UnifiedMonkeyService,
    private val processAIPlansUseCase: ProcessAIPlansUseCase,
    private val eventBus: ChapelotasEventBus,
    private val calendarSync: CalendarSyncUseCase,
    private val migrateToMonkeyAgendaUseCase: MigrateToMonkeyAgendaUseCase, // ← Faltaba
    private val initializeChatThreadsUseCase: InitializeChatThreadsUseCase   // ← Faltaba

    ) : ViewModel() {

    val currentDayPlan: StateFlow<DayPlan?> = database.dayPlanDao().observeTodayPlan()
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = null)

    val todayEvents: StateFlow<List<EventPlan>> = database.eventPlanDao().observeTodayEvents()
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    val activeConflicts: StateFlow<List<EventConflict>> = database.conflictDao().observeActiveConflicts()
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    val activeNotifications: StateFlow<List<ScheduledNotification>> = database.notificationDao().observeActiveNotifications()
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    val nextCheckTime: StateFlow<LocalDateTime?> = currentDayPlan.map { it?.nextAlarmTime }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = null)

    companion object {
        const val TAG = "Vigía-ViewModel"
    }

    init {
        observeSystemEvents()
    }

    fun iniciarDia() {
        if (_isMonitoring.value) {
            Log.d(TAG, "El día ya está iniciado")
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "Iniciando día...")

                // Crear plan del día si no existe
                database.dayPlanDao().getOrCreateTodayPlan()

                // NUEVO: Migrar al sistema MonkeyAgenda
                Log.d(TAG, "Migrando a MonkeyAgenda...")
                val migrationSuccess = migrateToMonkeyAgendaUseCase()
                if (!migrationSuccess) {
                    Log.e(TAG, "Error en migración a MonkeyAgenda")
                }

                // NUEVO: Inicializar chat threads
                Log.d(TAG, "Inicializando chat threads...")
                initializeChatThreadsUseCase()

                // Sincronizar eventos del calendario
                Log.d(TAG, "Sincronizando calendario...")
                calendarSync.syncTodayEvents()

                // Actualizar estados
                refreshEventStates()

                // Procesar planes con IA
                Log.d(TAG, "Procesando planes con IA...")
                processAIPlansUseCase()

                // Programar el mono
                Log.d(TAG, "Programando el mono unificado...")
                unifiedMonkey.scheduleNextAlarm()

                _isMonitoring.value = true
                eventBus.tryEmit(ChapelotasEvent.ServiceStarted("CalendarMonitor"))

                Log.d(TAG, "✅ Día iniciado correctamente con nuevo sistema MonkeyAgenda")

            } catch (e: Exception) {
                Log.e(TAG, "Error al iniciar el día", e)
                eventBus.tryEmit(ChapelotasEvent.MonkeyError(
                    error = e.message ?: "Error desconocido",
                    willRetry = true,
                    retryInSeconds = 60
                ))
            }
        }
    }

    private suspend fun syncCalendarWithRoom(calendarEvents: List<CalendarEvent>) {
        val today = LocalDate.now()
        Log.d(TAG, "syncCalendarWithRoom: Sincronizando ${calendarEvents.size} eventos.")

        database.withTransaction {
            val currentEventIds = calendarEvents.map { "${it.id}_$today" }.toSet()
            val existingPlans = database.eventPlanDao().getEventsByDate(today)

            // Eliminar eventos que ya no existen en el calendario
            existingPlans.filterNot { it.eventId in currentEventIds }.forEach {
                database.eventPlanDao().deleteById(it.eventId)
            }

            // Lista para acumular nuevos eventos
            val newEvents = mutableListOf<CalendarEvent>()

            // Insertar o actualizar eventos
            for (event in calendarEvents) {
                val eventIdWithDate = "${event.id}_$today"
                var existingPlan = existingPlans.find { it.eventId == eventIdWithDate }

                if (existingPlan == null) {
                    // Evento nuevo - crear con aiPlanStatus = "AUTO_APPROVED" por defecto
                    existingPlan = EventPlan(
                        eventId = eventIdWithDate,
                        dayDate = today,
                        calendarId = event.calendarId,
                        calendarEventId = event.id,
                        title = event.title,
                        description = event.description,
                        startTime = event.startTime,
                        endTime = event.endTime,
                        location = event.location,
                        isAllDay = event.isAllDay,
                        aiPlanStatus = "AUTO_APPROVED", // AUTO-APROBADO por defecto
                        userModified = false
                    )
                    database.eventPlanDao().insert(existingPlan)
                    newEvents.add(event)

                    // Programar notificaciones inmediatamente (no esperar aprobación)
                    unifiedMonkey.planNotificationsForEvent(existingPlan)
                }
            }

            // Procesar TODOS los eventos con la AI Estricta
            if (calendarEvents.isNotEmpty()) {
                Log.d(TAG, "Enviando ${calendarEvents.size} eventos a la AI Estricta para análisis")
                processAIPlansUseCase.processEventsWithAI(
                    calendarEvents = calendarEvents,
                    existingPlans = database.eventPlanDao().getEventsByDate(today)
                )
            }
        }
    }

    private fun observarCalendario() {
        viewModelScope.launch {
            calendarRepository.observeCalendarChanges().collect {
                if (_isMonitoring.value) {
                    Log.d(TAG, "observarCalendario: DETECTADO CAMBIO EN CALENDARIO. Re-sincronizando...")
                    val calendarEvents = calendarRepository.getTodayEvents()
                    syncCalendarWithRoom(calendarEvents)
                }
            }
        }
    }

    fun actualizarCriticidad(eventId: String, esCritico: Boolean) {
        viewModelScope.launch {
            database.eventPlanDao().getEvent(eventId)?.let {
                // Marcar como modificado por usuario
                val updatedPlan = it.copy(
                    isCritical = esCritico,
                    userModified = true // IMPORTANTE: El usuario hizo un cambio manual
                )
                database.eventPlanDao().update(updatedPlan)
                unifiedMonkey.planNotificationsForEvent(updatedPlan)
                Log.d(TAG, "Usuario cambió criticidad de '${it.title}' a $esCritico")
            }
        }
    }
    fun revivirEvento(eventId: String) {
        viewModelScope.launch {
            database.eventPlanDao().getEvent(eventId)?.let { event ->
                // Cambiar el estado a PENDING
                database.eventPlanDao().updateResolutionStatus(
                    eventId,
                    EventResolutionStatus.PENDING
                )

                // Registrar en el log
                database.conversationLogDao().insert(
                    ConversationLog(
                        timestamp = LocalDateTime.now(),
                        role = "user",
                        content = "🔄 Reactivé el evento: ${event.title}",
                        eventId = eventId
                    )
                )

                // Respuesta de Chapelotas
                val dayPlan = database.dayPlanDao().getTodayPlan()
                val response = if (dayPlan?.sarcasticMode == true) {
                    "¿Viste que no estaba tan hecho? ${event.title} está de vuelta en la lista. 🙄"
                } else {
                    "He reactivado ${event.title}. Las notificaciones se reanudarán según lo programado."
                }

                database.conversationLogDao().insert(
                    ConversationLog(
                        timestamp = LocalDateTime.now().plusSeconds(1),
                        role = "assistant",
                        content = response
                    )
                )

                // Re-programar notificaciones para el evento reactivado
                unifiedMonkey.planNotificationsForEvent(event)

                Log.d(TAG, "Evento '${event.title}' reactivado")
            }
        }
    }

    fun actualizarDistancia(eventId: String, distancia: String) {
        viewModelScope.launch {
            database.eventPlanDao().getEvent(eventId)?.let {
                // Marcar como modificado por usuario
                val updatedPlan = it.copy(
                    distance = EventDistance.fromString(distancia),
                    userModified = true // IMPORTANTE: El usuario hizo un cambio manual
                )
                database.eventPlanDao().update(updatedPlan)
                unifiedMonkey.planNotificationsForEvent(updatedPlan)
                Log.d(TAG, "Usuario cambió distancia de '${it.title}' a $distancia")
            }
        }
    }

    private fun observeSystemEvents() {
        viewModelScope.launch {
            eventBus.events.collect { event ->
                if (event is ChapelotasEvent.MonkeyCheckCompleted) {
                    // La UI se actualiza desde la DB
                }
            }
        }
    }
}