package com.chapelotas.app.data

import android.content.Context
import com.chapelotas.app.data.ai.AIRepositoryImpl
import com.chapelotas.app.data.calendar.CalendarRepositoryImpl
import com.chapelotas.app.data.database.ChapelotasDatabase
import com.chapelotas.app.data.database.daos.*
import com.chapelotas.app.data.notifications.NotificationRepositoryImpl
import com.chapelotas.app.domain.events.ChapelotasEventBus
import com.chapelotas.app.domain.repositories.AIRepository
import com.chapelotas.app.domain.repositories.CalendarRepository
import com.chapelotas.app.domain.repositories.NotificationRepository
import com.chapelotas.app.domain.repositories.PreferencesRepository
import com.chapelotas.app.domain.usecases.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Módulo de Hilt - VERSIÓN FINAL con Room y todos los servicios
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindCalendarRepository(
        calendarRepositoryImpl: CalendarRepositoryImpl
    ): CalendarRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        notificationRepositoryImpl: NotificationRepositoryImpl
    ): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindAIRepository(
        aiRepositoryImpl: AIRepositoryImpl
    ): AIRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(
        preferencesRepositoryImpl: PreferencesRepositoryImpl
    ): PreferencesRepository

    companion object {

        // ===== ROOM DATABASE =====
        @Provides
        @Singleton
        fun provideDatabase(
            @ApplicationContext context: Context
        ): ChapelotasDatabase {
            return ChapelotasDatabase.getDatabase(
                context,
                CoroutineScope(SupervisorJob() + Dispatchers.IO)
            )
        }

        @Provides
        @Singleton
        fun provideDayPlanDao(database: ChapelotasDatabase): DayPlanDao {
            return database.dayPlanDao()
        }

        @Provides
        @Singleton
        fun provideEventPlanDao(database: ChapelotasDatabase): EventPlanDao {
            return database.eventPlanDao()
        }

        @Provides
        @Singleton
        fun provideNotificationDao(database: ChapelotasDatabase): NotificationDao {
            return database.notificationDao()
        }

        @Provides
        @Singleton
        fun provideConflictDao(database: ChapelotasDatabase): ConflictDao {
            return database.conflictDao()
        }

        @Provides
        @Singleton
        fun provideNotificationLogDao(database: ChapelotasDatabase): NotificationLogDao {
            return database.notificationLogDao()
        }

        // ===== CORE SERVICES =====
        @Provides
        @Singleton
        fun provideGson(): Gson {
            return GsonBuilder()
                .create()
        }

        @Provides
        @Singleton
        fun provideApplicationScope(): CoroutineScope {
            return CoroutineScope(SupervisorJob() + Dispatchers.Default)
        }

        @Provides
        @Singleton
        fun provideEventBus(): ChapelotasEventBus {
            return ChapelotasEventBus()
        }

        // ===== USE CASES =====
        @Provides
        @Singleton
        fun provideMasterPlanController(
            aiRepository: AIRepository,
            gson: Gson
        ): MasterPlanController {
            return MasterPlanController(aiRepository, gson)
        }

        @Provides
        @Singleton
        fun provideUnifiedMonkeyService(
            @ApplicationContext context: Context,
            database: ChapelotasDatabase,
            eventBus: ChapelotasEventBus,
            notificationRepository: NotificationRepository,
            masterPlanController: MasterPlanController,
            scope: CoroutineScope
        ): UnifiedMonkeyService {
            return UnifiedMonkeyService(
                context = context,
                database = database,
                eventBus = eventBus,
                notificationRepository = notificationRepository,
                masterPlanController = masterPlanController,
                scope = scope
            )
        }

        @Provides
        @Singleton
        fun provideNotificationActionHandler(
            database: ChapelotasDatabase,
            eventBus: ChapelotasEventBus,
            notificationManagerCompat: androidx.core.app.NotificationManagerCompat,
            scope: CoroutineScope
        ): NotificationActionHandler {
            return NotificationActionHandler(
                database = database,
                eventBus = eventBus,
                notificationManager = notificationManagerCompat,
                scope = scope
            )
        }

        @Provides
        @Singleton
        fun provideNotificationManagerCompat(
            @ApplicationContext context: Context
        ): androidx.core.app.NotificationManagerCompat {
            return androidx.core.app.NotificationManagerCompat.from(context)
        }

        // ===== LEGACY - Para compatibilidad temporal =====
        @Provides
        @Singleton
        fun provideMonkeyCheckerService(
            @ApplicationContext context: Context,
            masterPlanController: MasterPlanController,
            notificationRepository: NotificationRepository,
            gson: Gson
        ): MonkeyCheckerService {
            return MonkeyCheckerService(
                context = context,
                masterPlanController = masterPlanController,
                notificationRepository = notificationRepository,
                gson = gson
            )
        }
    }
}