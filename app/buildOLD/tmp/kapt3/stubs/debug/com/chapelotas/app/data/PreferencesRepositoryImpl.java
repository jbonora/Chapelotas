package com.chapelotas.app.data;

/**
 * Implementación del repositorio de preferencias usando SharedPreferences
 */
@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000V\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0010\"\n\u0002\u0010\t\n\u0002\b\t\b\u0007\u0018\u0000 /2\u00020\u0001:\u0001/B\u0011\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\n\u001a\u00020\u000bH\u0096@\u00a2\u0006\u0002\u0010\fJ\u000e\u0010\r\u001a\u00020\u0007H\u0096@\u00a2\u0006\u0002\u0010\fJ\u000e\u0010\u000e\u001a\u00020\u000fH\u0096@\u00a2\u0006\u0002\u0010\fJ\b\u0010\u0010\u001a\u00020\u0007H\u0002J\u000e\u0010\u0011\u001a\u00020\u000bH\u0096@\u00a2\u0006\u0002\u0010\fJ\u000e\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00070\u0013H\u0016J\u000e\u0010\u0014\u001a\u00020\u000bH\u0096@\u00a2\u0006\u0002\u0010\fJ\u0010\u0010\u0015\u001a\u00020\u000b2\u0006\u0010\u0016\u001a\u00020\u0007H\u0002J\u0016\u0010\u0017\u001a\u00020\u000b2\u0006\u0010\u0018\u001a\u00020\u000fH\u0096@\u00a2\u0006\u0002\u0010\u0019J\u0016\u0010\u001a\u001a\u00020\u000b2\u0006\u0010\u001b\u001a\u00020\u001cH\u0096@\u00a2\u0006\u0002\u0010\u001dJ\u001e\u0010\u001e\u001a\u00020\u000b2\u0006\u0010\u001f\u001a\u00020 2\u0006\u0010!\u001a\u00020 H\u0096@\u00a2\u0006\u0002\u0010\"J\u0016\u0010#\u001a\u00020\u000b2\u0006\u0010\u001b\u001a\u00020\u001cH\u0096@\u00a2\u0006\u0002\u0010\u001dJ\u001c\u0010$\u001a\u00020\u000b2\f\u0010%\u001a\b\u0012\u0004\u0012\u00020\'0&H\u0096@\u00a2\u0006\u0002\u0010(J\u0016\u0010)\u001a\u00020\u000b2\u0006\u0010*\u001a\u00020 H\u0096@\u00a2\u0006\u0002\u0010+J\u001e\u0010,\u001a\u00020\u000b2\u0006\u0010\u001f\u001a\u00020 2\u0006\u0010!\u001a\u00020 H\u0096@\u00a2\u0006\u0002\u0010\"J\u0016\u0010-\u001a\u00020\u000b2\u0006\u0010\u0016\u001a\u00020\u0007H\u0096@\u00a2\u0006\u0002\u0010.R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u00060"}, d2 = {"Lcom/chapelotas/app/data/PreferencesRepositoryImpl;", "Lcom/chapelotas/app/domain/repositories/PreferencesRepository;", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "_preferencesFlow", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/chapelotas/app/domain/entities/UserPreferences;", "prefs", "Landroid/content/SharedPreferences;", "acceptPrivacyPolicy", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getUserPreferences", "isFirstTimeUser", "", "loadPreferences", "markAsExperiencedUser", "observeUserPreferences", "Lkotlinx/coroutines/flow/Flow;", "resetToDefaults", "savePreferences", "preferences", "setSarcasticMode", "enabled", "(ZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateCriticalAlertSound", "soundUri", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateDailySummaryTime", "hour", "", "minute", "(IILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateNotificationSound", "updatePreferredCalendars", "calendarIds", "", "", "(Ljava/util/Set;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateReminderMinutesBefore", "minutes", "(ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateTomorrowSummaryTime", "updateUserPreferences", "(Lcom/chapelotas/app/domain/entities/UserPreferences;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Companion", "app_debug"})
public final class PreferencesRepositoryImpl implements com.chapelotas.app.domain.repositories.PreferencesRepository {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final android.content.SharedPreferences prefs = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.chapelotas.app.domain.entities.UserPreferences> _preferencesFlow = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PREFS_NAME = "chapelotas_preferences";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_DAILY_SUMMARY_HOUR = "daily_summary_hour";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_DAILY_SUMMARY_MINUTE = "daily_summary_minute";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_TOMORROW_SUMMARY_HOUR = "tomorrow_summary_hour";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_TOMORROW_SUMMARY_MINUTE = "tomorrow_summary_minute";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_SARCASTIC_MODE = "sarcastic_mode";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_CRITICAL_ALERT_SOUND = "critical_alert_sound";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_NOTIFICATION_SOUND = "notification_sound";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_REMINDER_MINUTES = "reminder_minutes_before";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_PRIVACY_ACCEPTED = "privacy_policy_accepted";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_FIRST_TIME_USER = "is_first_time_user";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_PREFERRED_CALENDARS = "preferred_calendars";
    @org.jetbrains.annotations.NotNull()
    public static final com.chapelotas.app.data.PreferencesRepositoryImpl.Companion Companion = null;
    
    @javax.inject.Inject()
    public PreferencesRepositoryImpl(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object getUserPreferences(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.chapelotas.app.domain.entities.UserPreferences> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.Flow<com.chapelotas.app.domain.entities.UserPreferences> observeUserPreferences() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object updateUserPreferences(@org.jetbrains.annotations.NotNull()
    com.chapelotas.app.domain.entities.UserPreferences preferences, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object updateDailySummaryTime(int hour, int minute, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object updateTomorrowSummaryTime(int hour, int minute, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object setSarcasticMode(boolean enabled, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object updatePreferredCalendars(@org.jetbrains.annotations.NotNull()
    java.util.Set<java.lang.Long> calendarIds, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object acceptPrivacyPolicy(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object markAsExperiencedUser(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object updateCriticalAlertSound(@org.jetbrains.annotations.NotNull()
    java.lang.String soundUri, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object updateNotificationSound(@org.jetbrains.annotations.NotNull()
    java.lang.String soundUri, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object updateReminderMinutesBefore(int minutes, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object resetToDefaults(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object isFirstTimeUser(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    /**
     * Carga las preferencias desde SharedPreferences
     */
    private final com.chapelotas.app.domain.entities.UserPreferences loadPreferences() {
        return null;
    }
    
    /**
     * Guarda las preferencias en SharedPreferences
     */
    private final void savePreferences(com.chapelotas.app.domain.entities.UserPreferences preferences) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\f\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0010"}, d2 = {"Lcom/chapelotas/app/data/PreferencesRepositoryImpl$Companion;", "", "()V", "KEY_CRITICAL_ALERT_SOUND", "", "KEY_DAILY_SUMMARY_HOUR", "KEY_DAILY_SUMMARY_MINUTE", "KEY_FIRST_TIME_USER", "KEY_NOTIFICATION_SOUND", "KEY_PREFERRED_CALENDARS", "KEY_PRIVACY_ACCEPTED", "KEY_REMINDER_MINUTES", "KEY_SARCASTIC_MODE", "KEY_TOMORROW_SUMMARY_HOUR", "KEY_TOMORROW_SUMMARY_MINUTE", "PREFS_NAME", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}