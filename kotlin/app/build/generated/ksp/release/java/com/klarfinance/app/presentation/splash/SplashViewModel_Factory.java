package com.klarfinance.app.presentation.splash;

import com.klarfinance.app.core.session.SecureTokenStore;
import com.klarfinance.app.domain.usecase.RefreshSessionUseCase;
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
public final class SplashViewModel_Factory implements Factory<SplashViewModel> {
  private final Provider<SecureTokenStore> secureTokenStoreProvider;

  private final Provider<RefreshSessionUseCase> refreshSessionUseCaseProvider;

  public SplashViewModel_Factory(Provider<SecureTokenStore> secureTokenStoreProvider,
      Provider<RefreshSessionUseCase> refreshSessionUseCaseProvider) {
    this.secureTokenStoreProvider = secureTokenStoreProvider;
    this.refreshSessionUseCaseProvider = refreshSessionUseCaseProvider;
  }

  @Override
  public SplashViewModel get() {
    return newInstance(secureTokenStoreProvider.get(), refreshSessionUseCaseProvider.get());
  }

  public static SplashViewModel_Factory create(Provider<SecureTokenStore> secureTokenStoreProvider,
      Provider<RefreshSessionUseCase> refreshSessionUseCaseProvider) {
    return new SplashViewModel_Factory(secureTokenStoreProvider, refreshSessionUseCaseProvider);
  }

  public static SplashViewModel newInstance(SecureTokenStore secureTokenStore,
      RefreshSessionUseCase refreshSessionUseCase) {
    return new SplashViewModel(secureTokenStore, refreshSessionUseCase);
  }
}
