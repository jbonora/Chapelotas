package com.chapelotas.app.presentation.viewmodels;

import com.chapelotas.app.domain.usecases.GetDailySummaryUseCase;
import com.chapelotas.app.domain.usecases.GetTomorrowSummaryUseCase;
import com.chapelotas.app.domain.usecases.MarkEventAsCriticalUseCase;
import com.chapelotas.app.domain.usecases.ScheduleNotificationsUseCase;
import com.chapelotas.app.domain.usecases.SetupInitialConfigurationUseCase;
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
public final class MainViewModel_Factory implements Factory<MainViewModel> {
  private final Provider<GetDailySummaryUseCase> getDailySummaryUseCaseProvider;

  private final Provider<GetTomorrowSummaryUseCase> getTomorrowSummaryUseCaseProvider;

  private final Provider<ScheduleNotificationsUseCase> scheduleNotificationsUseCaseProvider;

  private final Provider<MarkEventAsCriticalUseCase> markEventAsCriticalUseCaseProvider;

  private final Provider<SetupInitialConfigurationUseCase> setupInitialConfigurationUseCaseProvider;

  public MainViewModel_Factory(Provider<GetDailySummaryUseCase> getDailySummaryUseCaseProvider,
      Provider<GetTomorrowSummaryUseCase> getTomorrowSummaryUseCaseProvider,
      Provider<ScheduleNotificationsUseCase> scheduleNotificationsUseCaseProvider,
      Provider<MarkEventAsCriticalUseCase> markEventAsCriticalUseCaseProvider,
      Provider<SetupInitialConfigurationUseCase> setupInitialConfigurationUseCaseProvider) {
    this.getDailySummaryUseCaseProvider = getDailySummaryUseCaseProvider;
    this.getTomorrowSummaryUseCaseProvider = getTomorrowSummaryUseCaseProvider;
    this.scheduleNotificationsUseCaseProvider = scheduleNotificationsUseCaseProvider;
    this.markEventAsCriticalUseCaseProvider = markEventAsCriticalUseCaseProvider;
    this.setupInitialConfigurationUseCaseProvider = setupInitialConfigurationUseCaseProvider;
  }

  @Override
  public MainViewModel get() {
    return newInstance(getDailySummaryUseCaseProvider.get(), getTomorrowSummaryUseCaseProvider.get(), scheduleNotificationsUseCaseProvider.get(), markEventAsCriticalUseCaseProvider.get(), setupInitialConfigurationUseCaseProvider.get());
  }

  public static MainViewModel_Factory create(
      Provider<GetDailySummaryUseCase> getDailySummaryUseCaseProvider,
      Provider<GetTomorrowSummaryUseCase> getTomorrowSummaryUseCaseProvider,
      Provider<ScheduleNotificationsUseCase> scheduleNotificationsUseCaseProvider,
      Provider<MarkEventAsCriticalUseCase> markEventAsCriticalUseCaseProvider,
      Provider<SetupInitialConfigurationUseCase> setupInitialConfigurationUseCaseProvider) {
    return new MainViewModel_Factory(getDailySummaryUseCaseProvider, getTomorrowSummaryUseCaseProvider, scheduleNotificationsUseCaseProvider, markEventAsCriticalUseCaseProvider, setupInitialConfigurationUseCaseProvider);
  }

  public static MainViewModel newInstance(GetDailySummaryUseCase getDailySummaryUseCase,
      GetTomorrowSummaryUseCase getTomorrowSummaryUseCase,
      ScheduleNotificationsUseCase scheduleNotificationsUseCase,
      MarkEventAsCriticalUseCase markEventAsCriticalUseCase,
      SetupInitialConfigurationUseCase setupInitialConfigurationUseCase) {
    return new MainViewModel(getDailySummaryUseCase, getTomorrowSummaryUseCase, scheduleNotificationsUseCase, markEventAsCriticalUseCase, setupInitialConfigurationUseCase);
  }
}
