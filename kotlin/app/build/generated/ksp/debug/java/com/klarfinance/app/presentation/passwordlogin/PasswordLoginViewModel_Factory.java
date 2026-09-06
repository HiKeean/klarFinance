package com.klarfinance.app.presentation.passwordlogin;

import androidx.lifecycle.SavedStateHandle;
import com.klarfinance.app.domain.usecase.LoginUseCase;
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
public final class PasswordLoginViewModel_Factory implements Factory<PasswordLoginViewModel> {
  private final Provider<LoginUseCase> loginUseCaseProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public PasswordLoginViewModel_Factory(Provider<LoginUseCase> loginUseCaseProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.loginUseCaseProvider = loginUseCaseProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public PasswordLoginViewModel get() {
    return newInstance(loginUseCaseProvider.get(), savedStateHandleProvider.get());
  }

  public static PasswordLoginViewModel_Factory create(Provider<LoginUseCase> loginUseCaseProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new PasswordLoginViewModel_Factory(loginUseCaseProvider, savedStateHandleProvider);
  }

  public static PasswordLoginViewModel newInstance(LoginUseCase loginUseCase,
      SavedStateHandle savedStateHandle) {
    return new PasswordLoginViewModel(loginUseCase, savedStateHandle);
  }
}
