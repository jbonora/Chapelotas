package com.chapelotas.app.di

import android.content.Context
import com.chapelotas.app.data.database.TaskDatabase
import com.chapelotas.app.data.database.TaskDao
import com.chapelotas.app.domain.debug.DebugLog
import com.chapelotas.app.domain.events.EventBus
import com.chapelotas.app.domain.repositories.CalendarRepository
import com.chapelotas.app.domain.repositories.NotificationRepository
import com.chapelotas.app.domain.repositories.PreferencesRepository
import com.chapelotas.app.domain.repositories.TaskRepository
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
    fun provideCalendarSyncUseCase(
        calendarRepository: CalendarRepository,
        taskRepository: TaskRepository,
        eventBus: EventBus,
        reminderEngine: ReminderEngine,
        debugLog: DebugLog
    ): CalendarSyncUseCase {
        return CalendarSyncUseCase(calendarRepository, taskRepository, eventBus, reminderEngine, debugLog)
    }

    // --- INICIO DE LA CORRECCIÓN ---
    @Provides
    @Singleton
    fun provideReminderEngine(
        taskRepository: TaskRepository,
        notificationRepository: NotificationRepository,
        preferencesRepository: PreferencesRepository, // <-- Añadimos el repositorio de preferencias
        debugLog: DebugLog
    ): ReminderEngine {
        // <-- Se lo pasamos al constructor
        return ReminderEngine(taskRepository, notificationRepository, preferencesRepository, debugLog)
    }
    // --- FIN DE LA CORRECCIÓN ---
}