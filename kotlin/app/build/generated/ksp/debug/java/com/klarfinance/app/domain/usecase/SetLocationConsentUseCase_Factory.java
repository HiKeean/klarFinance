package com.klarfinance.app.domain.usecase;

import com.klarfinance.app.domain.repository.LocationTrackingRepository;
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
public final class SetLocationConsentUseCase_Factory implements Factory<SetLocationConsentUseCase> {
  private final Provider<LocationTrackingRepository> repositoryProvider;

  public SetLocationConsentUseCase_Factory(
      Provider<LocationTrackingRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public SetLocationConsentUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static SetLocationConsentUseCase_Factory create(
      Provider<LocationTrackingRepository> repositoryProvider) {
    return new SetLocationConsentUseCase_Factory(repositoryProvider);
  }

  public static SetLocationConsentUseCase newInstance(LocationTrackingRepository repository) {
    return new SetLocationConsentUseCase(repository);
  }
}
