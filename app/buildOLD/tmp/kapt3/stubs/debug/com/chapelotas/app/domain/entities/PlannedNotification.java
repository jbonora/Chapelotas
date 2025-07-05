package com.chapelotas.app.domain.entities;

/**
 * Notificación planeada por la IA (antes de ser programada)
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u001b\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0087\b\u0018\u00002\u00020\u0001BA\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u0012\b\u0010\f\u001a\u0004\u0018\u00010\u0007\u0012\b\b\u0002\u0010\r\u001a\u00020\u000e\u00a2\u0006\u0002\u0010\u000fJ\t\u0010\u001f\u001a\u00020\u0003H\u00c6\u0003J\t\u0010 \u001a\u00020\u0005H\u00c6\u0003J\t\u0010!\u001a\u00020\u0007H\u00c6\u0003J\t\u0010\"\u001a\u00020\tH\u00c6\u0003J\t\u0010#\u001a\u00020\u000bH\u00c6\u0003J\u000b\u0010$\u001a\u0004\u0018\u00010\u0007H\u00c6\u0003J\t\u0010%\u001a\u00020\u000eH\u00c6\u0003JQ\u0010&\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\t2\b\b\u0002\u0010\n\u001a\u00020\u000b2\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\u00072\b\b\u0002\u0010\r\u001a\u00020\u000eH\u00c6\u0001J\u0013\u0010\'\u001a\u00020\u000e2\b\u0010(\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010)\u001a\u00020*H\u00d6\u0001J\u0006\u0010+\u001a\u00020,J\t\u0010-\u001a\u00020\u0007H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u001a\u0010\r\u001a\u00020\u000eX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0012\u0010\u0013\"\u0004\b\u0014\u0010\u0015R\u0011\u0010\b\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0017R\u0013\u0010\f\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0019R\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u0019R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u001cR\u0011\u0010\n\u001a\u00020\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u001e\u00a8\u0006."}, d2 = {"Lcom/chapelotas/app/domain/entities/PlannedNotification;", "", "eventId", "", "suggestedTime", "Ljava/time/LocalDateTime;", "suggestedMessage", "", "priority", "Lcom/chapelotas/app/domain/entities/NotificationPriority;", "type", "Lcom/chapelotas/app/domain/entities/NotificationType;", "rationale", "hasBeenScheduled", "", "(JLjava/time/LocalDateTime;Ljava/lang/String;Lcom/chapelotas/app/domain/entities/NotificationPriority;Lcom/chapelotas/app/domain/entities/NotificationType;Ljava/lang/String;Z)V", "getEventId", "()J", "getHasBeenScheduled", "()Z", "setHasBeenScheduled", "(Z)V", "getPriority", "()Lcom/chapelotas/app/domain/entities/NotificationPriority;", "getRationale", "()Ljava/lang/String;", "getSuggestedMessage", "getSuggestedTime", "()Ljava/time/LocalDateTime;", "getType", "()Lcom/chapelotas/app/domain/entities/NotificationType;", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "copy", "equals", "other", "hashCode", "", "toScheduledNotification", "Lcom/chapelotas/app/domain/entities/ChapelotasNotification;", "toString", "app_debug"})
public final class PlannedNotification {
    private final long eventId = 0L;
    @org.jetbrains.annotations.NotNull()
    private final java.time.LocalDateTime suggestedTime = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String suggestedMessage = null;
    @org.jetbrains.annotations.NotNull()
    private final com.chapelotas.app.domain.entities.NotificationPriority priority = null;
    @org.jetbrains.annotations.NotNull()
    private final com.chapelotas.app.domain.entities.NotificationType type = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String rationale = null;
    private boolean hasBeenScheduled;
    
    public PlannedNotification(long eventId, @org.jetbrains.annotations.NotNull()
    java.time.LocalDateTime suggestedTime, @org.jetbrains.annotations.NotNull()
    java.lang.String suggestedMessage, @org.jetbrains.annotations.NotNull()
    com.chapelotas.app.domain.entities.NotificationPriority priority, @org.jetbrains.annotations.NotNull()
    com.chapelotas.app.domain.entities.NotificationType type, @org.jetbrains.annotations.Nullable()
    java.lang.String rationale, boolean hasBeenScheduled) {
        super();
    }
    
    public final long getEventId() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.time.LocalDateTime getSuggestedTime() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getSuggestedMessage() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.chapelotas.app.domain.entities.NotificationPriority getPriority() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.chapelotas.app.domain.entities.NotificationType getType() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getRationale() {
        return null;
    }
    
    public final boolean getHasBeenScheduled() {
        return false;
    }
    
    public final void setHasBeenScheduled(boolean p0) {
    }
    
    /**
     * Convierte a ChapelotasNotification para programar
     */
    @org.jetbrains.annotations.NotNull()
    public final com.chapelotas.app.domain.entities.ChapelotasNotification toScheduledNotification() {
        return null;
    }
    
    public final long component1() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.time.LocalDateTime component2() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component3() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.chapelotas.app.domain.entities.NotificationPriority component4() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.chapelotas.app.domain.entities.NotificationType component5() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component6() {
        return null;
    }
    
    public final boolean component7() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.chapelotas.app.domain.entities.PlannedNotification copy(long eventId, @org.jetbrains.annotations.NotNull()
    java.time.LocalDateTime suggestedTime, @org.jetbrains.annotations.NotNull()
    java.lang.String suggestedMessage, @org.jetbrains.annotations.NotNull()
    com.chapelotas.app.domain.entities.NotificationPriority priority, @org.jetbrains.annotations.NotNull()
    com.chapelotas.app.domain.entities.NotificationType type, @org.jetbrains.annotations.Nullable()
    java.lang.String rationale, boolean hasBeenScheduled) {
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