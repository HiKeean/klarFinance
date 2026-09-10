package com.klarfinance.app.domain.usecase;

import com.klarfinance.app.domain.repository.AuthRepository;
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
public final class RefreshSessionUseCase_Factory implements Factory<RefreshSessionUseCase> {
  private final Provider<AuthRepository> repositoryProvider;

  public RefreshSessionUseCase_Factory(Provider<AuthRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public RefreshSessionUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static RefreshSessionUseCase_Factory create(Provider<AuthRepository> repositoryProvider) {
    return new RefreshSessionUseCase_Factory(repositoryProvider);
  }

  public static RefreshSessionUseCase newInstance(AuthRepository repository) {
    return new RefreshSessionUseCase(repository);
  }
}
