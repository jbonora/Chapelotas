package com.chapelotas.app.domain.usecases;

import com.chapelotas.app.domain.repositories.AIRepository;
import com.chapelotas.app.domain.repositories.CalendarRepository;
import com.chapelotas.app.domain.repositories.NotificationRepository;
import com.chapelotas.app.domain.repositories.PreferencesRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class ScheduleNotificationsUseCase_Factory implements Factory<ScheduleNotificationsUseCase> {
  private final Provider<CalendarRepository> calendarRepositoryProvider;

  private final Provider<NotificationRepository> notificationRepositoryProvider;

  private final Provider<AIRepository> aiRepositoryProvider;

  private final Provider<PreferencesRepository> preferencesRepositoryProvider;

  public ScheduleNotificationsUseCase_Factory(
      Provider<CalendarRepository> calendarRepositoryProvider,
      Provider<NotificationRepository> notificationRepositoryProvider,
      Provider<AIRepository> aiRepositoryProvider,
      Provider<PreferencesRepository> preferencesRepositoryProvider) {
    this.calendarRepositoryProvider = calendarRepositoryProvider;
    this.notificationRepositoryProvider = notificationRepositoryProvider;
    this.aiRepositoryProvider = aiRepositoryProvider;
    this.preferencesRepositoryProvider = preferencesRepositoryProvider;
  }

  @Override
  public ScheduleNotificationsUseCase get() {
    return newInstance(calendarRepositoryProvider.get(), notificationRepositoryProvider.get(), aiRepositoryProvider.get(), preferencesRepositoryProvider.get());
  }

  public static ScheduleNotificationsUseCase_Factory create(
      Provider<CalendarRepository> calendarRepositoryProvider,
      Provider<NotificationRepository> notificationRepositoryProvider,
      Provider<AIRepository> aiRepositoryProvider,
      Provider<PreferencesRepository> preferencesRepositoryProvider) {
    return new ScheduleNotificationsUseCase_Factory(calendarRepositoryProvider, notificationRepositoryProvider, aiRepositoryProvider, preferencesRepositoryProvider);
  }

  public static ScheduleNotificationsUseCase newInstance(CalendarRepository calendarRepository,
      NotificationRepository notificationRepository, AIRepository aiRepository,
      PreferencesRepository preferencesRepository) {
    return new ScheduleNotificationsUseCase(calendarRepository, notificationRepository, aiRepository, preferencesRepository);
  }
}
