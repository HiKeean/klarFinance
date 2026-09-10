package com.klarfinance.app.presentation.home;

import androidx.lifecycle.SavedStateHandle;
import com.klarfinance.app.core.notification.FcmEventBus;
import com.klarfinance.app.domain.usecase.GetLimitSummaryUseCase;
import com.klarfinance.app.domain.usecase.GetProfileUseCase;
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<GetProfileUseCase> getProfileUseCaseProvider;

  private final Provider<GetLimitSummaryUseCase> getLimitSummaryUseCaseProvider;

  private final Provider<FcmEventBus> fcmEventBusProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public HomeViewModel_Factory(Provider<GetProfileUseCase> getProfileUseCaseProvider,
      Provider<GetLimitSummaryUseCase> getLimitSummaryUseCaseProvider,
      Provider<FcmEventBus> fcmEventBusProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.getProfileUseCaseProvider = getProfileUseCaseProvider;
    this.getLimitSummaryUseCaseProvider = getLimitSummaryUseCaseProvider;
    this.fcmEventBusProvider = fcmEventBusProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(getProfileUseCaseProvider.get(), getLimitSummaryUseCaseProvider.get(), fcmEventBusProvider.get(), savedStateHandleProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<GetProfileUseCase> getProfileUseCaseProvider,
      Provider<GetLimitSummaryUseCase> getLimitSummaryUseCaseProvider,
      Provider<FcmEventBus> fcmEventBusProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new HomeViewModel_Factory(getProfileUseCaseProvider, getLimitSummaryUseCaseProvider, fcmEventBusProvider, savedStateHandleProvider);
  }

  public static HomeViewModel newInstance(GetProfileUseCase getProfileUseCase,
      GetLimitSummaryUseCase getLimitSummaryUseCase, FcmEventBus fcmEventBus,
      SavedStateHandle savedStateHandle) {
    return new HomeViewModel(getProfileUseCase, getLimitSummaryUseCase, fcmEventBus, savedStateHandle);
  }
}
