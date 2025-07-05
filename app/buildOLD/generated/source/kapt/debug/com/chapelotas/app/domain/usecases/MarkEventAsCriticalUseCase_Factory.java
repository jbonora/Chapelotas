package com.chapelotas.app.domain.usecases;

import com.chapelotas.app.domain.repositories.CalendarRepository;
import com.chapelotas.app.domain.repositories.NotificationRepository;
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
public final class MarkEventAsCriticalUseCase_Factory implements Factory<MarkEventAsCriticalUseCase> {
  private final Provider<CalendarRepository> calendarRepositoryProvider;

  private final Provider<NotificationRepository> notificationRepositoryProvider;

  public MarkEventAsCriticalUseCase_Factory(Provider<CalendarRepository> calendarRepositoryProvider,
      Provider<NotificationRepository> notificationRepositoryProvider) {
    this.calendarRepositoryProvider = calendarRepositoryProvider;
    this.notificationRepositoryProvider = notificationRepositoryProvider;
  }

  @Override
  public MarkEventAsCriticalUseCase get() {
    return newInstance(calendarRepositoryProvider.get(), notificationRepositoryProvider.get());
  }

  public static MarkEventAsCriticalUseCase_Factory create(
      Provider<CalendarRepository> calendarRepositoryProvider,
      Provider<NotificationRepository> notificationRepositoryProvider) {
    return new MarkEventAsCriticalUseCase_Factory(calendarRepositoryProvider, notificationRepositoryProvider);
  }

  public static MarkEventAsCriticalUseCase newInstance(CalendarRepository calendarRepository,
      NotificationRepository notificationRepository) {
    return new MarkEventAsCriticalUseCase(calendarRepository, notificationRepository);
  }
}
