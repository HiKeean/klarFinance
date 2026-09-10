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
public final class RepayLoanUseCase_Factory implements Factory<RepayLoanUseCase> {
  private final Provider<LoanRepository> repositoryProvider;

  public RepayLoanUseCase_Factory(Provider<LoanRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public RepayLoanUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static RepayLoanUseCase_Factory create(Provider<LoanRepository> repositoryProvider) {
    return new RepayLoanUseCase_Factory(repositoryProvider);
  }

  public static RepayLoanUseCase newInstance(LoanRepository repository) {
    return new RepayLoanUseCase(repository);
  }
}
