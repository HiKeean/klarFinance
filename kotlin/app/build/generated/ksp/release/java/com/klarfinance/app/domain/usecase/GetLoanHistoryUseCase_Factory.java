package com.klarfinance.app.domain.usecase;

import com.klarfinance.app.domain.repository.LoanRepository;
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
public final class GetLoanHistoryUseCase_Factory implements Factory<GetLoanHistoryUseCase> {
  private final Provider<LoanRepository> repositoryProvider;

  public GetLoanHistoryUseCase_Factory(Provider<LoanRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public GetLoanHistoryUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static GetLoanHistoryUseCase_Factory create(Provider<LoanRepository> repositoryProvider) {
    return new GetLoanHistoryUseCase_Factory(repositoryProvider);
  }

  public static GetLoanHistoryUseCase newInstance(LoanRepository repository) {
    return new GetLoanHistoryUseCase(repository);
  }
}
