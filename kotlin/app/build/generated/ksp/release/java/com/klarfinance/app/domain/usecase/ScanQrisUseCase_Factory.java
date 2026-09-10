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
public final class ScanQrisUseCase_Factory implements Factory<ScanQrisUseCase> {
  private final Provider<QrisRepository> repositoryProvider;

  public ScanQrisUseCase_Factory(Provider<QrisRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public ScanQrisUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static ScanQrisUseCase_Factory create(Provider<QrisRepository> repositoryProvider) {
    return new ScanQrisUseCase_Factory(repositoryProvider);
  }

  public static ScanQrisUseCase newInstance(QrisRepository repository) {
    return new ScanQrisUseCase(repository);
  }
}
