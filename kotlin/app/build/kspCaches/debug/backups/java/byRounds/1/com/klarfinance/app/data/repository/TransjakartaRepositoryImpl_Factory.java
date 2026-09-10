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
public final class TransjakartaRepositoryImpl_Factory implements Factory<TransjakartaRepositoryImpl> {
  private final Provider<ApiService> apiServiceProvider;

  public TransjakartaRepositoryImpl_Factory(Provider<ApiService> apiServiceProvider) {
    this.apiServiceProvider = apiServiceProvider;
  }

  @Override
  public TransjakartaRepositoryImpl get() {
    return newInstance(apiServiceProvider.get());
  }

  public static TransjakartaRepositoryImpl_Factory create(Provider<ApiService> apiServiceProvider) {
    return new TransjakartaRepositoryImpl_Factory(apiServiceProvider);
  }

  public static TransjakartaRepositoryImpl newInstance(ApiService apiService) {
    return new TransjakartaRepositoryImpl(apiService);
  }
}
