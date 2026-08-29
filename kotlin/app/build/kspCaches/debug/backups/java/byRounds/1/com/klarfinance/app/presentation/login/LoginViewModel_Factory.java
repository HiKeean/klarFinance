package com.klarfinance.app.presentation.login;

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

  public LoginViewModel_Factory(Provider<RequestOtpUseCase> requestOtpUseCaseProvider) {
    this.requestOtpUseCaseProvider = requestOtpUseCaseProvider;
  }

  @Override
  public LoginViewModel get() {
    return newInstance(requestOtpUseCaseProvider.get());
  }

  public static LoginViewModel_Factory create(
      Provider<RequestOtpUseCase> requestOtpUseCaseProvider) {
    return new LoginViewModel_Factory(requestOtpUseCaseProvider);
  }

  public static LoginViewModel newInstance(RequestOtpUseCase requestOtpUseCase) {
    return new LoginViewModel(requestOtpUseCase);
  }
}
