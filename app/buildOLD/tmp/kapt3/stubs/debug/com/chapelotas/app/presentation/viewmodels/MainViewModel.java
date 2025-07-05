package com.chapelotas.app.presentation.viewmodels;

/**
 * ViewModel para la pantalla principal
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0010\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\u0002\b\u0007\u0018\u00002\u00020\u0001B/\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u00a2\u0006\u0002\u0010\fJ\b\u0010 \u001a\u00020!H\u0002J\u0006\u0010\"\u001a\u00020!J\u0006\u0010#\u001a\u00020!J\u0006\u0010$\u001a\u00020!J\u0006\u0010%\u001a\u00020!J\u0010\u0010&\u001a\u00020!2\b\b\u0002\u0010\'\u001a\u00020(J\u0016\u0010)\u001a\u00020!2\u0006\u0010*\u001a\u00020+2\u0006\u0010,\u001a\u00020(R\u0014\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u000f0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0010\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00120\u00110\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u000f0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00150\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u000f0\u0017\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0019R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\u001a\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00120\u00110\u0017\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u0019R\u0017\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u000f0\u0017\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u0019R\u0017\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\u00150\u0017\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010\u0019\u00a8\u0006-"}, d2 = {"Lcom/chapelotas/app/presentation/viewmodels/MainViewModel;", "Landroidx/lifecycle/ViewModel;", "getDailySummaryUseCase", "Lcom/chapelotas/app/domain/usecases/GetDailySummaryUseCase;", "getTomorrowSummaryUseCase", "Lcom/chapelotas/app/domain/usecases/GetTomorrowSummaryUseCase;", "scheduleNotificationsUseCase", "Lcom/chapelotas/app/domain/usecases/ScheduleNotificationsUseCase;", "markEventAsCriticalUseCase", "Lcom/chapelotas/app/domain/usecases/MarkEventAsCriticalUseCase;", "setupInitialConfigurationUseCase", "Lcom/chapelotas/app/domain/usecases/SetupInitialConfigurationUseCase;", "(Lcom/chapelotas/app/domain/usecases/GetDailySummaryUseCase;Lcom/chapelotas/app/domain/usecases/GetTomorrowSummaryUseCase;Lcom/chapelotas/app/domain/usecases/ScheduleNotificationsUseCase;Lcom/chapelotas/app/domain/usecases/MarkEventAsCriticalUseCase;Lcom/chapelotas/app/domain/usecases/SetupInitialConfigurationUseCase;)V", "_dailySummary", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "_todayEvents", "", "Lcom/chapelotas/app/domain/entities/CalendarEvent;", "_tomorrowSummary", "_uiState", "Lcom/chapelotas/app/presentation/viewmodels/MainUiState;", "dailySummary", "Lkotlinx/coroutines/flow/StateFlow;", "getDailySummary", "()Lkotlinx/coroutines/flow/StateFlow;", "todayEvents", "getTodayEvents", "tomorrowSummary", "getTomorrowSummary", "uiState", "getUiState", "checkInitialSetup", "", "clearError", "loadDailySummary", "loadTomorrowSummary", "onPermissionsGranted", "scheduleNotifications", "forTomorrow", "", "toggleEventCritical", "eventId", "", "isCritical", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class MainViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.chapelotas.app.domain.usecases.GetDailySummaryUseCase getDailySummaryUseCase = null;
    @org.jetbrains.annotations.NotNull()
    private final com.chapelotas.app.domain.usecases.GetTomorrowSummaryUseCase getTomorrowSummaryUseCase = null;
    @org.jetbrains.annotations.NotNull()
    private final com.chapelotas.app.domain.usecases.ScheduleNotificationsUseCase scheduleNotificationsUseCase = null;
    @org.jetbrains.annotations.NotNull()
    private final com.chapelotas.app.domain.usecases.MarkEventAsCriticalUseCase markEventAsCriticalUseCase = null;
    @org.jetbrains.annotations.NotNull()
    private final com.chapelotas.app.domain.usecases.SetupInitialConfigurationUseCase setupInitialConfigurationUseCase = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.chapelotas.app.presentation.viewmodels.MainUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.chapelotas.app.presentation.viewmodels.MainUiState> uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.chapelotas.app.domain.entities.CalendarEvent>> _todayEvents = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.chapelotas.app.domain.entities.CalendarEvent>> todayEvents = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.String> _dailySummary = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.String> dailySummary = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.String> _tomorrowSummary = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.String> tomorrowSummary = null;
    
    @javax.inject.Inject()
    public MainViewModel(@org.jetbrains.annotations.NotNull()
    com.chapelotas.app.domain.usecases.GetDailySummaryUseCase getDailySummaryUseCase, @org.jetbrains.annotations.NotNull()
    com.chapelotas.app.domain.usecases.GetTomorrowSummaryUseCase getTomorrowSummaryUseCase, @org.jetbrains.annotations.NotNull()
    com.chapelotas.app.domain.usecases.ScheduleNotificationsUseCase scheduleNotificationsUseCase, @org.jetbrains.annotations.NotNull()
    com.chapelotas.app.domain.usecases.MarkEventAsCriticalUseCase markEventAsCriticalUseCase, @org.jetbrains.annotations.NotNull()
    com.chapelotas.app.domain.usecases.SetupInitialConfigurationUseCase setupInitialConfigurationUseCase) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.chapelotas.app.presentation.viewmodels.MainUiState> getUiState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.chapelotas.app.domain.entities.CalendarEvent>> getTodayEvents() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.String> getDailySummary() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.String> getTomorrowSummary() {
        return null;
    }
    
    /**
     * Verifica si es la primera vez y realiza configuración inicial
     */
    private final void checkInitialSetup() {
    }
    
    /**
     * Carga el resumen del día
     */
    public final void loadDailySummary() {
    }
    
    /**
     * Carga el resumen de mañana
     */
    public final void loadTomorrowSummary() {
    }
    
    /**
     * Programa las notificaciones del día
     */
    public final void scheduleNotifications(boolean forTomorrow) {
    }
    
    /**
     * Marca/desmarca un evento como crítico
     */
    public final void toggleEventCritical(long eventId, boolean isCritical) {
    }
    
    /**
     * Limpia el mensaje de error
     */
    public final void clearError() {
    }
    
    /**
     * Marca que los permisos fueron otorgados
     */
    public final void onPermissionsGranted() {
    }
}