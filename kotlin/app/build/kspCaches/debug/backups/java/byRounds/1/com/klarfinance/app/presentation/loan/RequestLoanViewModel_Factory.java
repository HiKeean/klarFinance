package com.klarfinance.app.presentation.loan;

import android.content.Context;
import com.klarfinance.app.domain.usecase.GetLimitSummaryUseCase;
import com.klarfinance.app.domain.usecase.RequestLoanUseCase;
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
public final class RequestLoanViewModel_Factory implements Factory<RequestLoanViewModel> {
  private final Provider<GetLimitSummaryUseCase> getLimitSummaryUseCaseProvider;

  private final Provider<RequestLoanUseCase> requestLoanUseCaseProvider;

  private final Provider<Context> appContextProvider;

  public RequestLoanViewModel_Factory(
      Provider<GetLimitSummaryUseCase> getLimitSummaryUseCaseProvider,
      Provider<RequestLoanUseCase> requestLoanUseCaseProvider,
      Provider<Context> appContextProvider) {
    this.getLimitSummaryUseCaseProvider = getLimitSummaryUseCaseProvider;
    this.requestLoanUseCaseProvider = requestLoanUseCaseProvider;
    this.appContextProvider = appContextProvider;
  }

  @Override
  public RequestLoanViewModel get() {
    return newInstance(getLimitSummaryUseCaseProvider.get(), requestLoanUseCaseProvider.get(), appContextProvider.get());
  }

  public static RequestLoanViewModel_Factory create(
      Provider<GetLimitSummaryUseCase> getLimitSummaryUseCaseProvider,
      Provider<RequestLoanUseCase> requestLoanUseCaseProvider,
      Provider<Context> appContextProvider) {
    return new RequestLoanViewModel_Factory(getLimitSummaryUseCaseProvider, requestLoanUseCaseProvider, appContextProvider);
  }

  public static RequestLoanViewModel newInstance(GetLimitSummaryUseCase getLimitSummaryUseCase,
      RequestLoanUseCase requestLoanUseCase, Context appContext) {
    return new RequestLoanViewModel(getLimitSummaryUseCase, requestLoanUseCase, appContext);
  }
}
