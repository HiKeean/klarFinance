package com.klarfinance.app.di;

import com.klarfinance.app.data.local.KlarFinanceDatabase;
import com.klarfinance.app.data.local.VerifiedPhoneDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideVerifiedPhoneDaoFactory implements Factory<VerifiedPhoneDao> {
  private final Provider<KlarFinanceDatabase> databaseProvider;

  public DatabaseModule_ProvideVerifiedPhoneDaoFactory(
      Provider<KlarFinanceDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public VerifiedPhoneDao get() {
    return provideVerifiedPhoneDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideVerifiedPhoneDaoFactory create(
      Provider<KlarFinanceDatabase> databaseProvider) {
    return new DatabaseModule_ProvideVerifiedPhoneDaoFactory(databaseProvider);
  }

  public static VerifiedPhoneDao provideVerifiedPhoneDao(KlarFinanceDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideVerifiedPhoneDao(database));
  }
}
