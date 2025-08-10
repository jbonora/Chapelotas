package com.chapelotas.app.di

import android.content.Context
import com.chapelotas.app.battery.HuaweiWakeUpManager
import com.chapelotas.app.data.database.TaskDatabase
import com.chapelotas.app.data.database.TaskDao
import com.chapelotas.app.domain.debug.DebugLog
import com.chapelotas.app.domain.events.EventBus
import com.chapelotas.app.domain.personality.PersonalityProvider
import com.chapelotas.app.domain.permissions.AppStatusManager
import com.chapelotas.app.domain.repositories.CalendarRepository
import com.chapelotas.app.domain.repositories.NotificationRepository
import com.chapelotas.app.domain.repositories.PreferencesRepository
import com.chapelotas.app.domain.repositories.TaskRepository
import com.chapelotas.app.domain.usecases.AlarmSchedulerUseCase
import com.chapelotas.app.domain.usecases.CalendarSyncUseCase
import com.chapelotas.app.domain.usecases.ReminderEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideTaskDatabase(@ApplicationContext context: Context): TaskDatabase {
        return TaskDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideTaskDao(database: TaskDatabase): TaskDao {
        return database.taskDao()
    }

    @Provides
    @Singleton
    fun provideDebugLog(): DebugLog {
        return DebugLog
    }

    @Provides
    @Singleton
    fun provideEventBus(): EventBus {
        return EventBus()
    }

    @Provides
    @Singleton
    fun provideHuaweiWakeUpManager(
        @ApplicationContext context: Context,
        debugLog: DebugLog
    ): HuaweiWakeUpManager {
        return HuaweiWakeUpManager(context, debugLog)
    }

    @Provides
    @Singleton
    fun provideAlarmSchedulerUseCase(
        @ApplicationContext context: Context,
        taskRepository: TaskRepository,
        preferencesRepository: PreferencesRepository,
        debugLog: DebugLog,
        eventBus: EventBus
    ): AlarmSchedulerUseCase {
        return AlarmSchedulerUseCase(
            context,
            taskRepository,
            preferencesRepository,
            debugLog,
            eventBus
        )
    }

    @Provides
    @Singleton
    fun provideCalendarSyncUseCase(
        calendarRepository: CalendarRepository,
        taskRepository: TaskRepository,
        reminderEngine: ReminderEngine,
        debugLog: DebugLog,
        appStatusManager: AppStatusManager,
        notificationRepository: NotificationRepository,
        eventBus: EventBus
        // El @ApplicationContext fue eliminado de aquí porque ya estaba en el constructor del UseCase
    ): CalendarSyncUseCase {
        return CalendarSyncUseCase(
            calendarRepository,
            taskRepository,
            reminderEngine,
            debugLog,
            appStatusManager,
            notificationRepository,
            eventBus
            // El context se pasa directamente al constructor, ya no es necesario aquí
        )
    }

    @Provides
    @Singleton
    fun provideReminderEngine(
        taskRepository: TaskRepository,
        notificationRepository: NotificationRepository,
        preferencesRepository: PreferencesRepository,
        debugLog: DebugLog,
        personalityProvider: PersonalityProvider,
        huaweiWakeUpManager: HuaweiWakeUpManager,
        @ApplicationContext context: Context
    ): ReminderEngine {
        return ReminderEngine(
            taskRepository,
            notificationRepository,
            preferencesRepository,
            debugLog,
            personalityProvider,
            huaweiWakeUpManager,
            context
        )
    }
}