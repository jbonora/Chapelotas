package com.chapelotas.app.domain.usecases;

/**
 * Caso de uso para obtener el resumen del día
 * Este es el corazón de Chapelotas - genera el mensaje matutino
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001B\u001f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0018\u0010\t\u001a\u0004\u0018\u00010\n2\f\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\n0\fH\u0002J\u001c\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u000f0\u000eH\u0086B\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u0010\u0010\u0011R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u000b\n\u0002\b!\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006\u0012"}, d2 = {"Lcom/chapelotas/app/domain/usecases/GetDailySummaryUseCase;", "", "calendarRepository", "Lcom/chapelotas/app/domain/repositories/CalendarRepository;", "aiRepository", "Lcom/chapelotas/app/domain/repositories/AIRepository;", "preferencesRepository", "Lcom/chapelotas/app/domain/repositories/PreferencesRepository;", "(Lcom/chapelotas/app/domain/repositories/CalendarRepository;Lcom/chapelotas/app/domain/repositories/AIRepository;Lcom/chapelotas/app/domain/repositories/PreferencesRepository;)V", "findNextEvent", "Lcom/chapelotas/app/domain/entities/CalendarEvent;", "events", "", "invoke", "Lkotlin/Result;", "Lcom/chapelotas/app/domain/usecases/DailySummaryResult;", "invoke-IoAF18A", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class GetDailySummaryUseCase {
    @org.jetbrains.annotations.NotNull()
    private final com.chapelotas.app.domain.repositories.CalendarRepository calendarRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.chapelotas.app.domain.repositories.AIRepository aiRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.chapelotas.app.domain.repositories.PreferencesRepository preferencesRepository = null;
    
    @javax.inject.Inject()
    public GetDailySummaryUseCase(@org.jetbrains.annotations.NotNull()
    com.chapelotas.app.domain.repositories.CalendarRepository calendarRepository, @org.jetbrains.annotations.NotNull()
    com.chapelotas.app.domain.repositories.AIRepository aiRepository, @org.jetbrains.annotations.NotNull()
    com.chapelotas.app.domain.repositories.PreferencesRepository preferencesRepository) {
        super();
    }
    
    /**
     * Encuentra el próximo evento del día
     */
    private final com.chapelotas.app.domain.entities.CalendarEvent findNextEvent(java.util.List<com.chapelotas.app.domain.entities.CalendarEvent> events) {
        return null;
    }
}