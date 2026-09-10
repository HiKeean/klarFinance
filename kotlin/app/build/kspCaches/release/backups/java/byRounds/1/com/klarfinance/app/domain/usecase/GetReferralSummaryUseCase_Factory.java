package com.klarfinance.app.domain.usecase;

import com.klarfinance.app.domain.repository.ReferralRepository;
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
public final class GetReferralSummaryUseCase_Factory implements Factory<GetReferralSummaryUseCase> {
  private final Provider<ReferralRepository> repositoryProvider;

  public GetReferralSummaryUseCase_Factory(Provider<ReferralRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public GetReferralSummaryUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static GetReferralSummaryUseCase_Factory create(
      Provider<ReferralRepository> repositoryProvider) {
    return new GetReferralSummaryUseCase_Factory(repositoryProvider);
  }

  public static GetReferralSummaryUseCase newInstance(ReferralRepository repository) {
    return new GetReferralSummaryUseCase(repository);
  }
}
