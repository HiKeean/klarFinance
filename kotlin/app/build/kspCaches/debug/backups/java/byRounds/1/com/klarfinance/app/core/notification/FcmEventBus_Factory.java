package com.klarfinance.app.core.notification;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class FcmEventBus_Factory implements Factory<FcmEventBus> {
  @Override
  public FcmEventBus get() {
    return newInstance();
  }

  public static FcmEventBus_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static FcmEventBus newInstance() {
    return new FcmEventBus();
  }

  private static final class InstanceHolder {
    private static final FcmEventBus_Factory INSTANCE = new FcmEventBus_Factory();
  }
}
