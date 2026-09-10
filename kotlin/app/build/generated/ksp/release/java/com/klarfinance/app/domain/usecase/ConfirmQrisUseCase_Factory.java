package com.klarfinance.app.domain.usecase;

import com.klarfinance.app.domain.repository.QrisRepository;
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
public final class ConfirmQrisUseCase_Factory implements Factory<ConfirmQrisUseCase> {
  private final Provider<QrisRepository> repositoryProvider;

  public ConfirmQrisUseCase_Factory(Provider<QrisRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public ConfirmQrisUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static ConfirmQrisUseCase_Factory create(Provider<QrisRepository> repositoryProvider) {
    return new ConfirmQrisUseCase_Factory(repositoryProvider);
  }

  public static ConfirmQrisUseCase newInstance(QrisRepository repository) {
    return new ConfirmQrisUseCase(repository);
  }
}
