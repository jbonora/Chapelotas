package com.chapelotas.app.domain.repositories

import com.chapelotas.app.domain.entities.CalendarEvent
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repositorio para acceder a los eventos del calendario del dispositivo
 * Define el contrato, la implementación real estará en la capa data
 */
interface CalendarRepository {

    /**
     * Verifica si la app tiene permiso para leer el calendario.
     * @return true si el permiso está concedido, false en caso contrario.
     */
    fun hasCalendarReadPermission(): Boolean

    /**
     * Obtiene todos los calendarios disponibles en el dispositivo
     * @return Map de ID a nombre del calendario
     */
    suspend fun getAvailableCalendars(): Map<Long, String>

    /**
     * Obtiene todos los eventos de una fecha específica
     * @param date Fecha a consultar
     */
    suspend fun getEventsForDate(date: LocalDate): List<CalendarEvent>

    /**
     * Obtiene eventos en un rango de fechas
     * @param startDate Fecha inicial (inclusive)
     * @param endDate Fecha final (inclusive)
     */
    suspend fun getEventsInRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<CalendarEvent>

    /**
     * Obtiene los eventos de hoy
     */
    suspend fun getTodayEvents(): List<CalendarEvent> {
        return getEventsForDate(LocalDate.now())
    }

    /**
     * Obtiene los eventos de mañana
     */
    suspend fun getTomorrowEvents(): List<CalendarEvent> {
        return getEventsForDate(LocalDate.now().plusDays(1))
    }

    /**
     * Observa cambios en el calendario (Flow para actualizaciones en tiempo real)
     * Emite Unit cada vez que detecta un cambio
     */
    fun observeCalendarChanges(): Flow<Unit>

    /**
     * Marca un evento como crítico (requiere alerta especial)
     * Esto es local a Chapelotas, no modifica el calendario real
     */
    suspend fun markEventAsCritical(eventId: Long, isCritical: Boolean)

    /**
     * Obtiene los IDs de eventos marcados como críticos
     */
    suspend fun getCriticalEventIds(): Set<Long>
}