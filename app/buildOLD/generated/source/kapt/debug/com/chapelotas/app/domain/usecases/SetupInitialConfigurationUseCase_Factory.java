package com.chapelotas.app.domain.usecases;

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
public final class SetupInitialConfigurationUseCase_Factory implements Factory<SetupInitialConfigurationUseCase> {
  private final Provider<CalendarRepository> calendarRepositoryProvider;

  private final Provider<NotificationRepository> notificationRepositoryProvider;

  private final Provider<PreferencesRepository> preferencesRepositoryProvider;

  public SetupInitialConfigurationUseCase_Factory(
      Provider<CalendarRepository> calendarRepositoryProvider,
      Provider<NotificationRepository> notificationRepositoryProvider,
      Provider<PreferencesRepository> preferencesRepositoryProvider) {
    this.calendarRepositoryProvider = calendarRepositoryProvider;
    this.notificationRepositoryProvider = notificationRepositoryProvider;
    this.preferencesRepositoryProvider = preferencesRepositoryProvider;
  }

  @Override
  public SetupInitialConfigurationUseCase get() {
    return newInstance(calendarRepositoryProvider.get(), notificationRepositoryProvider.get(), preferencesRepositoryProvider.get());
  }

  public static SetupInitialConfigurationUseCase_Factory create(
      Provider<CalendarRepository> calendarRepositoryProvider,
      Provider<NotificationRepository> notificationRepositoryProvider,
      Provider<PreferencesRepository> preferencesRepositoryProvider) {
    return new SetupInitialConfigurationUseCase_Factory(calendarRepositoryProvider, notificationRepositoryProvider, preferencesRepositoryProvider);
  }

  public static SetupInitialConfigurationUseCase newInstance(CalendarRepository calendarRepository,
      NotificationRepository notificationRepository, PreferencesRepository preferencesRepository) {
    return new SetupInitialConfigurationUseCase(calendarRepository, notificationRepository, preferencesRepository);
  }
}
