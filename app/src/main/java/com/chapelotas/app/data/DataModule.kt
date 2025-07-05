package com.chapelotas.app.data

import android.content.Context
import com.chapelotas.app.data.ai.AIRepositoryImpl
import com.chapelotas.app.data.calendar.CalendarRepositoryImpl
import com.chapelotas.app.data.notifications.NotificationRepositoryImpl
import com.chapelotas.app.domain.repositories.AIRepository
import com.chapelotas.app.domain.repositories.CalendarRepository
import com.chapelotas.app.domain.repositories.NotificationRepository
import com.chapelotas.app.domain.repositories.PreferencesRepository
import com.chapelotas.app.domain.usecases.MasterPlanController
import com.chapelotas.app.domain.usecases.MonkeyCheckerService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de Hilt que provee las implementaciones de los repositorios
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
        @Provides
        @Singleton
        fun provideGson(): Gson {
            return GsonBuilder()
                .create()
        }

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