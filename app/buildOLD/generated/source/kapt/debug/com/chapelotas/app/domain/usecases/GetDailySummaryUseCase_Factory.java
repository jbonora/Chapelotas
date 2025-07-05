package com.chapelotas.app.domain.usecases;

import com.chapelotas.app.domain.repositories.AIRepository;
import com.chapelotas.app.domain.repositories.CalendarRepository;
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
public final class GetDailySummaryUseCase_Factory implements Factory<GetDailySummaryUseCase> {
  private final Provider<CalendarRepository> calendarRepositoryProvider;

  private final Provider<AIRepository> aiRepositoryProvider;

  private final Provider<PreferencesRepository> preferencesRepositoryProvider;

  public GetDailySummaryUseCase_Factory(Provider<CalendarRepository> calendarRepositoryProvider,
      Provider<AIRepository> aiRepositoryProvider,
      Provider<PreferencesRepository> preferencesRepositoryProvider) {
    this.calendarRepositoryProvider = calendarRepositoryProvider;
    this.aiRepositoryProvider = aiRepositoryProvider;
    this.preferencesRepositoryProvider = preferencesRepositoryProvider;
  }

  @Override
  public GetDailySummaryUseCase get() {
    return newInstance(calendarRepositoryProvider.get(), aiRepositoryProvider.get(), preferencesRepositoryProvider.get());
  }

  public static GetDailySummaryUseCase_Factory create(
      Provider<CalendarRepository> calendarRepositoryProvider,
      Provider<AIRepository> aiRepositoryProvider,
      Provider<PreferencesRepository> preferencesRepositoryProvider) {
    return new GetDailySummaryUseCase_Factory(calendarRepositoryProvider, aiRepositoryProvider, preferencesRepositoryProvider);
  }

  public static GetDailySummaryUseCase newInstance(CalendarRepository calendarRepository,
      AIRepository aiRepository, PreferencesRepository preferencesRepository) {
    return new GetDailySummaryUseCase(calendarRepository, aiRepository, preferencesRepository);
  }
}
