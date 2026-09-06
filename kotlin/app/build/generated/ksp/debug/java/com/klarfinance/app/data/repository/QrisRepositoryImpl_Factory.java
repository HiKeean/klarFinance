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
public final class QrisRepositoryImpl_Factory implements Factory<QrisRepositoryImpl> {
  private final Provider<ApiService> apiServiceProvider;

  public QrisRepositoryImpl_Factory(Provider<ApiService> apiServiceProvider) {
    this.apiServiceProvider = apiServiceProvider;
  }

  @Override
  public QrisRepositoryImpl get() {
    return newInstance(apiServiceProvider.get());
  }

  public static QrisRepositoryImpl_Factory create(Provider<ApiService> apiServiceProvider) {
    return new QrisRepositoryImpl_Factory(apiServiceProvider);
  }

  public static QrisRepositoryImpl newInstance(ApiService apiService) {
    return new QrisRepositoryImpl(apiService);
  }
}
