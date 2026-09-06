package com.klarfinance.app.core.network;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class HmacInterceptor_Factory implements Factory<HmacInterceptor> {
  @Override
  public HmacInterceptor get() {
    return newInstance();
  }

  public static HmacInterceptor_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static HmacInterceptor newInstance() {
    return new HmacInterceptor();
  }

  private static final class InstanceHolder {
    private static final HmacInterceptor_Factory INSTANCE = new HmacInterceptor_Factory();
  }
}
