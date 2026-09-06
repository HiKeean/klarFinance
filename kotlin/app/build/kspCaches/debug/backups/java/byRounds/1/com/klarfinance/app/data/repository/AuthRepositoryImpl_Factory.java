package com.klarfinance.app.data.repository;

import com.klarfinance.app.core.network.ApiService;
import com.klarfinance.app.core.session.SecureTokenStore;
import com.klarfinance.app.core.session.SessionManager;
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
public final class AuthRepositoryImpl_Factory implements Factory<AuthRepositoryImpl> {
  private final Provider<ApiService> apiServiceProvider;

  private final Provider<SessionManager> sessionManagerProvider;

  private final Provider<SecureTokenStore> secureTokenStoreProvider;

  public AuthRepositoryImpl_Factory(Provider<ApiService> apiServiceProvider,
      Provider<SessionManager> sessionManagerProvider,
      Provider<SecureTokenStore> secureTokenStoreProvider) {
    this.apiServiceProvider = apiServiceProvider;
    this.sessionManagerProvider = sessionManagerProvider;
    this.secureTokenStoreProvider = secureTokenStoreProvider;
  }

  @Override
  public AuthRepositoryImpl get() {
    return newInstance(apiServiceProvider.get(), sessionManagerProvider.get(), secureTokenStoreProvider.get());
  }

  public static AuthRepositoryImpl_Factory create(Provider<ApiService> apiServiceProvider,
      Provider<SessionManager> sessionManagerProvider,
      Provider<SecureTokenStore> secureTokenStoreProvider) {
    return new AuthRepositoryImpl_Factory(apiServiceProvider, sessionManagerProvider, secureTokenStoreProvider);
  }

  public static AuthRepositoryImpl newInstance(ApiService apiService, SessionManager sessionManager,
      SecureTokenStore secureTokenStore) {
    return new AuthRepositoryImpl(apiService, sessionManager, secureTokenStore);
  }
}
