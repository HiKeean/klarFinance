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
public final class RegisterUseCase_Factory implements Factory<RegisterUseCase> {
  private final Provider<AuthRepository> repositoryProvider;

  public RegisterUseCase_Factory(Provider<AuthRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public RegisterUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static RegisterUseCase_Factory create(Provider<AuthRepository> repositoryProvider) {
    return new RegisterUseCase_Factory(repositoryProvider);
  }

  public static RegisterUseCase newInstance(AuthRepository repository) {
    return new RegisterUseCase(repository);
  }
}
