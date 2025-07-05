package com.chapelotas.app.domain.entities;

/**
 * Representa una notificación programada por Chapelotas
 * La IA decide cuándo y cómo notificar cada evento
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000:\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u001c\n\u0002\u0010\b\n\u0002\b\u0004\b\u0087\b\u0018\u00002\u00020\u0001BI\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\u0003\u0012\u0006\u0010\t\u001a\u00020\n\u0012\u0006\u0010\u000b\u001a\u00020\f\u0012\b\b\u0002\u0010\r\u001a\u00020\u000e\u0012\b\b\u0002\u0010\u000f\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\u0010J\t\u0010\u001f\u001a\u00020\u0003H\u00c6\u0003J\t\u0010 \u001a\u00020\u0005H\u00c6\u0003J\t\u0010!\u001a\u00020\u0007H\u00c6\u0003J\t\u0010\"\u001a\u00020\u0003H\u00c6\u0003J\t\u0010#\u001a\u00020\nH\u00c6\u0003J\t\u0010$\u001a\u00020\fH\u00c6\u0003J\t\u0010%\u001a\u00020\u000eH\u00c6\u0003J\t\u0010&\u001a\u00020\u0007H\u00c6\u0003JY\u0010\'\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\u00032\b\b\u0002\u0010\t\u001a\u00020\n2\b\b\u0002\u0010\u000b\u001a\u00020\f2\b\b\u0002\u0010\r\u001a\u00020\u000e2\b\b\u0002\u0010\u000f\u001a\u00020\u0007H\u00c6\u0001J\u0013\u0010(\u001a\u00020\u000e2\b\u0010)\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010*\u001a\u00020+H\u00d6\u0001J\u0006\u0010,\u001a\u00020\u0005J\u0006\u0010-\u001a\u00020\u000eJ\t\u0010.\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u000f\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014R\u0011\u0010\r\u001a\u00020\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0018R\u0011\u0010\b\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u0018R\u0011\u0010\t\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u001bR\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u0012R\u0011\u0010\u000b\u001a\u00020\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u001e\u00a8\u0006/"}, d2 = {"Lcom/chapelotas/app/domain/entities/ChapelotasNotification;", "", "id", "", "eventId", "", "scheduledTime", "Ljava/time/LocalDateTime;", "message", "priority", "Lcom/chapelotas/app/domain/entities/NotificationPriority;", "type", "Lcom/chapelotas/app/domain/entities/NotificationType;", "hasBeenShown", "", "createdAt", "(Ljava/lang/String;JLjava/time/LocalDateTime;Ljava/lang/String;Lcom/chapelotas/app/domain/entities/NotificationPriority;Lcom/chapelotas/app/domain/entities/NotificationType;ZLjava/time/LocalDateTime;)V", "getCreatedAt", "()Ljava/time/LocalDateTime;", "getEventId", "()J", "getHasBeenShown", "()Z", "getId", "()Ljava/lang/String;", "getMessage", "getPriority", "()Lcom/chapelotas/app/domain/entities/NotificationPriority;", "getScheduledTime", "getType", "()Lcom/chapelotas/app/domain/entities/NotificationType;", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "copy", "equals", "other", "hashCode", "", "minutesUntilShow", "shouldShowNow", "toString", "app_debug"})
public final class ChapelotasNotification {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String id = null;
    private final long eventId = 0L;
    @org.jetbrains.annotations.NotNull()
    private final java.time.LocalDateTime scheduledTime = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String message = null;
    @org.jetbrains.annotations.NotNull()
    private final com.chapelotas.app.domain.entities.NotificationPriority priority = null;
    @org.jetbrains.annotations.NotNull()
    private final com.chapelotas.app.domain.entities.NotificationType type = null;
    private final boolean hasBeenShown = false;
    @org.jetbrains.annotations.NotNull()
    private final java.time.LocalDateTime createdAt = null;
    
    public ChapelotasNotification(@org.jetbrains.annotations.NotNull()
    java.lang.String id, long eventId, @org.jetbrains.annotations.NotNull()
    java.time.LocalDateTime scheduledTime, @org.jetbrains.annotations.NotNull()
    java.lang.String message, @org.jetbrains.annotations.NotNull()
    com.chapelotas.app.domain.entities.NotificationPriority priority, @org.jetbrains.annotations.NotNull()
    com.chapelotas.app.domain.entities.NotificationType type, boolean hasBeenShown, @org.jetbrains.annotations.NotNull()
    java.time.LocalDateTime createdAt) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getId() {
        return null;
    }
    
    public final long getEventId() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.time.LocalDateTime getScheduledTime() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getMessage() {
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
    
    public final boolean getHasBeenShown() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.time.LocalDateTime getCreatedAt() {
        return null;
    }
    
    /**
     * Verifica si es momento de mostrar esta notificación
     */
    public final boolean shouldShowNow() {
        return false;
    }
    
    /**
     * Minutos hasta mostrar la notificación
     */
    public final long minutesUntilShow() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component1() {
        return null;
    }
    
    public final long component2() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.time.LocalDateTime component3() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component4() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.chapelotas.app.domain.entities.NotificationPriority component5() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.chapelotas.app.domain.entities.NotificationType component6() {
        return null;
    }
    
    public final boolean component7() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.time.LocalDateTime component8() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.chapelotas.app.domain.entities.ChapelotasNotification copy(@org.jetbrains.annotations.NotNull()
    java.lang.String id, long eventId, @org.jetbrains.annotations.NotNull()
    java.time.LocalDateTime scheduledTime, @org.jetbrains.annotations.NotNull()
    java.lang.String message, @org.jetbrains.annotations.NotNull()
    com.chapelotas.app.domain.entities.NotificationPriority priority, @org.jetbrains.annotations.NotNull()
    com.chapelotas.app.domain.entities.NotificationType type, boolean hasBeenShown, @org.jetbrains.annotations.NotNull()
    java.time.LocalDateTime createdAt) {
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