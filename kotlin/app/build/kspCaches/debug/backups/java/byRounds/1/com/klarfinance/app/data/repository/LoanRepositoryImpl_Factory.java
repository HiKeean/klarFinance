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
public final class LoanRepositoryImpl_Factory implements Factory<LoanRepositoryImpl> {
  private final Provider<ApiService> apiServiceProvider;

  public LoanRepositoryImpl_Factory(Provider<ApiService> apiServiceProvider) {
    this.apiServiceProvider = apiServiceProvider;
  }

  @Override
  public LoanRepositoryImpl get() {
    return newInstance(apiServiceProvider.get());
  }

  public static LoanRepositoryImpl_Factory create(Provider<ApiService> apiServiceProvider) {
    return new LoanRepositoryImpl_Factory(apiServiceProvider);
  }

  public static LoanRepositoryImpl newInstance(ApiService apiService) {
    return new LoanRepositoryImpl(apiService);
  }
}
