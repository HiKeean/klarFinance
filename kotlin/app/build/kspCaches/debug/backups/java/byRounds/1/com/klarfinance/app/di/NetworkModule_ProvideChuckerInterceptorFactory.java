package com.klarfinance.app.di;

import android.content.Context;
import com.chuckerteam.chucker.api.ChuckerInterceptor;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class NetworkModule_ProvideChuckerInterceptorFactory implements Factory<ChuckerInterceptor> {
  private final Provider<Context> contextProvider;

  public NetworkModule_ProvideChuckerInterceptorFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ChuckerInterceptor get() {
    return provideChuckerInterceptor(contextProvider.get());
  }

  public static NetworkModule_ProvideChuckerInterceptorFactory create(
      Provider<Context> contextProvider) {
    return new NetworkModule_ProvideChuckerInterceptorFactory(contextProvider);
  }

  public static ChuckerInterceptor provideChuckerInterceptor(Context context) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideChuckerInterceptor(context));
  }
}
