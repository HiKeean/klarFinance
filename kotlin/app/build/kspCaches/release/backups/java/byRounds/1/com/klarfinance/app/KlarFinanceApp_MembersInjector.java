package com.klarfinance.app;

import androidx.hilt.work.HiltWorkerFactory;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class KlarFinanceApp_MembersInjector implements MembersInjector<KlarFinanceApp> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  public KlarFinanceApp_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
  }

  public static MembersInjector<KlarFinanceApp> create(
      Provider<HiltWorkerFactory> workerFactoryProvider) {
    return new KlarFinanceApp_MembersInjector(workerFactoryProvider);
  }

  @Override
  public void injectMembers(KlarFinanceApp instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
  }

  @InjectedFieldSignature("com.klarfinance.app.KlarFinanceApp.workerFactory")
  public static void injectWorkerFactory(KlarFinanceApp instance, HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }
}
