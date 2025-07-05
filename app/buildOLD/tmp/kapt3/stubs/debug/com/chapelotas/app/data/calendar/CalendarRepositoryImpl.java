package com.chapelotas.app.data.calendar;

/**
 * Implementación del repositorio de calendario usando el ContentProvider de Android
 */
@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000j\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010#\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\"\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\f\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0007\u0018\u0000 ,2\u00020\u0001:\u0001,B\u0019\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\b\u0010\f\u001a\u00020\rH\u0002J\u001a\u0010\u000e\u001a\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\u00100\u000fH\u0096@\u00a2\u0006\u0002\u0010\u0011J\u0014\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0013H\u0096@\u00a2\u0006\u0002\u0010\u0011J,\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00160\u00152\u0006\u0010\u0017\u001a\u00020\u00182\u000e\u0010\u0019\u001a\n\u0012\u0004\u0012\u00020\u000b\u0018\u00010\u0013H\u0096@\u00a2\u0006\u0002\u0010\u001aJ4\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u00160\u00152\u0006\u0010\u001c\u001a\u00020\u00182\u0006\u0010\u001d\u001a\u00020\u00182\u000e\u0010\u0019\u001a\n\u0012\u0004\u0012\u00020\u000b\u0018\u00010\u0013H\u0096@\u00a2\u0006\u0002\u0010\u001eJ.\u0010\u001f\u001a\b\u0012\u0004\u0012\u00020\u00160\u00152\u0006\u0010 \u001a\u00020\u000b2\u0006\u0010!\u001a\u00020\u000b2\u000e\u0010\u0019\u001a\n\u0012\u0004\u0012\u00020\u000b\u0018\u00010\u0013H\u0002J\u001e\u0010\"\u001a\u00020\r2\u0006\u0010#\u001a\u00020\u000b2\u0006\u0010$\u001a\u00020%H\u0096@\u00a2\u0006\u0002\u0010&J\u000e\u0010\'\u001a\b\u0012\u0004\u0012\u00020\r0(H\u0016J\u0010\u0010)\u001a\u00020\u00162\u0006\u0010*\u001a\u00020+H\u0002R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006-"}, d2 = {"Lcom/chapelotas/app/data/calendar/CalendarRepositoryImpl;", "Lcom/chapelotas/app/domain/repositories/CalendarRepository;", "context", "Landroid/content/Context;", "preferencesRepository", "Lcom/chapelotas/app/domain/repositories/PreferencesRepository;", "(Landroid/content/Context;Lcom/chapelotas/app/domain/repositories/PreferencesRepository;)V", "contentResolver", "Landroid/content/ContentResolver;", "criticalEventIds", "", "", "checkCalendarPermission", "", "getAvailableCalendars", "", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getCriticalEventIds", "", "getEventsForDate", "", "Lcom/chapelotas/app/domain/entities/CalendarEvent;", "date", "Ljava/time/LocalDate;", "calendarIds", "(Ljava/time/LocalDate;Ljava/util/Set;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getEventsInRange", "startDate", "endDate", "(Ljava/time/LocalDate;Ljava/time/LocalDate;Ljava/util/Set;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getEventsInTimeRange", "startMillis", "endMillis", "markEventAsCritical", "eventId", "isCritical", "", "(JZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "observeCalendarChanges", "Lkotlinx/coroutines/flow/Flow;", "parseEventFromCursor", "cursor", "Landroid/database/Cursor;", "Companion", "app_debug"})
public final class CalendarRepositoryImpl implements com.chapelotas.app.domain.repositories.CalendarRepository {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final com.chapelotas.app.domain.repositories.PreferencesRepository preferencesRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final android.content.ContentResolver contentResolver = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Set<java.lang.Long> criticalEventIds = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String[] CALENDAR_PROJECTION = {"_id", "calendar_displayName", "account_name", "calendar_color", "visible"};
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String[] EVENT_PROJECTION = {"_id", "title", "description", "dtstart", "dtend", "allDay", "eventLocation", "calendar_id", "calendar_displayName"};
    private static final int CALENDAR_ID_INDEX = 0;
    private static final int CALENDAR_NAME_INDEX = 1;
    private static final int EVENT_ID_INDEX = 0;
    private static final int EVENT_TITLE_INDEX = 1;
    private static final int EVENT_DESCRIPTION_INDEX = 2;
    private static final int EVENT_START_INDEX = 3;
    private static final int EVENT_END_INDEX = 4;
    private static final int EVENT_ALL_DAY_INDEX = 5;
    private static final int EVENT_LOCATION_INDEX = 6;
    private static final int EVENT_CALENDAR_ID_INDEX = 7;
    private static final int EVENT_CALENDAR_NAME_INDEX = 8;
    @org.jetbrains.annotations.NotNull()
    public static final com.chapelotas.app.data.calendar.CalendarRepositoryImpl.Companion Companion = null;
    
    @javax.inject.Inject()
    public CalendarRepositoryImpl(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.chapelotas.app.domain.repositories.PreferencesRepository preferencesRepository) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object getAvailableCalendars(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.Map<java.lang.Long, java.lang.String>> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object getEventsForDate(@org.jetbrains.annotations.NotNull()
    java.time.LocalDate date, @org.jetbrains.annotations.Nullable()
    java.util.Set<java.lang.Long> calendarIds, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.chapelotas.app.domain.entities.CalendarEvent>> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object getEventsInRange(@org.jetbrains.annotations.NotNull()
    java.time.LocalDate startDate, @org.jetbrains.annotations.NotNull()
    java.time.LocalDate endDate, @org.jetbrains.annotations.Nullable()
    java.util.Set<java.lang.Long> calendarIds, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.chapelotas.app.domain.entities.CalendarEvent>> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.Flow<kotlin.Unit> observeCalendarChanges() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object markEventAsCritical(long eventId, boolean isCritical, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object getCriticalEventIds(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.Set<java.lang.Long>> $completion) {
        return null;
    }
    
    /**
     * Obtiene eventos en un rango de tiempo
     */
    private final java.util.List<com.chapelotas.app.domain.entities.CalendarEvent> getEventsInTimeRange(long startMillis, long endMillis, java.util.Set<java.lang.Long> calendarIds) {
        return null;
    }
    
    /**
     * Parsea un evento desde el cursor
     */
    private final com.chapelotas.app.domain.entities.CalendarEvent parseEventFromCursor(android.database.Cursor cursor) {
        return null;
    }
    
    /**
     * Verifica que tenemos permisos de calendario
     */
    private final void checkCalendarPermission() {
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object getTodayEvents(@org.jetbrains.annotations.Nullable()
    java.util.Set<java.lang.Long> calendarIds, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.chapelotas.app.domain.entities.CalendarEvent>> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object getTomorrowEvents(@org.jetbrains.annotations.Nullable()
    java.util.Set<java.lang.Long> calendarIds, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.chapelotas.app.domain.entities.CalendarEvent>> $completion) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u0011\n\u0002\u0010\u000e\n\u0002\b\f\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u0007X\u0082\u0004\u00a2\u0006\u0004\n\u0002\u0010\tR\u000e\u0010\n\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\b0\u0007X\u0082\u0004\u00a2\u0006\u0004\n\u0002\u0010\tR\u000e\u0010\u0012\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0014"}, d2 = {"Lcom/chapelotas/app/data/calendar/CalendarRepositoryImpl$Companion;", "", "()V", "CALENDAR_ID_INDEX", "", "CALENDAR_NAME_INDEX", "CALENDAR_PROJECTION", "", "", "[Ljava/lang/String;", "EVENT_ALL_DAY_INDEX", "EVENT_CALENDAR_ID_INDEX", "EVENT_CALENDAR_NAME_INDEX", "EVENT_DESCRIPTION_INDEX", "EVENT_END_INDEX", "EVENT_ID_INDEX", "EVENT_LOCATION_INDEX", "EVENT_PROJECTION", "EVENT_START_INDEX", "EVENT_TITLE_INDEX", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}