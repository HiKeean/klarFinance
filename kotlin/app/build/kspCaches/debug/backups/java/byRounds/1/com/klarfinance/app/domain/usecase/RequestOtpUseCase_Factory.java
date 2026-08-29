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
public final class RequestOtpUseCase_Factory implements Factory<RequestOtpUseCase> {
  private final Provider<AuthRepository> repositoryProvider;

  public RequestOtpUseCase_Factory(Provider<AuthRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public RequestOtpUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static RequestOtpUseCase_Factory create(Provider<AuthRepository> repositoryProvider) {
    return new RequestOtpUseCase_Factory(repositoryProvider);
  }

  public static RequestOtpUseCase newInstance(AuthRepository repository) {
    return new RequestOtpUseCase(repository);
  }
}
