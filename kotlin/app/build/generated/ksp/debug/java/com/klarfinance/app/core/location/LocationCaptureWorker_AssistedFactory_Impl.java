package com.klarfinance.app.core.location;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.InstanceFactory;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class LocationCaptureWorker_AssistedFactory_Impl implements LocationCaptureWorker_AssistedFactory {
  private final LocationCaptureWorker_Factory delegateFactory;

  LocationCaptureWorker_AssistedFactory_Impl(LocationCaptureWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public LocationCaptureWorker create(Context p0, WorkerParameters p1) {
    return delegateFactory.get(p0, p1);
  }

  public static Provider<LocationCaptureWorker_AssistedFactory> create(
      LocationCaptureWorker_Factory delegateFactory) {
    return InstanceFactory.create(new LocationCaptureWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<LocationCaptureWorker_AssistedFactory> createFactoryProvider(
      LocationCaptureWorker_Factory delegateFactory) {
    return InstanceFactory.create(new LocationCaptureWorker_AssistedFactory_Impl(delegateFactory));
  }
}
