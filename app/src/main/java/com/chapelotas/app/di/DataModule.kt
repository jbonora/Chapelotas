package com.chapelotas.app.di

import com.chapelotas.app.data.calendar.CalendarRepositoryImpl
import com.chapelotas.app.data.notifications.NotificationRepositoryImpl
import com.chapelotas.app.data.preferences.PreferencesRepositoryImpl
import com.chapelotas.app.data.repositories.TaskRepositoryImpl
import com.chapelotas.app.domain.repositories.CalendarRepository
import com.chapelotas.app.domain.repositories.NotificationRepository
import com.chapelotas.app.domain.repositories.PreferencesRepository
import com.chapelotas.app.domain.repositories.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de bindings
 * Conecta interfaces con sus implementaciones
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        impl: TaskRepositoryImpl
    ): TaskRepository

    @Binds
    @Singleton
    abstract fun bindCalendarRepository(
        impl: CalendarRepositoryImpl
    ): CalendarRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        impl: NotificationRepositoryImpl
    ): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(
        impl: PreferencesRepositoryImpl
    ): PreferencesRepository
}