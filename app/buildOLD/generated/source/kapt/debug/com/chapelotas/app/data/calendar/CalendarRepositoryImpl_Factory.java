package com.chapelotas.app.data.calendar;

import android.content.Context;
import com.chapelotas.app.domain.repositories.PreferencesRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class CalendarRepositoryImpl_Factory implements Factory<CalendarRepositoryImpl> {
  private final Provider<Context> contextProvider;

  private final Provider<PreferencesRepository> preferencesRepositoryProvider;

  public CalendarRepositoryImpl_Factory(Provider<Context> contextProvider,
      Provider<PreferencesRepository> preferencesRepositoryProvider) {
    this.contextProvider = contextProvider;
    this.preferencesRepositoryProvider = preferencesRepositoryProvider;
  }

  @Override
  public CalendarRepositoryImpl get() {
    return newInstance(contextProvider.get(), preferencesRepositoryProvider.get());
  }

  public static CalendarRepositoryImpl_Factory create(Provider<Context> contextProvider,
      Provider<PreferencesRepository> preferencesRepositoryProvider) {
    return new CalendarRepositoryImpl_Factory(contextProvider, preferencesRepositoryProvider);
  }

  public static CalendarRepositoryImpl newInstance(Context context,
      PreferencesRepository preferencesRepository) {
    return new CalendarRepositoryImpl(context, preferencesRepository);
  }
}
