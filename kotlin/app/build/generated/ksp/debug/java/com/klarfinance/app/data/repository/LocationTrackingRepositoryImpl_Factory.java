package com.klarfinance.app.data.repository;

import com.klarfinance.app.core.network.ApiService;
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
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class LocationTrackingRepositoryImpl_Factory implements Factory<LocationTrackingRepositoryImpl> {
  private final Provider<ApiService> apiServiceProvider;

  public LocationTrackingRepositoryImpl_Factory(Provider<ApiService> apiServiceProvider) {
    this.apiServiceProvider = apiServiceProvider;
  }

  @Override
  public LocationTrackingRepositoryImpl get() {
    return newInstance(apiServiceProvider.get());
  }

  public static LocationTrackingRepositoryImpl_Factory create(
      Provider<ApiService> apiServiceProvider) {
    return new LocationTrackingRepositoryImpl_Factory(apiServiceProvider);
  }

  public static LocationTrackingRepositoryImpl newInstance(ApiService apiService) {
    return new LocationTrackingRepositoryImpl(apiService);
  }
}
