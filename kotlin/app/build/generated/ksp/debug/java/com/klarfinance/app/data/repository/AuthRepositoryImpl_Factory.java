package com.klarfinance.app.data.repository;

import com.klarfinance.app.data.remote.AuthApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.serialization.json.Json;

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
public final class AuthRepositoryImpl_Factory implements Factory<AuthRepositoryImpl> {
  private final Provider<AuthApi> apiProvider;

  private final Provider<Json> jsonProvider;

  public AuthRepositoryImpl_Factory(Provider<AuthApi> apiProvider, Provider<Json> jsonProvider) {
    this.apiProvider = apiProvider;
    this.jsonProvider = jsonProvider;
  }

  @Override
  public AuthRepositoryImpl get() {
    return newInstance(apiProvider.get(), jsonProvider.get());
  }

  public static AuthRepositoryImpl_Factory create(Provider<AuthApi> apiProvider,
      Provider<Json> jsonProvider) {
    return new AuthRepositoryImpl_Factory(apiProvider, jsonProvider);
  }

  public static AuthRepositoryImpl newInstance(AuthApi api, Json json) {
    return new AuthRepositoryImpl(api, json);
  }
}
