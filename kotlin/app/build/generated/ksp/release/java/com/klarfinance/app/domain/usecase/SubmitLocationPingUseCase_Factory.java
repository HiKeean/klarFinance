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
public final class SubmitLocationPingUseCase_Factory implements Factory<SubmitLocationPingUseCase> {
  private final Provider<LocationTrackingRepository> repositoryProvider;

  public SubmitLocationPingUseCase_Factory(
      Provider<LocationTrackingRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public SubmitLocationPingUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static SubmitLocationPingUseCase_Factory create(
      Provider<LocationTrackingRepository> repositoryProvider) {
    return new SubmitLocationPingUseCase_Factory(repositoryProvider);
  }

  public static SubmitLocationPingUseCase newInstance(LocationTrackingRepository repository) {
    return new SubmitLocationPingUseCase(repository);
  }
}
