package com.chapelotas.app.domain.entities;

/**
 * Preferencias del usuario para Chapelotas
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010\"\n\u0002\u0010\t\n\u0002\b\u001e\b\u0087\b\u0018\u00002\u00020\u0001Be\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0005\u001a\u00020\u0006\u0012\b\b\u0002\u0010\u0007\u001a\u00020\b\u0012\b\b\u0002\u0010\t\u001a\u00020\b\u0012\b\b\u0002\u0010\n\u001a\u00020\u000b\u0012\b\b\u0002\u0010\f\u001a\u00020\u0006\u0012\b\b\u0002\u0010\r\u001a\u00020\u0006\u0012\u000e\b\u0002\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00100\u000f\u00a2\u0006\u0002\u0010\u0011J\t\u0010\u001e\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u001f\u001a\u00020\u0003H\u00c6\u0003J\t\u0010 \u001a\u00020\u0006H\u00c6\u0003J\t\u0010!\u001a\u00020\bH\u00c6\u0003J\t\u0010\"\u001a\u00020\bH\u00c6\u0003J\t\u0010#\u001a\u00020\u000bH\u00c6\u0003J\t\u0010$\u001a\u00020\u0006H\u00c6\u0003J\t\u0010%\u001a\u00020\u0006H\u00c6\u0003J\u000f\u0010&\u001a\b\u0012\u0004\u0012\u00020\u00100\u000fH\u00c6\u0003Ji\u0010\'\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00062\b\b\u0002\u0010\u0007\u001a\u00020\b2\b\b\u0002\u0010\t\u001a\u00020\b2\b\b\u0002\u0010\n\u001a\u00020\u000b2\b\b\u0002\u0010\f\u001a\u00020\u00062\b\b\u0002\u0010\r\u001a\u00020\u00062\u000e\b\u0002\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00100\u000fH\u00c6\u0001J\u0013\u0010(\u001a\u00020\u00062\b\u0010)\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010*\u001a\u00020\u000bH\u00d6\u0001J\u0006\u0010+\u001a\u00020\u0006J\u0006\u0010,\u001a\u00020\u0006J\t\u0010-\u001a\u00020\bH\u00d6\u0001R\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015R\u0011\u0010\f\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0017R\u0011\u0010\r\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u0017R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0017R\u0011\u0010\n\u001a\u00020\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0019R\u0011\u0010\t\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u0013R\u0017\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00100\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u001cR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u0015\u00a8\u0006."}, d2 = {"Lcom/chapelotas/app/domain/entities/UserPreferences;", "", "dailySummaryTime", "Ljava/time/LocalTime;", "tomorrowSummaryTime", "isSarcasticModeEnabled", "", "criticalAlertSound", "", "notificationSound", "minutesBeforeEventForReminder", "", "hasAcceptedPrivacyPolicy", "isFirstTimeUser", "preferredCalendars", "", "", "(Ljava/time/LocalTime;Ljava/time/LocalTime;ZLjava/lang/String;Ljava/lang/String;IZZLjava/util/Set;)V", "getCriticalAlertSound", "()Ljava/lang/String;", "getDailySummaryTime", "()Ljava/time/LocalTime;", "getHasAcceptedPrivacyPolicy", "()Z", "getMinutesBeforeEventForReminder", "()I", "getNotificationSound", "getPreferredCalendars", "()Ljava/util/Set;", "getTomorrowSummaryTime", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "equals", "other", "hashCode", "isTimeForDailySummary", "isTimeForTomorrowSummary", "toString", "app_debug"})
public final class UserPreferences {
    @org.jetbrains.annotations.NotNull()
    private final java.time.LocalTime dailySummaryTime = null;
    @org.jetbrains.annotations.NotNull()
    private final java.time.LocalTime tomorrowSummaryTime = null;
    private final boolean isSarcasticModeEnabled = false;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String criticalAlertSound = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String notificationSound = null;
    private final int minutesBeforeEventForReminder = 0;
    private final boolean hasAcceptedPrivacyPolicy = false;
    private final boolean isFirstTimeUser = false;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Set<java.lang.Long> preferredCalendars = null;
    
    public UserPreferences(@org.jetbrains.annotations.NotNull()
    java.time.LocalTime dailySummaryTime, @org.jetbrains.annotations.NotNull()
    java.time.LocalTime tomorrowSummaryTime, boolean isSarcasticModeEnabled, @org.jetbrains.annotations.NotNull()
    java.lang.String criticalAlertSound, @org.jetbrains.annotations.NotNull()
    java.lang.String notificationSound, int minutesBeforeEventForReminder, boolean hasAcceptedPrivacyPolicy, boolean isFirstTimeUser, @org.jetbrains.annotations.NotNull()
    java.util.Set<java.lang.Long> preferredCalendars) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.time.LocalTime getDailySummaryTime() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.time.LocalTime getTomorrowSummaryTime() {
        return null;
    }
    
    public final boolean isSarcasticModeEnabled() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getCriticalAlertSound() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getNotificationSound() {
        return null;
    }
    
    public final int getMinutesBeforeEventForReminder() {
        return 0;
    }
    
    public final boolean getHasAcceptedPrivacyPolicy() {
        return false;
    }
    
    public final boolean isFirstTimeUser() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Set<java.lang.Long> getPreferredCalendars() {
        return null;
    }
    
    /**
     * Verifica si es hora del resumen diario
     */
    public final boolean isTimeForDailySummary() {
        return false;
    }
    
    /**
     * Verifica si es hora del resumen de mañana
     */
    public final boolean isTimeForTomorrowSummary() {
        return false;
    }
    
    public UserPreferences() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.time.LocalTime component1() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.time.LocalTime component2() {
        return null;
    }
    
    public final boolean component3() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component4() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component5() {
        return null;
    }
    
    public final int component6() {
        return 0;
    }
    
    public final boolean component7() {
        return false;
    }
    
    public final boolean component8() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Set<java.lang.Long> component9() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.chapelotas.app.domain.entities.UserPreferences copy(@org.jetbrains.annotations.NotNull()
    java.time.LocalTime dailySummaryTime, @org.jetbrains.annotations.NotNull()
    java.time.LocalTime tomorrowSummaryTime, boolean isSarcasticModeEnabled, @org.jetbrains.annotations.NotNull()
    java.lang.String criticalAlertSound, @org.jetbrains.annotations.NotNull()
    java.lang.String notificationSound, int minutesBeforeEventForReminder, boolean hasAcceptedPrivacyPolicy, boolean isFirstTimeUser, @org.jetbrains.annotations.NotNull()
    java.util.Set<java.lang.Long> preferredCalendars) {
        return null;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override()
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.String toString() {
        return null;
    }
}