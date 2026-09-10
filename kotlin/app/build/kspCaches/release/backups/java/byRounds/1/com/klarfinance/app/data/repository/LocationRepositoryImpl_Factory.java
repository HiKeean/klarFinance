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
public final class LocationRepositoryImpl_Factory implements Factory<LocationRepositoryImpl> {
  private final Provider<ApiService> apiServiceProvider;

  public LocationRepositoryImpl_Factory(Provider<ApiService> apiServiceProvider) {
    this.apiServiceProvider = apiServiceProvider;
  }

  @Override
  public LocationRepositoryImpl get() {
    return newInstance(apiServiceProvider.get());
  }

  public static LocationRepositoryImpl_Factory create(Provider<ApiService> apiServiceProvider) {
    return new LocationRepositoryImpl_Factory(apiServiceProvider);
  }

  public static LocationRepositoryImpl newInstance(ApiService apiService) {
    return new LocationRepositoryImpl(apiService);
  }
}
