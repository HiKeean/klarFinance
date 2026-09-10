package com.klarfinance.app.presentation.otp;

import androidx.lifecycle.SavedStateHandle;
import com.klarfinance.app.domain.repository.VerifiedPhoneRepository;
import com.klarfinance.app.domain.usecase.CheckPhoneRegisteredUseCase;
import com.klarfinance.app.domain.usecase.RequestOtpUseCase;
import com.klarfinance.app.domain.usecase.VerifyOtpUseCase;
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
public final class OtpViewModel_Factory implements Factory<OtpViewModel> {
  private final Provider<VerifyOtpUseCase> verifyOtpUseCaseProvider;

  private final Provider<RequestOtpUseCase> requestOtpUseCaseProvider;

  private final Provider<VerifiedPhoneRepository> verifiedPhoneRepositoryProvider;

  private final Provider<CheckPhoneRegisteredUseCase> checkPhoneRegisteredUseCaseProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public OtpViewModel_Factory(Provider<VerifyOtpUseCase> verifyOtpUseCaseProvider,
      Provider<RequestOtpUseCase> requestOtpUseCaseProvider,
      Provider<VerifiedPhoneRepository> verifiedPhoneRepositoryProvider,
      Provider<CheckPhoneRegisteredUseCase> checkPhoneRegisteredUseCaseProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.verifyOtpUseCaseProvider = verifyOtpUseCaseProvider;
    this.requestOtpUseCaseProvider = requestOtpUseCaseProvider;
    this.verifiedPhoneRepositoryProvider = verifiedPhoneRepositoryProvider;
    this.checkPhoneRegisteredUseCaseProvider = checkPhoneRegisteredUseCaseProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public OtpViewModel get() {
    return newInstance(verifyOtpUseCaseProvider.get(), requestOtpUseCaseProvider.get(), verifiedPhoneRepositoryProvider.get(), checkPhoneRegisteredUseCaseProvider.get(), savedStateHandleProvider.get());
  }

  public static OtpViewModel_Factory create(Provider<VerifyOtpUseCase> verifyOtpUseCaseProvider,
      Provider<RequestOtpUseCase> requestOtpUseCaseProvider,
      Provider<VerifiedPhoneRepository> verifiedPhoneRepositoryProvider,
      Provider<CheckPhoneRegisteredUseCase> checkPhoneRegisteredUseCaseProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new OtpViewModel_Factory(verifyOtpUseCaseProvider, requestOtpUseCaseProvider, verifiedPhoneRepositoryProvider, checkPhoneRegisteredUseCaseProvider, savedStateHandleProvider);
  }

  public static OtpViewModel newInstance(VerifyOtpUseCase verifyOtpUseCase,
      RequestOtpUseCase requestOtpUseCase, VerifiedPhoneRepository verifiedPhoneRepository,
      CheckPhoneRegisteredUseCase checkPhoneRegisteredUseCase, SavedStateHandle savedStateHandle) {
    return new OtpViewModel(verifyOtpUseCase, requestOtpUseCase, verifiedPhoneRepository, checkPhoneRegisteredUseCase, savedStateHandle);
  }
}
