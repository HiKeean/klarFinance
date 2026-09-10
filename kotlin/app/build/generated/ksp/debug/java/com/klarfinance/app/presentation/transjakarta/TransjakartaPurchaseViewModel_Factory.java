package com.klarfinance.app.presentation.transjakarta;

import com.klarfinance.app.core.session.SecureTokenStore;
import com.klarfinance.app.domain.usecase.GetMyTransjakartaTicketsUseCase;
import com.klarfinance.app.domain.usecase.PurchaseTransjakartaTicketUseCase;
import com.klarfinance.app.domain.usecase.ToggleTransjakartaTicketUsedUseCase;
import com.klarfinance.app.domain.usecase.VerifyPasswordUseCase;
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
public final class TransjakartaPurchaseViewModel_Factory implements Factory<TransjakartaPurchaseViewModel> {
  private final Provider<PurchaseTransjakartaTicketUseCase> purchaseTicketUseCaseProvider;

  private final Provider<GetMyTransjakartaTicketsUseCase> getMyTicketsUseCaseProvider;

  private final Provider<ToggleTransjakartaTicketUsedUseCase> toggleUsedUseCaseProvider;

  private final Provider<VerifyPasswordUseCase> verifyPasswordUseCaseProvider;

  private final Provider<SecureTokenStore> secureTokenStoreProvider;

  public TransjakartaPurchaseViewModel_Factory(
      Provider<PurchaseTransjakartaTicketUseCase> purchaseTicketUseCaseProvider,
      Provider<GetMyTransjakartaTicketsUseCase> getMyTicketsUseCaseProvider,
      Provider<ToggleTransjakartaTicketUsedUseCase> toggleUsedUseCaseProvider,
      Provider<VerifyPasswordUseCase> verifyPasswordUseCaseProvider,
      Provider<SecureTokenStore> secureTokenStoreProvider) {
    this.purchaseTicketUseCaseProvider = purchaseTicketUseCaseProvider;
    this.getMyTicketsUseCaseProvider = getMyTicketsUseCaseProvider;
    this.toggleUsedUseCaseProvider = toggleUsedUseCaseProvider;
    this.verifyPasswordUseCaseProvider = verifyPasswordUseCaseProvider;
    this.secureTokenStoreProvider = secureTokenStoreProvider;
  }

  @Override
  public TransjakartaPurchaseViewModel get() {
    return newInstance(purchaseTicketUseCaseProvider.get(), getMyTicketsUseCaseProvider.get(), toggleUsedUseCaseProvider.get(), verifyPasswordUseCaseProvider.get(), secureTokenStoreProvider.get());
  }

  public static TransjakartaPurchaseViewModel_Factory create(
      Provider<PurchaseTransjakartaTicketUseCase> purchaseTicketUseCaseProvider,
      Provider<GetMyTransjakartaTicketsUseCase> getMyTicketsUseCaseProvider,
      Provider<ToggleTransjakartaTicketUsedUseCase> toggleUsedUseCaseProvider,
      Provider<VerifyPasswordUseCase> verifyPasswordUseCaseProvider,
      Provider<SecureTokenStore> secureTokenStoreProvider) {
    return new TransjakartaPurchaseViewModel_Factory(purchaseTicketUseCaseProvider, getMyTicketsUseCaseProvider, toggleUsedUseCaseProvider, verifyPasswordUseCaseProvider, secureTokenStoreProvider);
  }

  public static TransjakartaPurchaseViewModel newInstance(
      PurchaseTransjakartaTicketUseCase purchaseTicketUseCase,
      GetMyTransjakartaTicketsUseCase getMyTicketsUseCase,
      ToggleTransjakartaTicketUsedUseCase toggleUsedUseCase,
      VerifyPasswordUseCase verifyPasswordUseCase, SecureTokenStore secureTokenStore) {
    return new TransjakartaPurchaseViewModel(purchaseTicketUseCase, getMyTicketsUseCase, toggleUsedUseCase, verifyPasswordUseCase, secureTokenStore);
  }
}
