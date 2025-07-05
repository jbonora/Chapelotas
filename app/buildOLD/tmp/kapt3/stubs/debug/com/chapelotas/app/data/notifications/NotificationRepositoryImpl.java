package com.chapelotas.app.data.notifications;

/**
 * Implementación del repositorio de notificaciones
 * Usa WorkManager para programar y Foreground Service para persistencia
 */
@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000l\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010\b\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u000f\b\u0007\u0018\u0000 82\u00020\u0001:\u00018B\u0011\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0010\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u0012H\u0002J\u0016\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u0016H\u0096@\u00a2\u0006\u0002\u0010\u0017J\u0016\u0010\u0018\u001a\u00020\u00142\u0006\u0010\u0019\u001a\u00020\u0010H\u0096@\u00a2\u0006\u0002\u0010\u001aJ\u0016\u0010\u001b\u001a\u00020\u00142\u0006\u0010\u001c\u001a\u00020\u001dH\u0096@\u00a2\u0006\u0002\u0010\u001eJ\b\u0010\u001f\u001a\u00020\u0014H\u0002J\u001c\u0010 \u001a\b\u0012\u0004\u0012\u00020\b0\u00072\u0006\u0010\u0019\u001a\u00020\u0010H\u0096@\u00a2\u0006\u0002\u0010\u001aJ\u0014\u0010!\u001a\b\u0012\u0004\u0012\u00020\b0\u0007H\u0096@\u00a2\u0006\u0002\u0010\"J\u000e\u0010#\u001a\u00020$H\u0096@\u00a2\u0006\u0002\u0010\"J\u0010\u0010%\u001a\u00020\u001d2\u0006\u0010&\u001a\u00020\'H\u0002J\u0016\u0010(\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u0016H\u0096@\u00a2\u0006\u0002\u0010\u0017J\u0014\u0010)\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u00070*H\u0016J\u0010\u0010+\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u0016H\u0002J\u001c\u0010,\u001a\u00020\u00142\f\u0010-\u001a\b\u0012\u0004\u0012\u00020\b0\u0007H\u0096@\u00a2\u0006\u0002\u0010.J\u0016\u0010/\u001a\u00020\u00142\u0006\u00100\u001a\u00020\bH\u0096@\u00a2\u0006\u0002\u00101J\u0010\u00102\u001a\u00020\u00142\u0006\u00100\u001a\u00020\bH\u0002J\u0016\u00103\u001a\u00020\u00142\u0006\u00100\u001a\u00020\bH\u0096@\u00a2\u0006\u0002\u00101J\u0010\u00104\u001a\u00020\u00142\u0006\u00100\u001a\u00020\bH\u0002J\u000e\u00105\u001a\u00020\u0014H\u0096@\u00a2\u0006\u0002\u0010\"J\u000e\u00106\u001a\u00020\u0014H\u0096@\u00a2\u0006\u0002\u0010\"J\u0010\u00107\u001a\u00020\u00142\u0006\u00100\u001a\u00020\bH\u0002R\u001a\u0010\u0005\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u00069"}, d2 = {"Lcom/chapelotas/app/data/notifications/NotificationRepositoryImpl;", "Lcom/chapelotas/app/domain/repositories/NotificationRepository;", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "_notificationsFlow", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "Lcom/chapelotas/app/domain/entities/ChapelotasNotification;", "gson", "Lcom/google/gson/Gson;", "notificationManager", "Landroidx/core/app/NotificationManagerCompat;", "workManager", "Landroidx/work/WorkManager;", "calculateDelay", "", "scheduledTime", "Ljava/time/LocalDateTime;", "cancelNotification", "", "notificationId", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "cancelNotificationsForEvent", "eventId", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "cleanOldNotifications", "daysToKeep", "", "(ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "createNotificationChannels", "getNotificationsForEvent", "getPendingNotifications", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "isNotificationServiceRunning", "", "mapPriority", "priority", "Lcom/chapelotas/app/domain/entities/NotificationPriority;", "markAsShown", "observeNotifications", "Lkotlinx/coroutines/flow/Flow;", "removeFromNotificationsList", "scheduleMultipleNotifications", "notifications", "(Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "scheduleNotification", "notification", "(Lcom/chapelotas/app/domain/entities/ChapelotasNotification;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "showCriticalNotification", "showImmediateNotification", "showStandardNotification", "startNotificationService", "stopNotificationService", "updateNotificationsList", "Companion", "app_debug"})
public final class NotificationRepositoryImpl implements com.chapelotas.app.domain.repositories.NotificationRepository {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.work.WorkManager workManager = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.core.app.NotificationManagerCompat notificationManager = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.gson.Gson gson = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.chapelotas.app.domain.entities.ChapelotasNotification>> _notificationsFlow = null;
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String CHANNEL_GENERAL = "chapelotas_general";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String CHANNEL_CRITICAL = "chapelotas_critical";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String CHANNEL_SERVICE = "chapelotas_service";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String WORK_TAG_PREFIX = "chapelotas_notification_";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String NOTIFICATION_DATA_KEY = "notification_data";
    public static final int FOREGROUND_SERVICE_ID = 1337;
    private static int notificationIdCounter = 2000;
    @org.jetbrains.annotations.NotNull()
    public static final com.chapelotas.app.data.notifications.NotificationRepositoryImpl.Companion Companion = null;
    
    @javax.inject.Inject()
    public NotificationRepositoryImpl(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object scheduleNotification(@org.jetbrains.annotations.NotNull()
    com.chapelotas.app.domain.entities.ChapelotasNotification notification, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object scheduleMultipleNotifications(@org.jetbrains.annotations.NotNull()
    java.util.List<com.chapelotas.app.domain.entities.ChapelotasNotification> notifications, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object cancelNotification(@org.jetbrains.annotations.NotNull()
    java.lang.String notificationId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object cancelNotificationsForEvent(long eventId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object getPendingNotifications(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.chapelotas.app.domain.entities.ChapelotasNotification>> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object getNotificationsForEvent(long eventId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.chapelotas.app.domain.entities.ChapelotasNotification>> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object markAsShown(@org.jetbrains.annotations.NotNull()
    java.lang.String notificationId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object cleanOldNotifications(int daysToKeep, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.Flow<java.util.List<com.chapelotas.app.domain.entities.ChapelotasNotification>> observeNotifications() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object showImmediateNotification(@org.jetbrains.annotations.NotNull()
    com.chapelotas.app.domain.entities.ChapelotasNotification notification, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object isNotificationServiceRunning(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object startNotificationService(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object stopNotificationService(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * Crea los canales de notificación necesarios
     */
    private final void createNotificationChannels() {
    }
    
    /**
     * Muestra una notificación estándar
     */
    private final void showStandardNotification(com.chapelotas.app.domain.entities.ChapelotasNotification notification) {
    }
    
    /**
     * Muestra una notificación crítica (pantalla completa)
     */
    private final void showCriticalNotification(com.chapelotas.app.domain.entities.ChapelotasNotification notification) {
    }
    
    /**
     * Calcula el delay para programar la notificación
     */
    private final long calculateDelay(java.time.LocalDateTime scheduledTime) {
        return 0L;
    }
    
    /**
     * Mapea la prioridad del dominio a la prioridad de Android
     */
    private final int mapPriority(com.chapelotas.app.domain.entities.NotificationPriority priority) {
        return 0;
    }
    
    /**
     * Actualiza la lista de notificaciones en memoria
     */
    private final void updateNotificationsList(com.chapelotas.app.domain.entities.ChapelotasNotification notification) {
    }
    
    /**
     * Remueve una notificación de la lista
     */
    private final void removeFromNotificationsList(java.lang.String notificationId) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0004\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\f"}, d2 = {"Lcom/chapelotas/app/data/notifications/NotificationRepositoryImpl$Companion;", "", "()V", "CHANNEL_CRITICAL", "", "CHANNEL_GENERAL", "CHANNEL_SERVICE", "FOREGROUND_SERVICE_ID", "", "NOTIFICATION_DATA_KEY", "WORK_TAG_PREFIX", "notificationIdCounter", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}