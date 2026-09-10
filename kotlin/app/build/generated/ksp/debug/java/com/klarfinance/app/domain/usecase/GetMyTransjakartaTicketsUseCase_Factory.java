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
public final class GetMyTransjakartaTicketsUseCase_Factory implements Factory<GetMyTransjakartaTicketsUseCase> {
  private final Provider<TransjakartaRepository> repositoryProvider;

  public GetMyTransjakartaTicketsUseCase_Factory(
      Provider<TransjakartaRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public GetMyTransjakartaTicketsUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static GetMyTransjakartaTicketsUseCase_Factory create(
      Provider<TransjakartaRepository> repositoryProvider) {
    return new GetMyTransjakartaTicketsUseCase_Factory(repositoryProvider);
  }

  public static GetMyTransjakartaTicketsUseCase newInstance(TransjakartaRepository repository) {
    return new GetMyTransjakartaTicketsUseCase(repository);
  }
}
