package com.chapelotas.app.data.notifications;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.chapelotas.app.domain.repositories.NotificationRepository;
import dagger.internal.DaggerGenerated;
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
public final class NotificationWorker_Factory {
  private final Provider<NotificationRepository> notificationRepositoryProvider;

  public NotificationWorker_Factory(
      Provider<NotificationRepository> notificationRepositoryProvider) {
    this.notificationRepositoryProvider = notificationRepositoryProvider;
  }

  public NotificationWorker get(Context appContext, WorkerParameters workerParams) {
    return newInstance(appContext, workerParams, notificationRepositoryProvider.get());
  }

  public static NotificationWorker_Factory create(
      Provider<NotificationRepository> notificationRepositoryProvider) {
    return new NotificationWorker_Factory(notificationRepositoryProvider);
  }

  public static NotificationWorker newInstance(Context appContext, WorkerParameters workerParams,
      NotificationRepository notificationRepository) {
    return new NotificationWorker(appContext, workerParams, notificationRepository);
  }
}
