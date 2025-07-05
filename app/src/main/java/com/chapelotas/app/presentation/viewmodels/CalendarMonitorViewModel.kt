package com.chapelotas.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chapelotas.app.domain.entities.CalendarEvent
import com.chapelotas.app.domain.entities.MasterPlan
import com.chapelotas.app.domain.repositories.CalendarRepository
import com.chapelotas.app.domain.usecases.MasterPlanController
import com.chapelotas.app.domain.usecases.MonkeyCheckerService
import dagger.hilt.android.lifecycle.HiltViewModel
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

    /**
     * Iniciar el día (7AM o manual)
     */
    fun iniciarDia() {
        viewModelScope.launch {
            // 1. Obtener eventos del día
            val todayEvents = calendarRepository.getTodayEvents()

            // 2. Generar plan inicial con IA
            val planInicial = masterPlanController.analizarDiaInicial(todayEvents)

            // 3. Guardar plan
            monkeyChecker.saveMasterPlan(planInicial)
            _currentPlan.value = planInicial

            // 4. Iniciar el mono
            monkeyChecker.startMonkey()
            _isMonitoring.value = true

            // 5. Observar cambios en calendario
            observarCalendario()
        }
    }

    /**
     * Detener monitoreo
     */
    fun detenerMonitoreo() {
        monkeyChecker.stopMonkey()
        _isMonitoring.value = false
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
            }

            monkeyChecker.saveMasterPlan(plan)
            _currentPlan.value = plan
        }
    }

    /**
     * Observar cambios en calendario
     */
    private fun observarCalendario() {
        viewModelScope.launch {
            calendarRepository.observeCalendarChanges().collect {
                // Detectar eventos nuevos
                val eventosActuales = calendarRepository.getTodayEvents()
                val plan = monkeyChecker.loadMasterPlan() ?: return@collect

                val idsExistentes = plan.eventosHoy.map { it.id }.toSet()
                val eventosNuevos = eventosActuales.filter {
                    it.id.toString() !in idsExistentes
                }

                // Procesar cada evento nuevo
                eventosNuevos.forEach { eventoNuevo ->
                    monkeyChecker.procesarEventoNuevo(eventoNuevo)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        detenerMonitoreo()
    }
}