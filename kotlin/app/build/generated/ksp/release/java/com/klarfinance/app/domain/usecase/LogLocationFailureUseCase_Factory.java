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
public final class LogLocationFailureUseCase_Factory implements Factory<LogLocationFailureUseCase> {
  private final Provider<LocationTrackingRepository> repositoryProvider;

  public LogLocationFailureUseCase_Factory(
      Provider<LocationTrackingRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public LogLocationFailureUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static LogLocationFailureUseCase_Factory create(
      Provider<LocationTrackingRepository> repositoryProvider) {
    return new LogLocationFailureUseCase_Factory(repositoryProvider);
  }

  public static LogLocationFailureUseCase newInstance(LocationTrackingRepository repository) {
    return new LogLocationFailureUseCase(repository);
  }
}
