package com.klarfinance.app.core.notification;

import com.klarfinance.app.core.session.SessionManager;
import com.klarfinance.app.domain.repository.AuthRepository;
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
public final class KlarFirebaseMessagingService_MembersInjector implements MembersInjector<KlarFirebaseMessagingService> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<SessionManager> sessionManagerProvider;

  private final Provider<FcmEventBus> fcmEventBusProvider;

  public KlarFirebaseMessagingService_MembersInjector(
      Provider<AuthRepository> authRepositoryProvider,
      Provider<SessionManager> sessionManagerProvider, Provider<FcmEventBus> fcmEventBusProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.sessionManagerProvider = sessionManagerProvider;
    this.fcmEventBusProvider = fcmEventBusProvider;
  }

  public static MembersInjector<KlarFirebaseMessagingService> create(
      Provider<AuthRepository> authRepositoryProvider,
      Provider<SessionManager> sessionManagerProvider, Provider<FcmEventBus> fcmEventBusProvider) {
    return new KlarFirebaseMessagingService_MembersInjector(authRepositoryProvider, sessionManagerProvider, fcmEventBusProvider);
  }

  @Override
  public void injectMembers(KlarFirebaseMessagingService instance) {
    injectAuthRepository(instance, authRepositoryProvider.get());
    injectSessionManager(instance, sessionManagerProvider.get());
    injectFcmEventBus(instance, fcmEventBusProvider.get());
  }

  @InjectedFieldSignature("com.klarfinance.app.core.notification.KlarFirebaseMessagingService.authRepository")
  public static void injectAuthRepository(KlarFirebaseMessagingService instance,
      AuthRepository authRepository) {
    instance.authRepository = authRepository;
  }

  @InjectedFieldSignature("com.klarfinance.app.core.notification.KlarFirebaseMessagingService.sessionManager")
  public static void injectSessionManager(KlarFirebaseMessagingService instance,
      SessionManager sessionManager) {
    instance.sessionManager = sessionManager;
  }

  @InjectedFieldSignature("com.klarfinance.app.core.notification.KlarFirebaseMessagingService.fcmEventBus")
  public static void injectFcmEventBus(KlarFirebaseMessagingService instance,
      FcmEventBus fcmEventBus) {
    instance.fcmEventBus = fcmEventBus;
  }
}
