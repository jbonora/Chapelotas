package com.chapelotas.app;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000@\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\t\n\u0002\b\u0003\u001a\u001e\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00010\u0005H\u0007\u001a\u0094\u0001\u0010\u0006\u001a\u00020\u00012\u0006\u0010\u0007\u001a\u00020\b2\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u00030\n2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\f2\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\u0012\u0010\u0011\u001a\u000e\u0012\u0004\u0012\u00020\u0013\u0012\u0004\u0012\u00020\u00010\u00122\u0018\u0010\u0014\u001a\u0014\u0012\u0004\u0012\u00020\u0016\u0012\u0004\u0012\u00020\u0013\u0012\u0004\u0012\u00020\u00010\u00152\f\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00010\u0005H\u0007\u001a\u0016\u0010\u0018\u001a\u00020\u00012\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00010\u0005H\u0007\u00a8\u0006\u0019"}, d2 = {"EventCard", "", "event", "Lcom/chapelotas/app/domain/entities/CalendarEvent;", "onToggleCritical", "Lkotlin/Function0;", "MainScreen", "uiState", "Lcom/chapelotas/app/presentation/viewmodels/MainUiState;", "todayEvents", "", "dailySummary", "", "tomorrowSummary", "onRequestPermissions", "onLoadDailySummary", "onLoadTomorrowSummary", "onScheduleNotifications", "Lkotlin/Function1;", "", "onToggleEventCritical", "Lkotlin/Function2;", "", "onDismissError", "PermissionRequestScreen", "app_debug"})
public final class MainActivityKt {
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void MainScreen(@org.jetbrains.annotations.NotNull()
    com.chapelotas.app.presentation.viewmodels.MainUiState uiState, @org.jetbrains.annotations.NotNull()
    java.util.List<com.chapelotas.app.domain.entities.CalendarEvent> todayEvents, @org.jetbrains.annotations.NotNull()
    java.lang.String dailySummary, @org.jetbrains.annotations.NotNull()
    java.lang.String tomorrowSummary, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onRequestPermissions, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onLoadDailySummary, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onLoadTomorrowSummary, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> onScheduleNotifications, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function2<? super java.lang.Long, ? super java.lang.Boolean, kotlin.Unit> onToggleEventCritical, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onDismissError) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void EventCard(@org.jetbrains.annotations.NotNull()
    com.chapelotas.app.domain.entities.CalendarEvent event, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onToggleCritical) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void PermissionRequestScreen(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onRequestPermissions) {
    }
}