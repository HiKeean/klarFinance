package com.klarfinance.app.presentation.history;

import com.klarfinance.app.domain.usecase.GetLoanHistoryUseCase;
import com.klarfinance.app.domain.usecase.RepayLoanUseCase;
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
public final class HistoryViewModel_Factory implements Factory<HistoryViewModel> {
  private final Provider<GetLoanHistoryUseCase> getLoanHistoryUseCaseProvider;

  private final Provider<RepayLoanUseCase> repayLoanUseCaseProvider;

  public HistoryViewModel_Factory(Provider<GetLoanHistoryUseCase> getLoanHistoryUseCaseProvider,
      Provider<RepayLoanUseCase> repayLoanUseCaseProvider) {
    this.getLoanHistoryUseCaseProvider = getLoanHistoryUseCaseProvider;
    this.repayLoanUseCaseProvider = repayLoanUseCaseProvider;
  }

  @Override
  public HistoryViewModel get() {
    return newInstance(getLoanHistoryUseCaseProvider.get(), repayLoanUseCaseProvider.get());
  }

  public static HistoryViewModel_Factory create(
      Provider<GetLoanHistoryUseCase> getLoanHistoryUseCaseProvider,
      Provider<RepayLoanUseCase> repayLoanUseCaseProvider) {
    return new HistoryViewModel_Factory(getLoanHistoryUseCaseProvider, repayLoanUseCaseProvider);
  }

  public static HistoryViewModel newInstance(GetLoanHistoryUseCase getLoanHistoryUseCase,
      RepayLoanUseCase repayLoanUseCase) {
    return new HistoryViewModel(getLoanHistoryUseCase, repayLoanUseCase);
  }
}
