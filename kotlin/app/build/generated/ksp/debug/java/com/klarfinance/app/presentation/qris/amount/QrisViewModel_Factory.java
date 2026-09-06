package com.klarfinance.app.presentation.qris.amount;

import androidx.lifecycle.SavedStateHandle;
import com.klarfinance.app.domain.usecase.ConfirmQrisUseCase;
import com.klarfinance.app.domain.usecase.GetLimitSummaryUseCase;
import com.klarfinance.app.domain.usecase.ScanQrisUseCase;
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
public final class QrisViewModel_Factory implements Factory<QrisViewModel> {
  private final Provider<ScanQrisUseCase> scanQrisUseCaseProvider;

  private final Provider<ConfirmQrisUseCase> confirmQrisUseCaseProvider;

  private final Provider<GetLimitSummaryUseCase> getLimitSummaryUseCaseProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public QrisViewModel_Factory(Provider<ScanQrisUseCase> scanQrisUseCaseProvider,
      Provider<ConfirmQrisUseCase> confirmQrisUseCaseProvider,
      Provider<GetLimitSummaryUseCase> getLimitSummaryUseCaseProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.scanQrisUseCaseProvider = scanQrisUseCaseProvider;
    this.confirmQrisUseCaseProvider = confirmQrisUseCaseProvider;
    this.getLimitSummaryUseCaseProvider = getLimitSummaryUseCaseProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public QrisViewModel get() {
    return newInstance(scanQrisUseCaseProvider.get(), confirmQrisUseCaseProvider.get(), getLimitSummaryUseCaseProvider.get(), savedStateHandleProvider.get());
  }

  public static QrisViewModel_Factory create(Provider<ScanQrisUseCase> scanQrisUseCaseProvider,
      Provider<ConfirmQrisUseCase> confirmQrisUseCaseProvider,
      Provider<GetLimitSummaryUseCase> getLimitSummaryUseCaseProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new QrisViewModel_Factory(scanQrisUseCaseProvider, confirmQrisUseCaseProvider, getLimitSummaryUseCaseProvider, savedStateHandleProvider);
  }

  public static QrisViewModel newInstance(ScanQrisUseCase scanQrisUseCase,
      ConfirmQrisUseCase confirmQrisUseCase, GetLimitSummaryUseCase getLimitSummaryUseCase,
      SavedStateHandle savedStateHandle) {
    return new QrisViewModel(scanQrisUseCase, confirmQrisUseCase, getLimitSummaryUseCase, savedStateHandle);
  }
}
