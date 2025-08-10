package com.chapelotas.app.domain.repositories

import com.chapelotas.app.domain.entities.CalendarEvent
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Repositorio para acceder a los eventos del calendario del dispositivo.
 * Su única responsabilidad es la de interactuar con los datos del calendario.
 */
interface CalendarRepository {

    /**
     * Obtiene todos los calendarios visibles en el dispositivo.
     * @return Un mapa donde la clave es el ID del calendario y el valor es su nombre.
     */
    suspend fun getAvailableCalendars(): Map<Long, String>

    /**
     * Obtiene todos los eventos de una fecha específica.
     * @param date La fecha para la cual se quieren obtener los eventos.
     */
    suspend fun getEventsForDate(date: LocalDate): List<CalendarEvent>

    /**
     * Obtiene eventos en un rango de fechas.
     * @param startDate La fecha inicial del rango (inclusiva).
     * @param endDate La fecha final del rango (inclusiva).
     */
    suspend fun getEventsInRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<CalendarEvent>

    /**
     * Obtiene los eventos programados para hoy.
     */
    suspend fun getTodayEvents(): List<CalendarEvent> {
        return getEventsForDate(LocalDate.now())
    }

    /**
     * Observa cambios en el calendario en tiempo real (añadir, modificar, eliminar eventos).
     * @return Un Flow que emite una señal (Unit) cada vez que se detecta un cambio.
     */
    fun observeCalendarChanges(): Flow<Unit>

    /**
     * Marca un evento como crítico. Esta es una bandera interna de Chapelotas.
     * @param eventId El ID del evento a marcar.
     * @param isCritical El estado crítico.
     */
    suspend fun markEventAsCritical(eventId: Long, isCritical: Boolean)

    /**
     * Obtiene los IDs de todos los eventos marcados como críticos.
     */
    suspend fun getCriticalEventIds(): Set<Long>

    /**
     * Actualiza la hora de inicio y fin de un evento existente en el calendario.
     * @return Devuelve true si la actualización fue exitosa, false en caso contrario.
     */
    suspend fun updateEventTime(eventId: Long, newStartTime: LocalDateTime, newEndTime: LocalDateTime): Boolean
}