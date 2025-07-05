package com.chapelotas.app.domain.repositories;

/**
 * Repositorio para interactuar con el servicio de IA
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0002\b\u0015\bf\u0018\u00002\u00020\u0001J(\u0010\u0002\u001a\u00020\u00032\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u00052\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\bH\u00a6@\u00a2\u0006\u0002\u0010\tJ&\u0010\n\u001a\u00020\b2\f\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\u00060\u00052\b\b\u0002\u0010\f\u001a\u00020\rH\u00a6@\u00a2\u0006\u0002\u0010\u000eJ4\u0010\u000f\u001a\u00020\b2\u0006\u0010\u0010\u001a\u00020\u00062\u0006\u0010\u0011\u001a\u00020\b2\b\b\u0002\u0010\f\u001a\u00020\r2\n\b\u0002\u0010\u0012\u001a\u0004\u0018\u00010\bH\u00a6@\u00a2\u0006\u0002\u0010\u0013J0\u0010\u0014\u001a\u0004\u0018\u00010\b2\u0006\u0010\u0010\u001a\u00020\u00062\n\b\u0002\u0010\u0015\u001a\u0004\u0018\u00010\b2\n\b\u0002\u0010\u0016\u001a\u0004\u0018\u00010\bH\u00a6@\u00a2\u0006\u0002\u0010\u0017J2\u0010\u0018\u001a\u00020\b2\f\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u00060\u00052\n\b\u0002\u0010\u001a\u001a\u0004\u0018\u00010\b2\b\b\u0002\u0010\f\u001a\u00020\rH\u00a6@\u00a2\u0006\u0002\u0010\u001bJ\u000e\u0010\u001c\u001a\u00020\bH\u00a6@\u00a2\u0006\u0002\u0010\u001dJ(\u0010\u001e\u001a\u00020\r2\u0006\u0010\u0010\u001a\u00020\u00062\u0010\b\u0002\u0010\u001f\u001a\n\u0012\u0004\u0012\u00020\u0006\u0018\u00010\u0005H\u00a6@\u00a2\u0006\u0002\u0010 J\u000e\u0010!\u001a\u00020\rH\u00a6@\u00a2\u0006\u0002\u0010\u001d\u00a8\u0006\""}, d2 = {"Lcom/chapelotas/app/domain/repositories/AIRepository;", "", "generateCommunicationPlan", "Lcom/chapelotas/app/domain/entities/AIPlan;", "events", "", "Lcom/chapelotas/app/domain/entities/CalendarEvent;", "userContext", "", "(Ljava/util/List;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "generateDailySummary", "todayEvents", "isSarcastic", "", "(Ljava/util/List;ZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "generateNotificationMessage", "event", "messageType", "additionalContext", "(Lcom/chapelotas/app/domain/entities/CalendarEvent;Ljava/lang/String;ZLjava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "generatePreparationTips", "weatherInfo", "trafficInfo", "(Lcom/chapelotas/app/domain/entities/CalendarEvent;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "generateTomorrowSummary", "tomorrowEvents", "todayContext", "(Ljava/util/List;Ljava/lang/String;ZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getCurrentModel", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "suggestCriticalEvents", "userHistory", "(Lcom/chapelotas/app/domain/entities/CalendarEvent;Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "testConnection", "app_debug"})
public abstract interface AIRepository {
    
    /**
     * Genera un plan de comunicación basado en los eventos del día
     * @param events Lista de eventos a analizar
     * @param userContext Contexto adicional del usuario (preferencias, historial, etc.)
     * @return Plan de notificaciones generado por la IA
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object generateCommunicationPlan(@org.jetbrains.annotations.NotNull()
    java.util.List<com.chapelotas.app.domain.entities.CalendarEvent> events, @org.jetbrains.annotations.Nullable()
    java.lang.String userContext, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.chapelotas.app.domain.entities.AIPlan> $completion);
    
    /**
     * Genera un mensaje personalizado para una notificación
     * @param event Evento sobre el cual notificar
     * @param messageType Tipo de mensaje a generar
     * @param isSarcastic Si debe usar tono sarcástico
     * @param additionalContext Contexto adicional para personalizar el mensaje
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object generateNotificationMessage(@org.jetbrains.annotations.NotNull()
    com.chapelotas.app.domain.entities.CalendarEvent event, @org.jetbrains.annotations.NotNull()
    java.lang.String messageType, boolean isSarcastic, @org.jetbrains.annotations.Nullable()
    java.lang.String additionalContext, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.String> $completion);
    
    /**
     * Genera el resumen del día
     * @param todayEvents Eventos de hoy
     * @param isSarcastic Si debe usar tono sarcástico
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object generateDailySummary(@org.jetbrains.annotations.NotNull()
    java.util.List<com.chapelotas.app.domain.entities.CalendarEvent> todayEvents, boolean isSarcastic, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.String> $completion);
    
    /**
     * Genera el resumen de mañana
     * @param tomorrowEvents Eventos de mañana
     * @param todayContext Contexto de lo que pasó hoy
     * @param isSarcastic Si debe usar tono sarcástico
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object generateTomorrowSummary(@org.jetbrains.annotations.NotNull()
    java.util.List<com.chapelotas.app.domain.entities.CalendarEvent> tomorrowEvents, @org.jetbrains.annotations.Nullable()
    java.lang.String todayContext, boolean isSarcastic, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.String> $completion);
    
    /**
     * Analiza si un evento debería ser crítico basado en su contenido
     * @param event Evento a analizar
     * @param userHistory Historial de eventos críticos previos
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object suggestCriticalEvents(@org.jetbrains.annotations.NotNull()
    com.chapelotas.app.domain.entities.CalendarEvent event, @org.jetbrains.annotations.Nullable()
    java.util.List<com.chapelotas.app.domain.entities.CalendarEvent> userHistory, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion);
    
    /**
     * Genera un mensaje de preparación inteligente
     * Por ejemplo: "Acordate de llevar paraguas, hay 80% de lluvia"
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object generatePreparationTips(@org.jetbrains.annotations.NotNull()
    com.chapelotas.app.domain.entities.CalendarEvent event, @org.jetbrains.annotations.Nullable()
    java.lang.String weatherInfo, @org.jetbrains.annotations.Nullable()
    java.lang.String trafficInfo, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.String> $completion);
    
    /**
     * Verifica la conectividad con el servicio de IA
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object testConnection(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion);
    
    /**
     * Obtiene el modelo de IA actual en uso
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getCurrentModel(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.String> $completion);
    
    /**
     * Repositorio para interactuar con el servicio de IA
     */
    @kotlin.Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    public static final class DefaultImpls {
    }
}