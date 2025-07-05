package com.chapelotas.app.data;

/**
 * Módulo de Hilt que provee las implementaciones de los repositorios
 */
@dagger.Module()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\'\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\'J\u0010\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\nH\'J\u0010\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000eH\'J\u0010\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u0012H\'\u00a8\u0006\u0013"}, d2 = {"Lcom/chapelotas/app/data/DataModule;", "", "()V", "bindAIRepository", "Lcom/chapelotas/app/domain/repositories/AIRepository;", "aiRepositoryImpl", "Lcom/chapelotas/app/data/ai/AIRepositoryImpl;", "bindCalendarRepository", "Lcom/chapelotas/app/domain/repositories/CalendarRepository;", "calendarRepositoryImpl", "Lcom/chapelotas/app/data/calendar/CalendarRepositoryImpl;", "bindNotificationRepository", "Lcom/chapelotas/app/domain/repositories/NotificationRepository;", "notificationRepositoryImpl", "Lcom/chapelotas/app/data/notifications/NotificationRepositoryImpl;", "bindPreferencesRepository", "Lcom/chapelotas/app/domain/repositories/PreferencesRepository;", "preferencesRepositoryImpl", "Lcom/chapelotas/app/data/PreferencesRepositoryImpl;", "app_debug"})
@dagger.hilt.InstallIn(value = {dagger.hilt.components.SingletonComponent.class})
public abstract class DataModule {
    
    public DataModule() {
        super();
    }
    
    @dagger.Binds()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public abstract com.chapelotas.app.domain.repositories.CalendarRepository bindCalendarRepository(@org.jetbrains.annotations.NotNull()
    com.chapelotas.app.data.calendar.CalendarRepositoryImpl calendarRepositoryImpl);
    
    @dagger.Binds()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public abstract com.chapelotas.app.domain.repositories.NotificationRepository bindNotificationRepository(@org.jetbrains.annotations.NotNull()
    com.chapelotas.app.data.notifications.NotificationRepositoryImpl notificationRepositoryImpl);
    
    @dagger.Binds()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public abstract com.chapelotas.app.domain.repositories.AIRepository bindAIRepository(@org.jetbrains.annotations.NotNull()
    com.chapelotas.app.data.ai.AIRepositoryImpl aiRepositoryImpl);
    
    @dagger.Binds()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public abstract com.chapelotas.app.domain.repositories.PreferencesRepository bindPreferencesRepository(@org.jetbrains.annotations.NotNull()
    com.chapelotas.app.data.PreferencesRepositoryImpl preferencesRepositoryImpl);
}