package com.klarfinance.app.core.location;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class LocationScheduler_Factory implements Factory<LocationScheduler> {
  private final Provider<Context> contextProvider;

  public LocationScheduler_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public LocationScheduler get() {
    return newInstance(contextProvider.get());
  }

  public static LocationScheduler_Factory create(Provider<Context> contextProvider) {
    return new LocationScheduler_Factory(contextProvider);
  }

  public static LocationScheduler newInstance(Context context) {
    return new LocationScheduler(context);
  }
}
