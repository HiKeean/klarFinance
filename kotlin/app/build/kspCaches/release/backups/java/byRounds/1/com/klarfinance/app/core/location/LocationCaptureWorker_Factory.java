package com.klarfinance.app.core.location;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.klarfinance.app.domain.usecase.LogLocationFailureUseCase;
import com.klarfinance.app.domain.usecase.SubmitLocationPingUseCase;
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
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class LocationCaptureWorker_Factory {
  private final Provider<SubmitLocationPingUseCase> submitLocationPingUseCaseProvider;

  private final Provider<LogLocationFailureUseCase> logLocationFailureUseCaseProvider;

  private final Provider<LocationScheduler> locationSchedulerProvider;

  public LocationCaptureWorker_Factory(
      Provider<SubmitLocationPingUseCase> submitLocationPingUseCaseProvider,
      Provider<LogLocationFailureUseCase> logLocationFailureUseCaseProvider,
      Provider<LocationScheduler> locationSchedulerProvider) {
    this.submitLocationPingUseCaseProvider = submitLocationPingUseCaseProvider;
    this.logLocationFailureUseCaseProvider = logLocationFailureUseCaseProvider;
    this.locationSchedulerProvider = locationSchedulerProvider;
  }

  public LocationCaptureWorker get(Context context, WorkerParameters params) {
    return newInstance(context, params, submitLocationPingUseCaseProvider.get(), logLocationFailureUseCaseProvider.get(), locationSchedulerProvider.get());
  }

  public static LocationCaptureWorker_Factory create(
      Provider<SubmitLocationPingUseCase> submitLocationPingUseCaseProvider,
      Provider<LogLocationFailureUseCase> logLocationFailureUseCaseProvider,
      Provider<LocationScheduler> locationSchedulerProvider) {
    return new LocationCaptureWorker_Factory(submitLocationPingUseCaseProvider, logLocationFailureUseCaseProvider, locationSchedulerProvider);
  }

  public static LocationCaptureWorker newInstance(Context context, WorkerParameters params,
      SubmitLocationPingUseCase submitLocationPingUseCase,
      LogLocationFailureUseCase logLocationFailureUseCase, LocationScheduler locationScheduler) {
    return new LocationCaptureWorker(context, params, submitLocationPingUseCase, logLocationFailureUseCase, locationScheduler);
  }
}
