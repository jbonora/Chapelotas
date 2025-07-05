package com.chapelotas.app.domain.usecases;

/**
 * Resultado del resumen de mañana
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0019\b\u0087\b\u0018\u00002\u00020\u0001B=\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u0012\u0006\u0010\u0007\u001a\u00020\b\u0012\b\u0010\t\u001a\u0004\u0018\u00010\u0006\u0012\u0006\u0010\n\u001a\u00020\u000b\u0012\u0006\u0010\f\u001a\u00020\b\u00a2\u0006\u0002\u0010\rJ\t\u0010\u0019\u001a\u00020\u0003H\u00c6\u0003J\u000f\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005H\u00c6\u0003J\t\u0010\u001b\u001a\u00020\bH\u00c6\u0003J\u000b\u0010\u001c\u001a\u0004\u0018\u00010\u0006H\u00c6\u0003J\t\u0010\u001d\u001a\u00020\u000bH\u00c6\u0003J\t\u0010\u001e\u001a\u00020\bH\u00c6\u0003JM\u0010\u001f\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\u000e\b\u0002\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u00052\b\b\u0002\u0010\u0007\u001a\u00020\b2\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\u00062\b\b\u0002\u0010\n\u001a\u00020\u000b2\b\b\u0002\u0010\f\u001a\u00020\bH\u00c6\u0001J\u0013\u0010 \u001a\u00020\b2\b\u0010!\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\"\u001a\u00020\u000bH\u00d6\u0001J\t\u0010#\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\n\u001a\u00020\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u0017\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u0013\u0010\t\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015R\u0011\u0010\f\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0015R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0018\u00a8\u0006$"}, d2 = {"Lcom/chapelotas/app/domain/usecases/TomorrowSummaryResult;", "", "summary", "", "events", "", "Lcom/chapelotas/app/domain/entities/CalendarEvent;", "hasEvents", "", "firstEvent", "criticalEventsCount", "", "needsEarlyAlarm", "(Ljava/lang/String;Ljava/util/List;ZLcom/chapelotas/app/domain/entities/CalendarEvent;IZ)V", "getCriticalEventsCount", "()I", "getEvents", "()Ljava/util/List;", "getFirstEvent", "()Lcom/chapelotas/app/domain/entities/CalendarEvent;", "getHasEvents", "()Z", "getNeedsEarlyAlarm", "getSummary", "()Ljava/lang/String;", "component1", "component2", "component3", "component4", "component5", "component6", "copy", "equals", "other", "hashCode", "toString", "app_debug"})
public final class TomorrowSummaryResult {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String summary = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.chapelotas.app.domain.entities.CalendarEvent> events = null;
    private final boolean hasEvents = false;
    @org.jetbrains.annotations.Nullable()
    private final com.chapelotas.app.domain.entities.CalendarEvent firstEvent = null;
    private final int criticalEventsCount = 0;
    private final boolean needsEarlyAlarm = false;
    
    public TomorrowSummaryResult(@org.jetbrains.annotations.NotNull()
    java.lang.String summary, @org.jetbrains.annotations.NotNull()
    java.util.List<com.chapelotas.app.domain.entities.CalendarEvent> events, boolean hasEvents, @org.jetbrains.annotations.Nullable()
    com.chapelotas.app.domain.entities.CalendarEvent firstEvent, int criticalEventsCount, boolean needsEarlyAlarm) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getSummary() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.chapelotas.app.domain.entities.CalendarEvent> getEvents() {
        return null;
    }
    
    public final boolean getHasEvents() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.chapelotas.app.domain.entities.CalendarEvent getFirstEvent() {
        return null;
    }
    
    public final int getCriticalEventsCount() {
        return 0;
    }
    
    public final boolean getNeedsEarlyAlarm() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component1() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.chapelotas.app.domain.entities.CalendarEvent> component2() {
        return null;
    }
    
    public final boolean component3() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.chapelotas.app.domain.entities.CalendarEvent component4() {
        return null;
    }
    
    public final int component5() {
        return 0;
    }
    
    public final boolean component6() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.chapelotas.app.domain.usecases.TomorrowSummaryResult copy(@org.jetbrains.annotations.NotNull()
    java.lang.String summary, @org.jetbrains.annotations.NotNull()
    java.util.List<com.chapelotas.app.domain.entities.CalendarEvent> events, boolean hasEvents, @org.jetbrains.annotations.Nullable()
    com.chapelotas.app.domain.entities.CalendarEvent firstEvent, int criticalEventsCount, boolean needsEarlyAlarm) {
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