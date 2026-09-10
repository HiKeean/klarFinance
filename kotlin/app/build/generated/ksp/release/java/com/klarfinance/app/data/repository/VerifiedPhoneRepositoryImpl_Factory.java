package com.klarfinance.app.data.repository;

import com.klarfinance.app.data.local.VerifiedPhoneDao;
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
public final class VerifiedPhoneRepositoryImpl_Factory implements Factory<VerifiedPhoneRepositoryImpl> {
  private final Provider<VerifiedPhoneDao> daoProvider;

  public VerifiedPhoneRepositoryImpl_Factory(Provider<VerifiedPhoneDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public VerifiedPhoneRepositoryImpl get() {
    return newInstance(daoProvider.get());
  }

  public static VerifiedPhoneRepositoryImpl_Factory create(Provider<VerifiedPhoneDao> daoProvider) {
    return new VerifiedPhoneRepositoryImpl_Factory(daoProvider);
  }

  public static VerifiedPhoneRepositoryImpl newInstance(VerifiedPhoneDao dao) {
    return new VerifiedPhoneRepositoryImpl(dao);
  }
}
