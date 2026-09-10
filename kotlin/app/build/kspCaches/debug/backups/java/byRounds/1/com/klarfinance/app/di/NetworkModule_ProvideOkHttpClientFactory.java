package com.klarfinance.app.di;

import com.chuckerteam.chucker.api.ChuckerInterceptor;
import com.klarfinance.app.core.network.AuthInterceptor;
import com.klarfinance.app.core.network.HmacInterceptor;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

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
public final class NetworkModule_ProvideOkHttpClientFactory implements Factory<OkHttpClient> {
  private final Provider<HmacInterceptor> hmacInterceptorProvider;

  private final Provider<AuthInterceptor> authInterceptorProvider;

  private final Provider<ChuckerInterceptor> chuckerInterceptorProvider;

  public NetworkModule_ProvideOkHttpClientFactory(Provider<HmacInterceptor> hmacInterceptorProvider,
      Provider<AuthInterceptor> authInterceptorProvider,
      Provider<ChuckerInterceptor> chuckerInterceptorProvider) {
    this.hmacInterceptorProvider = hmacInterceptorProvider;
    this.authInterceptorProvider = authInterceptorProvider;
    this.chuckerInterceptorProvider = chuckerInterceptorProvider;
  }

  @Override
  public OkHttpClient get() {
    return provideOkHttpClient(hmacInterceptorProvider.get(), authInterceptorProvider.get(), chuckerInterceptorProvider.get());
  }

  public static NetworkModule_ProvideOkHttpClientFactory create(
      Provider<HmacInterceptor> hmacInterceptorProvider,
      Provider<AuthInterceptor> authInterceptorProvider,
      Provider<ChuckerInterceptor> chuckerInterceptorProvider) {
    return new NetworkModule_ProvideOkHttpClientFactory(hmacInterceptorProvider, authInterceptorProvider, chuckerInterceptorProvider);
  }

  public static OkHttpClient provideOkHttpClient(HmacInterceptor hmacInterceptor,
      AuthInterceptor authInterceptor, ChuckerInterceptor chuckerInterceptor) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideOkHttpClient(hmacInterceptor, authInterceptor, chuckerInterceptor));
  }
}
