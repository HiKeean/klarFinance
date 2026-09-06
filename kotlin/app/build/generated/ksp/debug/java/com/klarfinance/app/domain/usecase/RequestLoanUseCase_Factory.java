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
public final class RequestLoanUseCase_Factory implements Factory<RequestLoanUseCase> {
  private final Provider<LoanRepository> repositoryProvider;

  public RequestLoanUseCase_Factory(Provider<LoanRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public RequestLoanUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static RequestLoanUseCase_Factory create(Provider<LoanRepository> repositoryProvider) {
    return new RequestLoanUseCase_Factory(repositoryProvider);
  }

  public static RequestLoanUseCase newInstance(LoanRepository repository) {
    return new RequestLoanUseCase(repository);
  }
}
