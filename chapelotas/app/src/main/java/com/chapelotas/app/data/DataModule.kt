package com.chapelotas.app.data

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.room.Room
import com.chapelotas.app.data.ai.AIRepositoryImpl
import com.chapelotas.app.data.calendar.CalendarRepositoryImpl
import com.chapelotas.app.data.database.ChapelotasDatabase
import com.chapelotas.app.data.database.daos.*
import com.chapelotas.app.data.notifications.NotificationRepositoryImpl
import com.chapelotas.app.data.preferences.PreferencesRepositoryImpl
import com.chapelotas.app.di.ApplicationScope
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
import com.chapelotas.app.data.preferences.PreferencesRepositoryImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindCalendarRepository(impl: CalendarRepositoryImpl): CalendarRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindAIRepository(impl: AIRepositoryImpl): AIRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): ChapelotasDatabase {
            return Room.databaseBuilder(context, ChapelotasDatabase::class.java, "chapelotas_database")
                .fallbackToDestructiveMigration()
                .build()
        }

        @Provides
        @Singleton
        fun provideDayPlanDao(db: ChapelotasDatabase): DayPlanDao = db.dayPlanDao()

        @Provides
        @Singleton
        fun provideEventPlanDao(db: ChapelotasDatabase): EventPlanDao = db.eventPlanDao()

        @Provides
        @Singleton
        fun provideNotificationDao(db: ChapelotasDatabase): NotificationDao = db.notificationDao()

        @Provides
        @Singleton
        fun provideConflictDao(db: ChapelotasDatabase): ConflictDao = db.conflictDao()

        @Provides
        @Singleton
        fun provideNotificationLogDao(db: ChapelotasDatabase): NotificationLogDao = db.notificationLogDao()

        @Provides
        @Singleton
        fun provideConversationLogDao(db: ChapelotasDatabase): ConversationLogDao = db.conversationLogDao()

        @Provides
        @Singleton
        fun provideMonkeyAgendaDao(database: ChapelotasDatabase): MonkeyAgendaDao {
            return database.monkeyAgendaDao()
        }

        @Provides
        @Singleton
        fun provideChatThreadDao(database: ChapelotasDatabase): ChatThreadDao {
            return database.chatThreadDao()
        }

        @Provides
        @Singleton
        fun provideGson(): Gson = GsonBuilder().create()

        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        @Provides
        @Singleton
        fun provideEventBus(): ChapelotasEventBus = ChapelotasEventBus()

        @Provides
        @Singleton
        fun provideMonkeyAgendaService(
            database: ChapelotasDatabase,
            notificationRepository: NotificationRepository,
            aiRepository: AIRepository,
            @ApplicationScope scope: CoroutineScope
        ): MonkeyAgendaService {
            return MonkeyAgendaService(
                database = database,
                notificationRepository = notificationRepository,
                aiRepository = aiRepository,
                scope = scope
            )
        }

        @Provides
        @Singleton
        fun provideUnifiedMonkeyService(
            @ApplicationContext context: Context,
            database: ChapelotasDatabase,
            eventBus: ChapelotasEventBus,
            monkeyAgendaService: MonkeyAgendaService
        ): UnifiedMonkeyService {
            return UnifiedMonkeyService(context, database, eventBus, monkeyAgendaService)
        }

        @Provides
        @Singleton
        fun provideNotificationActionHandler(
            database: ChapelotasDatabase,
            eventBus: ChapelotasEventBus,
            notificationManager: NotificationManagerCompat,
            @ApplicationScope scope: CoroutineScope
        ): NotificationActionHandler {
            return NotificationActionHandler(database, eventBus, notificationManager, scope)
        }

        @Provides
        @Singleton
        fun provideNotificationManagerCompat(@ApplicationContext context: Context): NotificationManagerCompat {
            return NotificationManagerCompat.from(context)
        }

        @Provides
        @Singleton
        fun provideProcessAIPlansUseCase(
            database: ChapelotasDatabase,
            aiRepository: AIRepository,
            unifiedMonkeyService: UnifiedMonkeyService
        ): ProcessAIPlansUseCase {
            return ProcessAIPlansUseCase(
                database = database,
                aiRepository = aiRepository,
                unifiedMonkey = unifiedMonkeyService
            )
        }

        @Provides
        @Singleton
        fun provideMigrateToMonkeyAgendaUseCase(
            database: ChapelotasDatabase,
            monkeyAgendaService: MonkeyAgendaService
        ): MigrateToMonkeyAgendaUseCase {
            return MigrateToMonkeyAgendaUseCase(database, monkeyAgendaService)
        }

        @Provides
        @Singleton
        fun provideInitializeChatThreadsUseCase(
            database: ChapelotasDatabase
        ): InitializeChatThreadsUseCase {
            return InitializeChatThreadsUseCase(database)
        }

        @Provides
        @Singleton
        fun provideCalendarSyncUseCase(
            calendarRepository: CalendarRepository,
            database: ChapelotasDatabase,
            eventBus: ChapelotasEventBus
        ): CalendarSyncUseCase {
            return CalendarSyncUseCase(calendarRepository, database, eventBus)
        }
    }
}