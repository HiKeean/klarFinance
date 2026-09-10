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
public final class ToggleTransjakartaTicketUsedUseCase_Factory implements Factory<ToggleTransjakartaTicketUsedUseCase> {
  private final Provider<TransjakartaRepository> repositoryProvider;

  public ToggleTransjakartaTicketUsedUseCase_Factory(
      Provider<TransjakartaRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public ToggleTransjakartaTicketUsedUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static ToggleTransjakartaTicketUsedUseCase_Factory create(
      Provider<TransjakartaRepository> repositoryProvider) {
    return new ToggleTransjakartaTicketUsedUseCase_Factory(repositoryProvider);
  }

  public static ToggleTransjakartaTicketUsedUseCase newInstance(TransjakartaRepository repository) {
    return new ToggleTransjakartaTicketUsedUseCase(repository);
  }
}
