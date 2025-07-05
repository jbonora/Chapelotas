package com.chapelotas.app.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chapelotas.app.domain.entities.CalendarEvent
import com.chapelotas.app.domain.entities.MasterPlan
import com.chapelotas.app.domain.repositories.CalendarRepository
import com.chapelotas.app.domain.usecases.MasterPlanController
import com.chapelotas.app.domain.usecases.MonkeyCheckerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalendarMonitorViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val masterPlanController: MasterPlanController,
    private val monkeyChecker: MonkeyCheckerService
) : ViewModel() {

    private val _currentPlan = MutableStateFlow<MasterPlan?>(null)
    val currentPlan: StateFlow<MasterPlan?> = _currentPlan

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring

    init {
        // Intentar cargar plan existente al iniciar
        viewModelScope.launch {
            val planExistente = monkeyChecker.loadMasterPlan()
            if (planExistente != null) {
                _currentPlan.value = planExistente
                Log.d("CalendarMonitor", "Plan existente cargado")
            }

            // Iniciar monitoreo automáticamente
            delay(2000) // Dar tiempo a que todo se inicialice
            if (!_isMonitoring.value) {
                iniciarDia()
            }
        }
    }

    /**
     * Iniciar el día (se llama automáticamente)
     */
    fun iniciarDia() {
        if (_isMonitoring.value) {
            Log.d("CalendarMonitor", "El mono ya está activo")
            return
        }

        viewModelScope.launch {
            try {
                Log.d("CalendarMonitor", "🐵 Despertando al mono...")

                // 1. Obtener eventos del día
                val todayEvents = calendarRepository.getTodayEvents()
                Log.d("CalendarMonitor", "Eventos encontrados: ${todayEvents.size}")

                // 2. Cargar plan existente o generar uno nuevo
                var planInicial = monkeyChecker.loadMasterPlan()

                if (planInicial == null || planInicial.eventosHoy.isEmpty()) {
                    // Generar plan inicial con IA
                    Log.d("CalendarMonitor", "Generando plan inicial con IA...")
                    planInicial = masterPlanController.analizarDiaInicial(todayEvents)
                } else {
                    // Actualizar eventos si hay cambios
                    Log.d("CalendarMonitor", "Actualizando plan existente...")
                    // TODO: Sincronizar eventos nuevos/eliminados
                }

                // 3. Guardar plan
                monkeyChecker.saveMasterPlan(planInicial)
                _currentPlan.value = planInicial

                // 4. Iniciar el mono
                monkeyChecker.startMonkey()
                _isMonitoring.value = true

                // 5. Observar cambios en calendario
                observarCalendario()

                // 6. Auto-recuperación: Si el mono muere, revivirlo
                monitorearEstadoMono()

            } catch (e: Exception) {
                Log.e("CalendarMonitor", "Error iniciando el día", e)
                // Reintentar en 5 segundos
                delay(5000)
                iniciarDia()
            }
        }
    }

    /**
     * El mono nunca debería detenerse, pero por si acaso...
     */
    fun detenerMonitoreo() {
        // NO hacer nada - el mono es inmortal
        Log.w("CalendarMonitor", "Intento de detener el mono ignorado - El mono es eterno 🐵")
    }

    /**
     * Monitorear que el mono siga vivo
     */
    private fun monitorearEstadoMono() {
        viewModelScope.launch {
            while (true) {
                delay(60_000) // Cada minuto

                if (!_isMonitoring.value) {
                    Log.w("CalendarMonitor", "¡El mono murió! Reviviéndolo...")
                    iniciarDia()
                }

                // Actualizar el plan visible
                val planActual = monkeyChecker.loadMasterPlan()
                if (planActual != null) {
                    _currentPlan.value = planActual
                }
            }
        }
    }

    /**
     * Usuario actualiza criticidad/distancia
     */
    fun actualizarEvento(eventoId: String, esCritico: Boolean, distancia: String) {
        viewModelScope.launch {
            val plan = monkeyChecker.loadMasterPlan() ?: return@launch

            plan.eventosHoy.find { it.id == eventoId }?.let { evento ->
                evento.esCritico = esCritico
                evento.distancia = distancia

                // Recalcular tiempos de aviso según distancia
                evento.avisosSugeridos = when(distancia) {
                    "lejos" -> listOf(60, 30, 10)
                    "cerca" -> listOf(20, 10)
                    else -> listOf(15, 5)
                }

                // Reprogramar notificaciones para este evento
                val ahora = java.time.LocalTime.now()
                val horaEvento = java.time.LocalTime.parse(evento.horaInicio,
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm"))

                evento.notificaciones.clear()
                evento.avisosSugeridos.forEach { minutosAntes ->
                    val horaNotif = horaEvento.minusMinutes(minutosAntes.toLong())
                    if (horaNotif.isAfter(ahora)) {
                        evento.notificaciones.add(
                            com.chapelotas.app.domain.entities.NotificacionProgramada(
                                horaExacta = horaNotif.format(
                                    java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                                ),
                                minutosAntes = minutosAntes,
                                ejecutada = false,
                                tipo = if (esCritico && minutosAntes <= 15) "alerta_critica" else "recordatorio",
                                razonIa = "Actualizado por usuario - distancia: $distancia"
                            )
                        )
                    }
                }
            }

            monkeyChecker.saveMasterPlan(plan)
            _currentPlan.value = plan

            Log.d("CalendarMonitor", "Evento $eventoId actualizado: crítico=$esCritico, distancia=$distancia")
        }
    }

    /**
     * Observar cambios en calendario
     */
    private fun observarCalendario() {
        viewModelScope.launch {
            calendarRepository.observeCalendarChanges().collect {
                Log.d("CalendarMonitor", "Cambio detectado en calendario")

                try {
                    // Detectar eventos nuevos
                    val eventosActuales = calendarRepository.getTodayEvents()
                    val plan = monkeyChecker.loadMasterPlan() ?: return@collect

                    val idsExistentes = plan.eventosHoy.map { it.id }.toSet()
                    val eventosNuevos = eventosActuales.filter {
                        it.id.toString() !in idsExistentes
                    }

                    // Procesar cada evento nuevo
                    eventosNuevos.forEach { eventoNuevo ->
                        Log.d("CalendarMonitor", "Evento nuevo detectado: ${eventoNuevo.title}")
                        monkeyChecker.procesarEventoNuevo(eventoNuevo)
                    }

                    // Detectar eventos eliminados
                    val idsActuales = eventosActuales.map { it.id.toString() }.toSet()
                    val eventosEliminados = plan.eventosHoy.filter { it.id !in idsActuales }

                    if (eventosEliminados.isNotEmpty()) {
                        Log.d("CalendarMonitor", "Eventos eliminados: ${eventosEliminados.size}")
                        plan.eventosHoy.removeAll(eventosEliminados)
                        monkeyChecker.saveMasterPlan(plan)
                    }

                    // Actualizar vista
                    _currentPlan.value = monkeyChecker.loadMasterPlan()

                } catch (e: Exception) {
                    Log.e("CalendarMonitor", "Error procesando cambios del calendario", e)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // NO detener el mono - debe seguir corriendo
        Log.d("CalendarMonitor", "ViewModel limpiado pero el mono sigue activo")
    }
}