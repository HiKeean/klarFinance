package com.klarfinance.app.presentation.loan;

import android.content.Context;
import com.klarfinance.app.core.session.SecureTokenStore;
import com.klarfinance.app.domain.usecase.GetLimitSummaryUseCase;
import com.klarfinance.app.domain.usecase.GetSavedBankAccountsUseCase;
import com.klarfinance.app.domain.usecase.RequestLoanUseCase;
import com.klarfinance.app.domain.usecase.VerifyPasswordUseCase;
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

  private final Provider<GetSavedBankAccountsUseCase> getSavedBankAccountsUseCaseProvider;

  private final Provider<RequestLoanUseCase> requestLoanUseCaseProvider;

  private final Provider<VerifyPasswordUseCase> verifyPasswordUseCaseProvider;

  private final Provider<SecureTokenStore> secureTokenStoreProvider;

  private final Provider<Context> appContextProvider;

  public RequestLoanViewModel_Factory(
      Provider<GetLimitSummaryUseCase> getLimitSummaryUseCaseProvider,
      Provider<GetSavedBankAccountsUseCase> getSavedBankAccountsUseCaseProvider,
      Provider<RequestLoanUseCase> requestLoanUseCaseProvider,
      Provider<VerifyPasswordUseCase> verifyPasswordUseCaseProvider,
      Provider<SecureTokenStore> secureTokenStoreProvider, Provider<Context> appContextProvider) {
    this.getLimitSummaryUseCaseProvider = getLimitSummaryUseCaseProvider;
    this.getSavedBankAccountsUseCaseProvider = getSavedBankAccountsUseCaseProvider;
    this.requestLoanUseCaseProvider = requestLoanUseCaseProvider;
    this.verifyPasswordUseCaseProvider = verifyPasswordUseCaseProvider;
    this.secureTokenStoreProvider = secureTokenStoreProvider;
    this.appContextProvider = appContextProvider;
  }

  @Override
  public RequestLoanViewModel get() {
    return newInstance(getLimitSummaryUseCaseProvider.get(), getSavedBankAccountsUseCaseProvider.get(), requestLoanUseCaseProvider.get(), verifyPasswordUseCaseProvider.get(), secureTokenStoreProvider.get(), appContextProvider.get());
  }

  public static RequestLoanViewModel_Factory create(
      Provider<GetLimitSummaryUseCase> getLimitSummaryUseCaseProvider,
      Provider<GetSavedBankAccountsUseCase> getSavedBankAccountsUseCaseProvider,
      Provider<RequestLoanUseCase> requestLoanUseCaseProvider,
      Provider<VerifyPasswordUseCase> verifyPasswordUseCaseProvider,
      Provider<SecureTokenStore> secureTokenStoreProvider, Provider<Context> appContextProvider) {
    return new RequestLoanViewModel_Factory(getLimitSummaryUseCaseProvider, getSavedBankAccountsUseCaseProvider, requestLoanUseCaseProvider, verifyPasswordUseCaseProvider, secureTokenStoreProvider, appContextProvider);
  }

  public static RequestLoanViewModel newInstance(GetLimitSummaryUseCase getLimitSummaryUseCase,
      GetSavedBankAccountsUseCase getSavedBankAccountsUseCase,
      RequestLoanUseCase requestLoanUseCase, VerifyPasswordUseCase verifyPasswordUseCase,
      SecureTokenStore secureTokenStore, Context appContext) {
    return new RequestLoanViewModel(getLimitSummaryUseCase, getSavedBankAccountsUseCase, requestLoanUseCase, verifyPasswordUseCase, secureTokenStore, appContext);
  }
}
