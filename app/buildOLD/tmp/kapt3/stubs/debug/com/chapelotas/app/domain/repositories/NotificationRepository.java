package com.chapelotas.app.domain.repositories;

/**
 * Repositorio para gestionar las notificaciones de Chapelotas
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000D\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\t\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\n\bf\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a6@\u00a2\u0006\u0002\u0010\u0006J\u0016\u0010\u0007\u001a\u00020\u00032\u0006\u0010\b\u001a\u00020\tH\u00a6@\u00a2\u0006\u0002\u0010\nJ\u0018\u0010\u000b\u001a\u00020\u00032\b\b\u0002\u0010\f\u001a\u00020\rH\u00a6@\u00a2\u0006\u0002\u0010\u000eJ\u001c\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00110\u00102\u0006\u0010\b\u001a\u00020\tH\u00a6@\u00a2\u0006\u0002\u0010\nJ\u0014\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00110\u0010H\u00a6@\u00a2\u0006\u0002\u0010\u0013J\u000e\u0010\u0014\u001a\u00020\u0015H\u00a6@\u00a2\u0006\u0002\u0010\u0013J\u0016\u0010\u0016\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a6@\u00a2\u0006\u0002\u0010\u0006J\u0014\u0010\u0017\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00110\u00100\u0018H&J\u001c\u0010\u0019\u001a\u00020\u00032\f\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00110\u0010H\u00a6@\u00a2\u0006\u0002\u0010\u001bJ\u0016\u0010\u001c\u001a\u00020\u00032\u0006\u0010\u001d\u001a\u00020\u0011H\u00a6@\u00a2\u0006\u0002\u0010\u001eJ\u0016\u0010\u001f\u001a\u00020\u00032\u0006\u0010\u001d\u001a\u00020\u0011H\u00a6@\u00a2\u0006\u0002\u0010\u001eJ\u000e\u0010 \u001a\u00020\u0003H\u00a6@\u00a2\u0006\u0002\u0010\u0013J\u000e\u0010!\u001a\u00020\u0003H\u00a6@\u00a2\u0006\u0002\u0010\u0013\u00a8\u0006\""}, d2 = {"Lcom/chapelotas/app/domain/repositories/NotificationRepository;", "", "cancelNotification", "", "notificationId", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "cancelNotificationsForEvent", "eventId", "", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "cleanOldNotifications", "daysToKeep", "", "(ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getNotificationsForEvent", "", "Lcom/chapelotas/app/domain/entities/ChapelotasNotification;", "getPendingNotifications", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "isNotificationServiceRunning", "", "markAsShown", "observeNotifications", "Lkotlinx/coroutines/flow/Flow;", "scheduleMultipleNotifications", "notifications", "(Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "scheduleNotification", "notification", "(Lcom/chapelotas/app/domain/entities/ChapelotasNotification;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "showImmediateNotification", "startNotificationService", "stopNotificationService", "app_debug"})
public abstract interface NotificationRepository {
    
    /**
     * Programa una notificación
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object scheduleNotification(@org.jetbrains.annotations.NotNull()
    com.chapelotas.app.domain.entities.ChapelotasNotification notification, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * Programa múltiples notificaciones
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object scheduleMultipleNotifications(@org.jetbrains.annotations.NotNull()
    java.util.List<com.chapelotas.app.domain.entities.ChapelotasNotification> notifications, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * Cancela una notificación programada
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object cancelNotification(@org.jetbrains.annotations.NotNull()
    java.lang.String notificationId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * Cancela todas las notificaciones de un evento específico
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object cancelNotificationsForEvent(long eventId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * Obtiene todas las notificaciones programadas pendientes
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getPendingNotifications(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.chapelotas.app.domain.entities.ChapelotasNotification>> $completion);
    
    /**
     * Obtiene las notificaciones de un evento específico
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getNotificationsForEvent(long eventId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.chapelotas.app.domain.entities.ChapelotasNotification>> $completion);
    
    /**
     * Marca una notificación como mostrada
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object markAsShown(@org.jetbrains.annotations.NotNull()
    java.lang.String notificationId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * Limpia notificaciones antiguas (ya mostradas y con más de X días)
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object cleanOldNotifications(int daysToKeep, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * Observa el estado de las notificaciones
     */
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.chapelotas.app.domain.entities.ChapelotasNotification>> observeNotifications();
    
    /**
     * Muestra una notificación inmediatamente (para alertas críticas)
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object showImmediateNotification(@org.jetbrains.annotations.NotNull()
    com.chapelotas.app.domain.entities.ChapelotasNotification notification, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * Verifica si el servicio de notificaciones está activo
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object isNotificationServiceRunning(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion);
    
    /**
     * Inicia el servicio de notificaciones persistente
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object startNotificationService(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * Detiene el servicio de notificaciones
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object stopNotificationService(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * Repositorio para gestionar las notificaciones de Chapelotas
     */
    @kotlin.Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    public static final class DefaultImpls {
    }
}