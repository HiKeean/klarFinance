package com.klarfinance.app.presentation.referral;

import com.klarfinance.app.domain.usecase.GetReferralSummaryUseCase;
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
public final class ReferralViewModel_Factory implements Factory<ReferralViewModel> {
  private final Provider<GetReferralSummaryUseCase> getReferralSummaryUseCaseProvider;

  public ReferralViewModel_Factory(
      Provider<GetReferralSummaryUseCase> getReferralSummaryUseCaseProvider) {
    this.getReferralSummaryUseCaseProvider = getReferralSummaryUseCaseProvider;
  }

  @Override
  public ReferralViewModel get() {
    return newInstance(getReferralSummaryUseCaseProvider.get());
  }

  public static ReferralViewModel_Factory create(
      Provider<GetReferralSummaryUseCase> getReferralSummaryUseCaseProvider) {
    return new ReferralViewModel_Factory(getReferralSummaryUseCaseProvider);
  }

  public static ReferralViewModel newInstance(GetReferralSummaryUseCase getReferralSummaryUseCase) {
    return new ReferralViewModel(getReferralSummaryUseCase);
  }
}
