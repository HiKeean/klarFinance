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
public final class VerifyPasswordUseCase_Factory implements Factory<VerifyPasswordUseCase> {
  private final Provider<AuthRepository> repositoryProvider;

  public VerifyPasswordUseCase_Factory(Provider<AuthRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public VerifyPasswordUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static VerifyPasswordUseCase_Factory create(Provider<AuthRepository> repositoryProvider) {
    return new VerifyPasswordUseCase_Factory(repositoryProvider);
  }

  public static VerifyPasswordUseCase newInstance(AuthRepository repository) {
    return new VerifyPasswordUseCase(repository);
  }
}
