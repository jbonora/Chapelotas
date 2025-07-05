package com.chapelotas.app.domain.repositories;

/**
 * Repositorio para gestionar las preferencias del usuario
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000D\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0010\"\n\u0002\u0010\t\n\u0002\b\t\bf\u0018\u00002\u00020\u0001J\u000e\u0010\u0002\u001a\u00020\u0003H\u00a6@\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u0005\u001a\u00020\u0006H\u00a6@\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u0007\u001a\u00020\bH\u00a6@\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\t\u001a\u00020\u0003H\u00a6@\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00060\u000bH&J\u000e\u0010\f\u001a\u00020\u0003H\u00a6@\u00a2\u0006\u0002\u0010\u0004J\u0016\u0010\r\u001a\u00020\u00032\u0006\u0010\u000e\u001a\u00020\bH\u00a6@\u00a2\u0006\u0002\u0010\u000fJ\u0016\u0010\u0010\u001a\u00020\u00032\u0006\u0010\u0011\u001a\u00020\u0012H\u00a6@\u00a2\u0006\u0002\u0010\u0013J\u001e\u0010\u0014\u001a\u00020\u00032\u0006\u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u0016H\u00a6@\u00a2\u0006\u0002\u0010\u0018J\u0016\u0010\u0019\u001a\u00020\u00032\u0006\u0010\u0011\u001a\u00020\u0012H\u00a6@\u00a2\u0006\u0002\u0010\u0013J\u001c\u0010\u001a\u001a\u00020\u00032\f\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u001d0\u001cH\u00a6@\u00a2\u0006\u0002\u0010\u001eJ\u0016\u0010\u001f\u001a\u00020\u00032\u0006\u0010 \u001a\u00020\u0016H\u00a6@\u00a2\u0006\u0002\u0010!J\u001e\u0010\"\u001a\u00020\u00032\u0006\u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u0016H\u00a6@\u00a2\u0006\u0002\u0010\u0018J\u0016\u0010#\u001a\u00020\u00032\u0006\u0010$\u001a\u00020\u0006H\u00a6@\u00a2\u0006\u0002\u0010%\u00a8\u0006&"}, d2 = {"Lcom/chapelotas/app/domain/repositories/PreferencesRepository;", "", "acceptPrivacyPolicy", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getUserPreferences", "Lcom/chapelotas/app/domain/entities/UserPreferences;", "isFirstTimeUser", "", "markAsExperiencedUser", "observeUserPreferences", "Lkotlinx/coroutines/flow/Flow;", "resetToDefaults", "setSarcasticMode", "enabled", "(ZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateCriticalAlertSound", "soundUri", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateDailySummaryTime", "hour", "", "minute", "(IILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateNotificationSound", "updatePreferredCalendars", "calendarIds", "", "", "(Ljava/util/Set;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateReminderMinutesBefore", "minutes", "(ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateTomorrowSummaryTime", "updateUserPreferences", "preferences", "(Lcom/chapelotas/app/domain/entities/UserPreferences;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public abstract interface PreferencesRepository {
    
    /**
     * Obtiene las preferencias actuales del usuario
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getUserPreferences(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.chapelotas.app.domain.entities.UserPreferences> $completion);
    
    /**
     * Observa cambios en las preferencias (Flow para actualizaciones en tiempo real)
     */
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<com.chapelotas.app.domain.entities.UserPreferences> observeUserPreferences();
    
    /**
     * Actualiza todas las preferencias
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateUserPreferences(@org.jetbrains.annotations.NotNull()
    com.chapelotas.app.domain.entities.UserPreferences preferences, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * Actualiza el horario del resumen diario
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateDailySummaryTime(int hour, int minute, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * Actualiza el horario del resumen de mañana
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateTomorrowSummaryTime(int hour, int minute, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * Activa/desactiva el modo sarcástico
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object setSarcasticMode(boolean enabled, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * Actualiza los calendarios preferidos
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updatePreferredCalendars(@org.jetbrains.annotations.NotNull()
    java.util.Set<java.lang.Long> calendarIds, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * Marca que el usuario aceptó la política de privacidad
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object acceptPrivacyPolicy(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * Marca que el usuario ya no es primera vez
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object markAsExperiencedUser(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * Actualiza el sonido de alerta crítica
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateCriticalAlertSound(@org.jetbrains.annotations.NotNull()
    java.lang.String soundUri, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * Actualiza el sonido de notificación normal
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateNotificationSound(@org.jetbrains.annotations.NotNull()
    java.lang.String soundUri, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * Actualiza minutos antes del evento para recordatorio
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateReminderMinutesBefore(int minutes, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * Resetea todas las preferencias a valores por defecto
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object resetToDefaults(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    /**
     * Verifica si es la primera vez que se ejecuta la app
     */
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object isFirstTimeUser(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion);
}