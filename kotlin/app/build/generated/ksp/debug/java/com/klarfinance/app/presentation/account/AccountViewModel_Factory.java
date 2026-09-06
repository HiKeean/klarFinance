package com.klarfinance.app.presentation.account;

import com.klarfinance.app.core.location.LocationScheduler;
import com.klarfinance.app.core.session.SecureTokenStore;
import com.klarfinance.app.core.session.SessionManager;
import com.klarfinance.app.domain.usecase.ChangePasswordUseCase;
import com.klarfinance.app.domain.usecase.GetLocationConsentUseCase;
import com.klarfinance.app.domain.usecase.GetProfileUseCase;
import com.klarfinance.app.domain.usecase.LogLocationFailureUseCase;
import com.klarfinance.app.domain.usecase.LogoutUseCase;
import com.klarfinance.app.domain.usecase.SetLocationConsentUseCase;
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
public final class AccountViewModel_Factory implements Factory<AccountViewModel> {
  private final Provider<GetProfileUseCase> getProfileUseCaseProvider;

  private final Provider<ChangePasswordUseCase> changePasswordUseCaseProvider;

  private final Provider<LogoutUseCase> logoutUseCaseProvider;

  private final Provider<SessionManager> sessionManagerProvider;

  private final Provider<SecureTokenStore> secureTokenStoreProvider;

  private final Provider<GetLocationConsentUseCase> getLocationConsentUseCaseProvider;

  private final Provider<SetLocationConsentUseCase> setLocationConsentUseCaseProvider;

  private final Provider<LogLocationFailureUseCase> logLocationFailureUseCaseProvider;

  private final Provider<LocationScheduler> locationSchedulerProvider;

  public AccountViewModel_Factory(Provider<GetProfileUseCase> getProfileUseCaseProvider,
      Provider<ChangePasswordUseCase> changePasswordUseCaseProvider,
      Provider<LogoutUseCase> logoutUseCaseProvider,
      Provider<SessionManager> sessionManagerProvider,
      Provider<SecureTokenStore> secureTokenStoreProvider,
      Provider<GetLocationConsentUseCase> getLocationConsentUseCaseProvider,
      Provider<SetLocationConsentUseCase> setLocationConsentUseCaseProvider,
      Provider<LogLocationFailureUseCase> logLocationFailureUseCaseProvider,
      Provider<LocationScheduler> locationSchedulerProvider) {
    this.getProfileUseCaseProvider = getProfileUseCaseProvider;
    this.changePasswordUseCaseProvider = changePasswordUseCaseProvider;
    this.logoutUseCaseProvider = logoutUseCaseProvider;
    this.sessionManagerProvider = sessionManagerProvider;
    this.secureTokenStoreProvider = secureTokenStoreProvider;
    this.getLocationConsentUseCaseProvider = getLocationConsentUseCaseProvider;
    this.setLocationConsentUseCaseProvider = setLocationConsentUseCaseProvider;
    this.logLocationFailureUseCaseProvider = logLocationFailureUseCaseProvider;
    this.locationSchedulerProvider = locationSchedulerProvider;
  }

  @Override
  public AccountViewModel get() {
    return newInstance(getProfileUseCaseProvider.get(), changePasswordUseCaseProvider.get(), logoutUseCaseProvider.get(), sessionManagerProvider.get(), secureTokenStoreProvider.get(), getLocationConsentUseCaseProvider.get(), setLocationConsentUseCaseProvider.get(), logLocationFailureUseCaseProvider.get(), locationSchedulerProvider.get());
  }

  public static AccountViewModel_Factory create(
      Provider<GetProfileUseCase> getProfileUseCaseProvider,
      Provider<ChangePasswordUseCase> changePasswordUseCaseProvider,
      Provider<LogoutUseCase> logoutUseCaseProvider,
      Provider<SessionManager> sessionManagerProvider,
      Provider<SecureTokenStore> secureTokenStoreProvider,
      Provider<GetLocationConsentUseCase> getLocationConsentUseCaseProvider,
      Provider<SetLocationConsentUseCase> setLocationConsentUseCaseProvider,
      Provider<LogLocationFailureUseCase> logLocationFailureUseCaseProvider,
      Provider<LocationScheduler> locationSchedulerProvider) {
    return new AccountViewModel_Factory(getProfileUseCaseProvider, changePasswordUseCaseProvider, logoutUseCaseProvider, sessionManagerProvider, secureTokenStoreProvider, getLocationConsentUseCaseProvider, setLocationConsentUseCaseProvider, logLocationFailureUseCaseProvider, locationSchedulerProvider);
  }

  public static AccountViewModel newInstance(GetProfileUseCase getProfileUseCase,
      ChangePasswordUseCase changePasswordUseCase, LogoutUseCase logoutUseCase,
      SessionManager sessionManager, SecureTokenStore secureTokenStore,
      GetLocationConsentUseCase getLocationConsentUseCase,
      SetLocationConsentUseCase setLocationConsentUseCase,
      LogLocationFailureUseCase logLocationFailureUseCase, LocationScheduler locationScheduler) {
    return new AccountViewModel(getProfileUseCase, changePasswordUseCase, logoutUseCase, sessionManager, secureTokenStore, getLocationConsentUseCase, setLocationConsentUseCase, logLocationFailureUseCase, locationScheduler);
  }
}
