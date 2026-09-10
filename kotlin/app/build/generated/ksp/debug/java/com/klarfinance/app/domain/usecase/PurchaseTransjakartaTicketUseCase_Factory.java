package com.klarfinance.app.domain.usecase;

import com.klarfinance.app.domain.repository.TransjakartaRepository;
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
public final class PurchaseTransjakartaTicketUseCase_Factory implements Factory<PurchaseTransjakartaTicketUseCase> {
  private final Provider<TransjakartaRepository> repositoryProvider;

  public PurchaseTransjakartaTicketUseCase_Factory(
      Provider<TransjakartaRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public PurchaseTransjakartaTicketUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static PurchaseTransjakartaTicketUseCase_Factory create(
      Provider<TransjakartaRepository> repositoryProvider) {
    return new PurchaseTransjakartaTicketUseCase_Factory(repositoryProvider);
  }

  public static PurchaseTransjakartaTicketUseCase newInstance(TransjakartaRepository repository) {
    return new PurchaseTransjakartaTicketUseCase(repository);
  }
}
