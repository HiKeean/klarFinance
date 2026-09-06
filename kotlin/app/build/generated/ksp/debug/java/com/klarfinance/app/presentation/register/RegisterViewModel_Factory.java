package com.klarfinance.app.presentation.register;

import android.content.Context;
import androidx.lifecycle.SavedStateHandle;
import com.klarfinance.app.domain.repository.LocationRepository;
import com.klarfinance.app.domain.usecase.LoginUseCase;
import com.klarfinance.app.domain.usecase.RegisterUseCase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class RegisterViewModel_Factory implements Factory<RegisterViewModel> {
  private final Provider<LocationRepository> locationRepositoryProvider;

  private final Provider<RegisterUseCase> registerUseCaseProvider;

  private final Provider<LoginUseCase> loginUseCaseProvider;

  private final Provider<Context> appContextProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public RegisterViewModel_Factory(Provider<LocationRepository> locationRepositoryProvider,
      Provider<RegisterUseCase> registerUseCaseProvider,
      Provider<LoginUseCase> loginUseCaseProvider, Provider<Context> appContextProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.locationRepositoryProvider = locationRepositoryProvider;
    this.registerUseCaseProvider = registerUseCaseProvider;
    this.loginUseCaseProvider = loginUseCaseProvider;
    this.appContextProvider = appContextProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public RegisterViewModel get() {
    return newInstance(locationRepositoryProvider.get(), registerUseCaseProvider.get(), loginUseCaseProvider.get(), appContextProvider.get(), savedStateHandleProvider.get());
  }

  public static RegisterViewModel_Factory create(
      Provider<LocationRepository> locationRepositoryProvider,
      Provider<RegisterUseCase> registerUseCaseProvider,
      Provider<LoginUseCase> loginUseCaseProvider, Provider<Context> appContextProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new RegisterViewModel_Factory(locationRepositoryProvider, registerUseCaseProvider, loginUseCaseProvider, appContextProvider, savedStateHandleProvider);
  }

  public static RegisterViewModel newInstance(LocationRepository locationRepository,
      RegisterUseCase registerUseCase, LoginUseCase loginUseCase, Context appContext,
      SavedStateHandle savedStateHandle) {
    return new RegisterViewModel(locationRepository, registerUseCase, loginUseCase, appContext, savedStateHandle);
  }
}
