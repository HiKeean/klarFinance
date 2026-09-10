package com.klarfinance.app.presentation.login;

import com.klarfinance.app.domain.repository.VerifiedPhoneRepository;
import com.klarfinance.app.domain.usecase.CheckPhoneRegisteredUseCase;
import com.klarfinance.app.domain.usecase.RequestOtpUseCase;
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
public final class LoginViewModel_Factory implements Factory<LoginViewModel> {
  private final Provider<RequestOtpUseCase> requestOtpUseCaseProvider;

  private final Provider<VerifiedPhoneRepository> verifiedPhoneRepositoryProvider;

  private final Provider<CheckPhoneRegisteredUseCase> checkPhoneRegisteredUseCaseProvider;

  public LoginViewModel_Factory(Provider<RequestOtpUseCase> requestOtpUseCaseProvider,
      Provider<VerifiedPhoneRepository> verifiedPhoneRepositoryProvider,
      Provider<CheckPhoneRegisteredUseCase> checkPhoneRegisteredUseCaseProvider) {
    this.requestOtpUseCaseProvider = requestOtpUseCaseProvider;
    this.verifiedPhoneRepositoryProvider = verifiedPhoneRepositoryProvider;
    this.checkPhoneRegisteredUseCaseProvider = checkPhoneRegisteredUseCaseProvider;
  }

  @Override
  public LoginViewModel get() {
    return newInstance(requestOtpUseCaseProvider.get(), verifiedPhoneRepositoryProvider.get(), checkPhoneRegisteredUseCaseProvider.get());
  }

  public static LoginViewModel_Factory create(Provider<RequestOtpUseCase> requestOtpUseCaseProvider,
      Provider<VerifiedPhoneRepository> verifiedPhoneRepositoryProvider,
      Provider<CheckPhoneRegisteredUseCase> checkPhoneRegisteredUseCaseProvider) {
    return new LoginViewModel_Factory(requestOtpUseCaseProvider, verifiedPhoneRepositoryProvider, checkPhoneRegisteredUseCaseProvider);
  }

  public static LoginViewModel newInstance(RequestOtpUseCase requestOtpUseCase,
      VerifiedPhoneRepository verifiedPhoneRepository,
      CheckPhoneRegisteredUseCase checkPhoneRegisteredUseCase) {
    return new LoginViewModel(requestOtpUseCase, verifiedPhoneRepository, checkPhoneRegisteredUseCase);
  }
}
