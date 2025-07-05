package com.chapelotas.app.domain.repositories;

/**
 * Repositorio para acceder a los eventos del calendario del dispositivo
 * Define el contrato, la implementación real estará en la capa data
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000H\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010$\n\u0002\u0010\t\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\"\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\n\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\bf\u0018\u00002\u00020\u0001J\u001a\u0010\u0002\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00050\u0003H\u00a6@\u00a2\u0006\u0002\u0010\u0006J\u0014\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00040\bH\u00a6@\u00a2\u0006\u0002\u0010\u0006J.\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\n2\u0006\u0010\f\u001a\u00020\r2\u0010\b\u0002\u0010\u000e\u001a\n\u0012\u0004\u0012\u00020\u0004\u0018\u00010\bH\u00a6@\u00a2\u0006\u0002\u0010\u000fJ6\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u000b0\n2\u0006\u0010\u0011\u001a\u00020\r2\u0006\u0010\u0012\u001a\u00020\r2\u0010\b\u0002\u0010\u000e\u001a\n\u0012\u0004\u0012\u00020\u0004\u0018\u00010\bH\u00a6@\u00a2\u0006\u0002\u0010\u0013J&\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u000b0\n2\u0010\b\u0002\u0010\u000e\u001a\n\u0012\u0004\u0012\u00020\u0004\u0018\u00010\bH\u0096@\u00a2\u0006\u0002\u0010\u0015J&\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u000b0\n2\u0010\b\u0002\u0010\u000e\u001a\n\u0012\u0004\u0012\u00020\u0004\u0018\u00010\bH\u0096@\u00a2\u0006\u0002\u0010\u0015J\u001e\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\u00042\u0006\u0010\u001a\u001a\u00020\u001bH\u00a6@\u00a2\u0006\u0002\u0010\u001cJ\u000e\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u00180\u001eH&\u00a8\u0006\u001f"}, d2 = {"Lcom/chapelotas/app/domain/repositories/CalendarRepository;", "", "getAvailableCalendars", "", "", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getCriticalEventIds", "", "getEventsForDate", "", "Lcom/chapelotas/app/domain/entities/CalendarEvent;", "date", "Ljava/time/LocalDate;", "calendarIds", "(Ljava/time/LocalDate;Ljava/util/Set;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getEventsInRange", "startDate", "endDate", "(Ljava/time/LocalDate;Ljava/time/LocalDate;Ljava/util/Set;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getTodayEvents", "(Ljava/util/Set;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getTomorrowEvents", "markEventAsCritical", "", "eventId", "isCritical", "", "(JZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "observeCalendarChanges", "Lkotlinx/coroutines/flow/Flow;", "app_debug"})
public abstract interface CalendarRepository {
    
    /**
     * Obtiene todos los calendarios disponibles en el dispositivo
     * @return Map de ID a nombre del calendario
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getAvailableCalendars(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.Map<java.lang.Long, java.lang.String>> $completion);
    
    /**
     * Obtiene todos los eventos de una fecha específica
     * @param date Fecha a consultar
     * @param calendarIds IDs de los calendarios a incluir (null = todos)
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getEventsForDate(@org.jetbrains.annotations.NotNull()
    java.time.LocalDate date, @org.jetbrains.annotations.Nullable()
    java.util.Set<java.lang.Long> calendarIds, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.chapelotas.app.domain.entities.CalendarEvent>> $completion);
    
    /**
     * Obtiene eventos en un rango de fechas
     * @param startDate Fecha inicial (inclusive)
     * @param endDate Fecha final (inclusive)
     * @param calendarIds IDs de los calendarios a incluir (null = todos)
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getEventsInRange(@org.jetbrains.annotations.NotNull()
    java.time.LocalDate startDate, @org.jetbrains.annotations.NotNull()
    java.time.LocalDate endDate, @org.jetbrains.annotations.Nullable()
    java.util.Set<java.lang.Long> calendarIds, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.chapelotas.app.domain.entities.CalendarEvent>> $completion);
    
    /**
     * Obtiene los eventos de hoy
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getTodayEvents(@org.jetbrains.annotations.Nullable()
    java.util.Set<java.lang.Long> calendarIds, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.chapelotas.app.domain.entities.CalendarEvent>> $completion);
    
    /**
     * Obtiene los eventos de mañana
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getTomorrowEvents(@org.jetbrains.annotations.Nullable()
    java.util.Set<java.lang.Long> calendarIds, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.chapelotas.app.domain.entities.CalendarEvent>> $completion);
    
    /**
     * Observa cambios en el calendario (Flow para actualizaciones en tiempo real)
     * Emite Unit cada vez que detecta un cambio
     */
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<kotlin.Unit> observeCalendarChanges();
    
    /**
     * Marca un evento como crítico (requiere alerta especial)
     * Esto es local a Chapelotas, no modifica el calendario real
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object markEventAsCritical(long eventId, boolean isCritical, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * Obtiene los IDs de eventos marcados como críticos
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getCriticalEventIds(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.Set<java.lang.Long>> $completion);
    
    /**
     * Repositorio para acceder a los eventos del calendario del dispositivo
     * Define el contrato, la implementación real estará en la capa data
     */
    @kotlin.Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    public static final class DefaultImpls {
        
        /**
         * Obtiene los eventos de hoy
         */
        @org.jetbrains.annotations.Nullable()
        public static java.lang.Object getTodayEvents(@org.jetbrains.annotations.NotNull()
        com.chapelotas.app.domain.repositories.CalendarRepository $this, @org.jetbrains.annotations.Nullable()
        java.util.Set<java.lang.Long> calendarIds, @org.jetbrains.annotations.NotNull()
        kotlin.coroutines.Continuation<? super java.util.List<com.chapelotas.app.domain.entities.CalendarEvent>> $completion) {
            return null;
        }
        
        /**
         * Obtiene los eventos de mañana
         */
        @org.jetbrains.annotations.Nullable()
        public static java.lang.Object getTomorrowEvents(@org.jetbrains.annotations.NotNull()
        com.chapelotas.app.domain.repositories.CalendarRepository $this, @org.jetbrains.annotations.Nullable()
        java.util.Set<java.lang.Long> calendarIds, @org.jetbrains.annotations.NotNull()
        kotlin.coroutines.Continuation<? super java.util.List<com.chapelotas.app.domain.entities.CalendarEvent>> $completion) {
            return null;
        }
    }
}