package com.chapelotas.app.domain.usecases;

/**
 * Caso de uso para marcar/desmarcar un evento como crítico
 * Los eventos críticos reciben el tratamiento de "llamada simulada"
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001B\u0017\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J,\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\b2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\rH\u0086B\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u000e\u0010\u000fR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u000b\n\u0002\b!\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006\u0010"}, d2 = {"Lcom/chapelotas/app/domain/usecases/MarkEventAsCriticalUseCase;", "", "calendarRepository", "Lcom/chapelotas/app/domain/repositories/CalendarRepository;", "notificationRepository", "Lcom/chapelotas/app/domain/repositories/NotificationRepository;", "(Lcom/chapelotas/app/domain/repositories/CalendarRepository;Lcom/chapelotas/app/domain/repositories/NotificationRepository;)V", "invoke", "Lkotlin/Result;", "", "eventId", "", "isCritical", "", "invoke-0E7RQCE", "(JZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class MarkEventAsCriticalUseCase {
    @org.jetbrains.annotations.NotNull()
    private final com.chapelotas.app.domain.repositories.CalendarRepository calendarRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.chapelotas.app.domain.repositories.NotificationRepository notificationRepository = null;
    
    @javax.inject.Inject()
    public MarkEventAsCriticalUseCase(@org.jetbrains.annotations.NotNull()
    com.chapelotas.app.domain.repositories.CalendarRepository calendarRepository, @org.jetbrains.annotations.NotNull()
    com.chapelotas.app.domain.repositories.NotificationRepository notificationRepository) {
        super();
    }
}