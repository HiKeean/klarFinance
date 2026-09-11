package com.klarfinance.app;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.hilt.work.WorkerAssistedFactory;
import androidx.hilt.work.WorkerFactoryModule_ProvideFactoryFactory;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;
import com.chuckerteam.chucker.api.ChuckerInterceptor;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.klarfinance.app.core.location.LocationCaptureWorker;
import com.klarfinance.app.core.location.LocationCaptureWorker_AssistedFactory;
import com.klarfinance.app.core.location.LocationScheduler;
import com.klarfinance.app.core.network.ApiService;
import com.klarfinance.app.core.network.AuthInterceptor;
import com.klarfinance.app.core.network.HmacInterceptor;
import com.klarfinance.app.core.notification.FcmEventBus;
import com.klarfinance.app.core.notification.KlarFirebaseMessagingService;
import com.klarfinance.app.core.notification.KlarFirebaseMessagingService_MembersInjector;
import com.klarfinance.app.core.session.SecureTokenStore;
import com.klarfinance.app.core.session.SessionManager;
import com.klarfinance.app.data.local.KlarFinanceDatabase;
import com.klarfinance.app.data.local.VerifiedPhoneDao;
import com.klarfinance.app.data.repository.AuthRepositoryImpl;
import com.klarfinance.app.data.repository.LoanRepositoryImpl;
import com.klarfinance.app.data.repository.LocationRepositoryImpl;
import com.klarfinance.app.data.repository.LocationTrackingRepositoryImpl;
import com.klarfinance.app.data.repository.QrisRepositoryImpl;
import com.klarfinance.app.data.repository.ReferralRepositoryImpl;
import com.klarfinance.app.data.repository.TransjakartaRepositoryImpl;
import com.klarfinance.app.data.repository.VerifiedPhoneRepositoryImpl;
import com.klarfinance.app.di.DatabaseModule_ProvideDatabaseFactory;
import com.klarfinance.app.di.DatabaseModule_ProvideVerifiedPhoneDaoFactory;
import com.klarfinance.app.di.NetworkModule_ProvideChuckerInterceptorFactory;
import com.klarfinance.app.di.NetworkModule_ProvideJsonFactory;
import com.klarfinance.app.di.NetworkModule_ProvideOkHttpClientFactory;
import com.klarfinance.app.domain.repository.AuthRepository;
import com.klarfinance.app.domain.repository.LoanRepository;
import com.klarfinance.app.domain.repository.LocationRepository;
import com.klarfinance.app.domain.repository.LocationTrackingRepository;
import com.klarfinance.app.domain.repository.QrisRepository;
import com.klarfinance.app.domain.repository.ReferralRepository;
import com.klarfinance.app.domain.repository.TransjakartaRepository;
import com.klarfinance.app.domain.repository.VerifiedPhoneRepository;
import com.klarfinance.app.domain.usecase.ChangePasswordUseCase;
import com.klarfinance.app.domain.usecase.CheckPhoneRegisteredUseCase;
import com.klarfinance.app.domain.usecase.ConfirmQrisUseCase;
import com.klarfinance.app.domain.usecase.GetLimitSummaryUseCase;
import com.klarfinance.app.domain.usecase.GetLoanHistoryUseCase;
import com.klarfinance.app.domain.usecase.GetLocationConsentUseCase;
import com.klarfinance.app.domain.usecase.GetMyTransjakartaTicketsUseCase;
import com.klarfinance.app.domain.usecase.GetProfileUseCase;
import com.klarfinance.app.domain.usecase.GetReferralSummaryUseCase;
import com.klarfinance.app.domain.usecase.GetSavedBankAccountsUseCase;
import com.klarfinance.app.domain.usecase.LogLocationFailureUseCase;
import com.klarfinance.app.domain.usecase.LoginUseCase;
import com.klarfinance.app.domain.usecase.LogoutUseCase;
import com.klarfinance.app.domain.usecase.PurchaseTransjakartaTicketUseCase;
import com.klarfinance.app.domain.usecase.RefreshSessionUseCase;
import com.klarfinance.app.domain.usecase.RegisterUseCase;
import com.klarfinance.app.domain.usecase.RepayLoanUseCase;
import com.klarfinance.app.domain.usecase.RequestLoanUseCase;
import com.klarfinance.app.domain.usecase.RequestOtpUseCase;
import com.klarfinance.app.domain.usecase.ScanQrisUseCase;
import com.klarfinance.app.domain.usecase.SetLocationConsentUseCase;
import com.klarfinance.app.domain.usecase.SubmitLocationPingUseCase;
import com.klarfinance.app.domain.usecase.ToggleTransjakartaTicketUsedUseCase;
import com.klarfinance.app.domain.usecase.VerifyOtpUseCase;
import com.klarfinance.app.domain.usecase.VerifyPasswordUseCase;
import com.klarfinance.app.presentation.account.AccountViewModel;
import com.klarfinance.app.presentation.account.AccountViewModel_HiltModules;
import com.klarfinance.app.presentation.history.HistoryViewModel;
import com.klarfinance.app.presentation.history.HistoryViewModel_HiltModules;
import com.klarfinance.app.presentation.home.HomeViewModel;
import com.klarfinance.app.presentation.home.HomeViewModel_HiltModules;
import com.klarfinance.app.presentation.loan.RequestLoanViewModel;
import com.klarfinance.app.presentation.loan.RequestLoanViewModel_HiltModules;
import com.klarfinance.app.presentation.login.LoginViewModel;
import com.klarfinance.app.presentation.login.LoginViewModel_HiltModules;
import com.klarfinance.app.presentation.otp.OtpViewModel;
import com.klarfinance.app.presentation.otp.OtpViewModel_HiltModules;
import com.klarfinance.app.presentation.passwordlogin.PasswordLoginViewModel;
import com.klarfinance.app.presentation.passwordlogin.PasswordLoginViewModel_HiltModules;
import com.klarfinance.app.presentation.qris.amount.QrisViewModel;
import com.klarfinance.app.presentation.qris.amount.QrisViewModel_HiltModules;
import com.klarfinance.app.presentation.referral.ReferralViewModel;
import com.klarfinance.app.presentation.referral.ReferralViewModel_HiltModules;
import com.klarfinance.app.presentation.register.RegisterViewModel;
import com.klarfinance.app.presentation.register.RegisterViewModel_HiltModules;
import com.klarfinance.app.presentation.splash.SplashViewModel;
import com.klarfinance.app.presentation.splash.SplashViewModel_HiltModules;
import com.klarfinance.app.presentation.transjakarta.TransjakartaPurchaseViewModel;
import com.klarfinance.app.presentation.transjakarta.TransjakartaPurchaseViewModel_HiltModules;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.MapBuilder;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.SingleCheck;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import kotlinx.serialization.json.Json;
import okhttp3.OkHttpClient;

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
public final class DaggerKlarFinanceApp_HiltComponents_SingletonC {
  private DaggerKlarFinanceApp_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public KlarFinanceApp_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements KlarFinanceApp_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public KlarFinanceApp_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements KlarFinanceApp_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public KlarFinanceApp_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements KlarFinanceApp_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public KlarFinanceApp_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements KlarFinanceApp_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public KlarFinanceApp_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements KlarFinanceApp_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public KlarFinanceApp_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements KlarFinanceApp_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public KlarFinanceApp_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements KlarFinanceApp_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public KlarFinanceApp_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends KlarFinanceApp_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends KlarFinanceApp_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends KlarFinanceApp_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends KlarFinanceApp_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(MapBuilder.<String, Boolean>newMapBuilder(12).put(LazyClassKeyProvider.com_klarfinance_app_presentation_account_AccountViewModel, AccountViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_klarfinance_app_presentation_history_HistoryViewModel, HistoryViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_klarfinance_app_presentation_home_HomeViewModel, HomeViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_klarfinance_app_presentation_login_LoginViewModel, LoginViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_klarfinance_app_presentation_otp_OtpViewModel, OtpViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_klarfinance_app_presentation_passwordlogin_PasswordLoginViewModel, PasswordLoginViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_klarfinance_app_presentation_qris_amount_QrisViewModel, QrisViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_klarfinance_app_presentation_referral_ReferralViewModel, ReferralViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_klarfinance_app_presentation_register_RegisterViewModel, RegisterViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_klarfinance_app_presentation_loan_RequestLoanViewModel, RequestLoanViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_klarfinance_app_presentation_splash_SplashViewModel, SplashViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_klarfinance_app_presentation_transjakarta_TransjakartaPurchaseViewModel, TransjakartaPurchaseViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_klarfinance_app_presentation_home_HomeViewModel = "com.klarfinance.app.presentation.home.HomeViewModel";

      static String com_klarfinance_app_presentation_splash_SplashViewModel = "com.klarfinance.app.presentation.splash.SplashViewModel";

      static String com_klarfinance_app_presentation_history_HistoryViewModel = "com.klarfinance.app.presentation.history.HistoryViewModel";

      static String com_klarfinance_app_presentation_account_AccountViewModel = "com.klarfinance.app.presentation.account.AccountViewModel";

      static String com_klarfinance_app_presentation_otp_OtpViewModel = "com.klarfinance.app.presentation.otp.OtpViewModel";

      static String com_klarfinance_app_presentation_login_LoginViewModel = "com.klarfinance.app.presentation.login.LoginViewModel";

      static String com_klarfinance_app_presentation_register_RegisterViewModel = "com.klarfinance.app.presentation.register.RegisterViewModel";

      static String com_klarfinance_app_presentation_transjakarta_TransjakartaPurchaseViewModel = "com.klarfinance.app.presentation.transjakarta.TransjakartaPurchaseViewModel";

      static String com_klarfinance_app_presentation_qris_amount_QrisViewModel = "com.klarfinance.app.presentation.qris.amount.QrisViewModel";

      static String com_klarfinance_app_presentation_loan_RequestLoanViewModel = "com.klarfinance.app.presentation.loan.RequestLoanViewModel";

      static String com_klarfinance_app_presentation_passwordlogin_PasswordLoginViewModel = "com.klarfinance.app.presentation.passwordlogin.PasswordLoginViewModel";

      static String com_klarfinance_app_presentation_referral_ReferralViewModel = "com.klarfinance.app.presentation.referral.ReferralViewModel";

      @KeepFieldType
      HomeViewModel com_klarfinance_app_presentation_home_HomeViewModel2;

      @KeepFieldType
      SplashViewModel com_klarfinance_app_presentation_splash_SplashViewModel2;

      @KeepFieldType
      HistoryViewModel com_klarfinance_app_presentation_history_HistoryViewModel2;

      @KeepFieldType
      AccountViewModel com_klarfinance_app_presentation_account_AccountViewModel2;

      @KeepFieldType
      OtpViewModel com_klarfinance_app_presentation_otp_OtpViewModel2;

      @KeepFieldType
      LoginViewModel com_klarfinance_app_presentation_login_LoginViewModel2;

      @KeepFieldType
      RegisterViewModel com_klarfinance_app_presentation_register_RegisterViewModel2;

      @KeepFieldType
      TransjakartaPurchaseViewModel com_klarfinance_app_presentation_transjakarta_TransjakartaPurchaseViewModel2;

      @KeepFieldType
      QrisViewModel com_klarfinance_app_presentation_qris_amount_QrisViewModel2;

      @KeepFieldType
      RequestLoanViewModel com_klarfinance_app_presentation_loan_RequestLoanViewModel2;

      @KeepFieldType
      PasswordLoginViewModel com_klarfinance_app_presentation_passwordlogin_PasswordLoginViewModel2;

      @KeepFieldType
      ReferralViewModel com_klarfinance_app_presentation_referral_ReferralViewModel2;
    }
  }

  private static final class ViewModelCImpl extends KlarFinanceApp_HiltComponents.ViewModelC {
    private final SavedStateHandle savedStateHandle;

    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<AccountViewModel> accountViewModelProvider;

    private Provider<HistoryViewModel> historyViewModelProvider;

    private Provider<HomeViewModel> homeViewModelProvider;

    private Provider<LoginViewModel> loginViewModelProvider;

    private Provider<OtpViewModel> otpViewModelProvider;

    private Provider<PasswordLoginViewModel> passwordLoginViewModelProvider;

    private Provider<QrisViewModel> qrisViewModelProvider;

    private Provider<ReferralViewModel> referralViewModelProvider;

    private Provider<RegisterViewModel> registerViewModelProvider;

    private Provider<RequestLoanViewModel> requestLoanViewModelProvider;

    private Provider<SplashViewModel> splashViewModelProvider;

    private Provider<TransjakartaPurchaseViewModel> transjakartaPurchaseViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.savedStateHandle = savedStateHandleParam;
      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    private GetProfileUseCase getProfileUseCase() {
      return new GetProfileUseCase(singletonCImpl.bindAuthRepositoryProvider.get());
    }

    private ChangePasswordUseCase changePasswordUseCase() {
      return new ChangePasswordUseCase(singletonCImpl.bindAuthRepositoryProvider.get());
    }

    private LogoutUseCase logoutUseCase() {
      return new LogoutUseCase(singletonCImpl.bindAuthRepositoryProvider.get());
    }

    private GetLocationConsentUseCase getLocationConsentUseCase() {
      return new GetLocationConsentUseCase(singletonCImpl.bindLocationTrackingRepositoryProvider.get());
    }

    private SetLocationConsentUseCase setLocationConsentUseCase() {
      return new SetLocationConsentUseCase(singletonCImpl.bindLocationTrackingRepositoryProvider.get());
    }

    private GetLoanHistoryUseCase getLoanHistoryUseCase() {
      return new GetLoanHistoryUseCase(singletonCImpl.bindLoanRepositoryProvider.get());
    }

    private RepayLoanUseCase repayLoanUseCase() {
      return new RepayLoanUseCase(singletonCImpl.bindLoanRepositoryProvider.get());
    }

    private GetLimitSummaryUseCase getLimitSummaryUseCase() {
      return new GetLimitSummaryUseCase(singletonCImpl.bindLoanRepositoryProvider.get());
    }

    private RequestOtpUseCase requestOtpUseCase() {
      return new RequestOtpUseCase(singletonCImpl.bindAuthRepositoryProvider.get());
    }

    private CheckPhoneRegisteredUseCase checkPhoneRegisteredUseCase() {
      return new CheckPhoneRegisteredUseCase(singletonCImpl.bindAuthRepositoryProvider.get());
    }

    private VerifyOtpUseCase verifyOtpUseCase() {
      return new VerifyOtpUseCase(singletonCImpl.bindAuthRepositoryProvider.get());
    }

    private LoginUseCase loginUseCase() {
      return new LoginUseCase(singletonCImpl.bindAuthRepositoryProvider.get());
    }

    private ScanQrisUseCase scanQrisUseCase() {
      return new ScanQrisUseCase(singletonCImpl.bindQrisRepositoryProvider.get());
    }

    private ConfirmQrisUseCase confirmQrisUseCase() {
      return new ConfirmQrisUseCase(singletonCImpl.bindQrisRepositoryProvider.get());
    }

    private VerifyPasswordUseCase verifyPasswordUseCase() {
      return new VerifyPasswordUseCase(singletonCImpl.bindAuthRepositoryProvider.get());
    }

    private GetReferralSummaryUseCase getReferralSummaryUseCase() {
      return new GetReferralSummaryUseCase(singletonCImpl.bindReferralRepositoryProvider.get());
    }

    private RegisterUseCase registerUseCase() {
      return new RegisterUseCase(singletonCImpl.bindAuthRepositoryProvider.get());
    }

    private GetSavedBankAccountsUseCase getSavedBankAccountsUseCase() {
      return new GetSavedBankAccountsUseCase(singletonCImpl.bindLoanRepositoryProvider.get());
    }

    private RequestLoanUseCase requestLoanUseCase() {
      return new RequestLoanUseCase(singletonCImpl.bindLoanRepositoryProvider.get());
    }

    private RefreshSessionUseCase refreshSessionUseCase() {
      return new RefreshSessionUseCase(singletonCImpl.bindAuthRepositoryProvider.get());
    }

    private PurchaseTransjakartaTicketUseCase purchaseTransjakartaTicketUseCase() {
      return new PurchaseTransjakartaTicketUseCase(singletonCImpl.bindTransjakartaRepositoryProvider.get());
    }

    private GetMyTransjakartaTicketsUseCase getMyTransjakartaTicketsUseCase() {
      return new GetMyTransjakartaTicketsUseCase(singletonCImpl.bindTransjakartaRepositoryProvider.get());
    }

    private ToggleTransjakartaTicketUsedUseCase toggleTransjakartaTicketUsedUseCase() {
      return new ToggleTransjakartaTicketUsedUseCase(singletonCImpl.bindTransjakartaRepositoryProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.accountViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.historyViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.homeViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.loginViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.otpViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.passwordLoginViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.qrisViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
      this.referralViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 7);
      this.registerViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 8);
      this.requestLoanViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 9);
      this.splashViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 10);
      this.transjakartaPurchaseViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 11);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(MapBuilder.<String, javax.inject.Provider<ViewModel>>newMapBuilder(12).put(LazyClassKeyProvider.com_klarfinance_app_presentation_account_AccountViewModel, ((Provider) accountViewModelProvider)).put(LazyClassKeyProvider.com_klarfinance_app_presentation_history_HistoryViewModel, ((Provider) historyViewModelProvider)).put(LazyClassKeyProvider.com_klarfinance_app_presentation_home_HomeViewModel, ((Provider) homeViewModelProvider)).put(LazyClassKeyProvider.com_klarfinance_app_presentation_login_LoginViewModel, ((Provider) loginViewModelProvider)).put(LazyClassKeyProvider.com_klarfinance_app_presentation_otp_OtpViewModel, ((Provider) otpViewModelProvider)).put(LazyClassKeyProvider.com_klarfinance_app_presentation_passwordlogin_PasswordLoginViewModel, ((Provider) passwordLoginViewModelProvider)).put(LazyClassKeyProvider.com_klarfinance_app_presentation_qris_amount_QrisViewModel, ((Provider) qrisViewModelProvider)).put(LazyClassKeyProvider.com_klarfinance_app_presentation_referral_ReferralViewModel, ((Provider) referralViewModelProvider)).put(LazyClassKeyProvider.com_klarfinance_app_presentation_register_RegisterViewModel, ((Provider) registerViewModelProvider)).put(LazyClassKeyProvider.com_klarfinance_app_presentation_loan_RequestLoanViewModel, ((Provider) requestLoanViewModelProvider)).put(LazyClassKeyProvider.com_klarfinance_app_presentation_splash_SplashViewModel, ((Provider) splashViewModelProvider)).put(LazyClassKeyProvider.com_klarfinance_app_presentation_transjakarta_TransjakartaPurchaseViewModel, ((Provider) transjakartaPurchaseViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return Collections.<Class<?>, Object>emptyMap();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_klarfinance_app_presentation_history_HistoryViewModel = "com.klarfinance.app.presentation.history.HistoryViewModel";

      static String com_klarfinance_app_presentation_qris_amount_QrisViewModel = "com.klarfinance.app.presentation.qris.amount.QrisViewModel";

      static String com_klarfinance_app_presentation_splash_SplashViewModel = "com.klarfinance.app.presentation.splash.SplashViewModel";

      static String com_klarfinance_app_presentation_transjakarta_TransjakartaPurchaseViewModel = "com.klarfinance.app.presentation.transjakarta.TransjakartaPurchaseViewModel";

      static String com_klarfinance_app_presentation_otp_OtpViewModel = "com.klarfinance.app.presentation.otp.OtpViewModel";

      static String com_klarfinance_app_presentation_register_RegisterViewModel = "com.klarfinance.app.presentation.register.RegisterViewModel";

      static String com_klarfinance_app_presentation_referral_ReferralViewModel = "com.klarfinance.app.presentation.referral.ReferralViewModel";

      static String com_klarfinance_app_presentation_loan_RequestLoanViewModel = "com.klarfinance.app.presentation.loan.RequestLoanViewModel";

      static String com_klarfinance_app_presentation_passwordlogin_PasswordLoginViewModel = "com.klarfinance.app.presentation.passwordlogin.PasswordLoginViewModel";

      static String com_klarfinance_app_presentation_home_HomeViewModel = "com.klarfinance.app.presentation.home.HomeViewModel";

      static String com_klarfinance_app_presentation_account_AccountViewModel = "com.klarfinance.app.presentation.account.AccountViewModel";

      static String com_klarfinance_app_presentation_login_LoginViewModel = "com.klarfinance.app.presentation.login.LoginViewModel";

      @KeepFieldType
      HistoryViewModel com_klarfinance_app_presentation_history_HistoryViewModel2;

      @KeepFieldType
      QrisViewModel com_klarfinance_app_presentation_qris_amount_QrisViewModel2;

      @KeepFieldType
      SplashViewModel com_klarfinance_app_presentation_splash_SplashViewModel2;

      @KeepFieldType
      TransjakartaPurchaseViewModel com_klarfinance_app_presentation_transjakarta_TransjakartaPurchaseViewModel2;

      @KeepFieldType
      OtpViewModel com_klarfinance_app_presentation_otp_OtpViewModel2;

      @KeepFieldType
      RegisterViewModel com_klarfinance_app_presentation_register_RegisterViewModel2;

      @KeepFieldType
      ReferralViewModel com_klarfinance_app_presentation_referral_ReferralViewModel2;

      @KeepFieldType
      RequestLoanViewModel com_klarfinance_app_presentation_loan_RequestLoanViewModel2;

      @KeepFieldType
      PasswordLoginViewModel com_klarfinance_app_presentation_passwordlogin_PasswordLoginViewModel2;

      @KeepFieldType
      HomeViewModel com_klarfinance_app_presentation_home_HomeViewModel2;

      @KeepFieldType
      AccountViewModel com_klarfinance_app_presentation_account_AccountViewModel2;

      @KeepFieldType
      LoginViewModel com_klarfinance_app_presentation_login_LoginViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.klarfinance.app.presentation.account.AccountViewModel 
          return (T) new AccountViewModel(viewModelCImpl.getProfileUseCase(), viewModelCImpl.changePasswordUseCase(), viewModelCImpl.logoutUseCase(), singletonCImpl.sessionManagerProvider.get(), singletonCImpl.secureTokenStoreProvider.get(), viewModelCImpl.getLocationConsentUseCase(), viewModelCImpl.setLocationConsentUseCase(), singletonCImpl.logLocationFailureUseCase(), singletonCImpl.locationSchedulerProvider.get());

          case 1: // com.klarfinance.app.presentation.history.HistoryViewModel 
          return (T) new HistoryViewModel(viewModelCImpl.getLoanHistoryUseCase(), viewModelCImpl.repayLoanUseCase());

          case 2: // com.klarfinance.app.presentation.home.HomeViewModel 
          return (T) new HomeViewModel(viewModelCImpl.getProfileUseCase(), viewModelCImpl.getLimitSummaryUseCase(), singletonCImpl.fcmEventBusProvider.get(), viewModelCImpl.savedStateHandle);

          case 3: // com.klarfinance.app.presentation.login.LoginViewModel 
          return (T) new LoginViewModel(viewModelCImpl.requestOtpUseCase(), singletonCImpl.bindVerifiedPhoneRepositoryProvider.get(), viewModelCImpl.checkPhoneRegisteredUseCase());

          case 4: // com.klarfinance.app.presentation.otp.OtpViewModel 
          return (T) new OtpViewModel(viewModelCImpl.verifyOtpUseCase(), viewModelCImpl.requestOtpUseCase(), singletonCImpl.bindVerifiedPhoneRepositoryProvider.get(), viewModelCImpl.checkPhoneRegisteredUseCase(), viewModelCImpl.savedStateHandle);

          case 5: // com.klarfinance.app.presentation.passwordlogin.PasswordLoginViewModel 
          return (T) new PasswordLoginViewModel(viewModelCImpl.loginUseCase(), viewModelCImpl.savedStateHandle);

          case 6: // com.klarfinance.app.presentation.qris.amount.QrisViewModel 
          return (T) new QrisViewModel(viewModelCImpl.scanQrisUseCase(), viewModelCImpl.confirmQrisUseCase(), viewModelCImpl.getLimitSummaryUseCase(), viewModelCImpl.verifyPasswordUseCase(), singletonCImpl.secureTokenStoreProvider.get(), viewModelCImpl.savedStateHandle);

          case 7: // com.klarfinance.app.presentation.referral.ReferralViewModel 
          return (T) new ReferralViewModel(viewModelCImpl.getReferralSummaryUseCase());

          case 8: // com.klarfinance.app.presentation.register.RegisterViewModel 
          return (T) new RegisterViewModel(singletonCImpl.bindLocationRepositoryProvider.get(), viewModelCImpl.registerUseCase(), viewModelCImpl.loginUseCase(), ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), viewModelCImpl.savedStateHandle);

          case 9: // com.klarfinance.app.presentation.loan.RequestLoanViewModel 
          return (T) new RequestLoanViewModel(viewModelCImpl.getLimitSummaryUseCase(), viewModelCImpl.getSavedBankAccountsUseCase(), viewModelCImpl.requestLoanUseCase(), viewModelCImpl.verifyPasswordUseCase(), singletonCImpl.secureTokenStoreProvider.get(), ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 10: // com.klarfinance.app.presentation.splash.SplashViewModel 
          return (T) new SplashViewModel(singletonCImpl.secureTokenStoreProvider.get(), viewModelCImpl.refreshSessionUseCase());

          case 11: // com.klarfinance.app.presentation.transjakarta.TransjakartaPurchaseViewModel 
          return (T) new TransjakartaPurchaseViewModel(viewModelCImpl.purchaseTransjakartaTicketUseCase(), viewModelCImpl.getMyTransjakartaTicketsUseCase(), viewModelCImpl.toggleTransjakartaTicketUsedUseCase(), viewModelCImpl.verifyPasswordUseCase(), singletonCImpl.secureTokenStoreProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends KlarFinanceApp_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends KlarFinanceApp_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }

    @Override
    public void injectKlarFirebaseMessagingService(
        KlarFirebaseMessagingService klarFirebaseMessagingService) {
      injectKlarFirebaseMessagingService2(klarFirebaseMessagingService);
    }

    @CanIgnoreReturnValue
    private KlarFirebaseMessagingService injectKlarFirebaseMessagingService2(
        KlarFirebaseMessagingService instance) {
      KlarFirebaseMessagingService_MembersInjector.injectAuthRepository(instance, singletonCImpl.bindAuthRepositoryProvider.get());
      KlarFirebaseMessagingService_MembersInjector.injectSessionManager(instance, singletonCImpl.sessionManagerProvider.get());
      KlarFirebaseMessagingService_MembersInjector.injectFcmEventBus(instance, singletonCImpl.fcmEventBusProvider.get());
      return instance;
    }
  }

  private static final class SingletonCImpl extends KlarFinanceApp_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<SessionManager> sessionManagerProvider;

    private Provider<ChuckerInterceptor> provideChuckerInterceptorProvider;

    private Provider<OkHttpClient> provideOkHttpClientProvider;

    private Provider<Json> provideJsonProvider;

    private Provider<ApiService> apiServiceProvider;

    private Provider<LocationTrackingRepositoryImpl> locationTrackingRepositoryImplProvider;

    private Provider<LocationTrackingRepository> bindLocationTrackingRepositoryProvider;

    private Provider<LocationScheduler> locationSchedulerProvider;

    private Provider<LocationCaptureWorker_AssistedFactory> locationCaptureWorker_AssistedFactoryProvider;

    private Provider<SecureTokenStore> secureTokenStoreProvider;

    private Provider<AuthRepositoryImpl> authRepositoryImplProvider;

    private Provider<AuthRepository> bindAuthRepositoryProvider;

    private Provider<LoanRepositoryImpl> loanRepositoryImplProvider;

    private Provider<LoanRepository> bindLoanRepositoryProvider;

    private Provider<FcmEventBus> fcmEventBusProvider;

    private Provider<KlarFinanceDatabase> provideDatabaseProvider;

    private Provider<VerifiedPhoneRepositoryImpl> verifiedPhoneRepositoryImplProvider;

    private Provider<VerifiedPhoneRepository> bindVerifiedPhoneRepositoryProvider;

    private Provider<QrisRepositoryImpl> qrisRepositoryImplProvider;

    private Provider<QrisRepository> bindQrisRepositoryProvider;

    private Provider<ReferralRepositoryImpl> referralRepositoryImplProvider;

    private Provider<ReferralRepository> bindReferralRepositoryProvider;

    private Provider<LocationRepositoryImpl> locationRepositoryImplProvider;

    private Provider<LocationRepository> bindLocationRepositoryProvider;

    private Provider<TransjakartaRepositoryImpl> transjakartaRepositoryImplProvider;

    private Provider<TransjakartaRepository> bindTransjakartaRepositoryProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);
      initialize2(applicationContextModuleParam);

    }

    private AuthInterceptor authInterceptor() {
      return new AuthInterceptor(sessionManagerProvider.get());
    }

    private SubmitLocationPingUseCase submitLocationPingUseCase() {
      return new SubmitLocationPingUseCase(bindLocationTrackingRepositoryProvider.get());
    }

    private LogLocationFailureUseCase logLocationFailureUseCase() {
      return new LogLocationFailureUseCase(bindLocationTrackingRepositoryProvider.get());
    }

    private Map<String, javax.inject.Provider<WorkerAssistedFactory<? extends ListenableWorker>>> mapOfStringAndProviderOfWorkerAssistedFactoryOf(
        ) {
      return Collections.<String, javax.inject.Provider<WorkerAssistedFactory<? extends ListenableWorker>>>singletonMap("com.klarfinance.app.core.location.LocationCaptureWorker", ((Provider) locationCaptureWorker_AssistedFactoryProvider));
    }

    private HiltWorkerFactory hiltWorkerFactory() {
      return WorkerFactoryModule_ProvideFactoryFactory.provideFactory(mapOfStringAndProviderOfWorkerAssistedFactoryOf());
    }

    private VerifiedPhoneDao verifiedPhoneDao() {
      return DatabaseModule_ProvideVerifiedPhoneDaoFactory.provideVerifiedPhoneDao(provideDatabaseProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.sessionManagerProvider = DoubleCheck.provider(new SwitchingProvider<SessionManager>(singletonCImpl, 4));
      this.provideChuckerInterceptorProvider = DoubleCheck.provider(new SwitchingProvider<ChuckerInterceptor>(singletonCImpl, 5));
      this.provideOkHttpClientProvider = DoubleCheck.provider(new SwitchingProvider<OkHttpClient>(singletonCImpl, 3));
      this.provideJsonProvider = DoubleCheck.provider(new SwitchingProvider<Json>(singletonCImpl, 6));
      this.apiServiceProvider = DoubleCheck.provider(new SwitchingProvider<ApiService>(singletonCImpl, 2));
      this.locationTrackingRepositoryImplProvider = new SwitchingProvider<>(singletonCImpl, 1);
      this.bindLocationTrackingRepositoryProvider = DoubleCheck.provider((Provider) locationTrackingRepositoryImplProvider);
      this.locationSchedulerProvider = DoubleCheck.provider(new SwitchingProvider<LocationScheduler>(singletonCImpl, 7));
      this.locationCaptureWorker_AssistedFactoryProvider = SingleCheck.provider(new SwitchingProvider<LocationCaptureWorker_AssistedFactory>(singletonCImpl, 0));
      this.secureTokenStoreProvider = DoubleCheck.provider(new SwitchingProvider<SecureTokenStore>(singletonCImpl, 9));
      this.authRepositoryImplProvider = new SwitchingProvider<>(singletonCImpl, 8);
      this.bindAuthRepositoryProvider = DoubleCheck.provider((Provider) authRepositoryImplProvider);
      this.loanRepositoryImplProvider = new SwitchingProvider<>(singletonCImpl, 10);
      this.bindLoanRepositoryProvider = DoubleCheck.provider((Provider) loanRepositoryImplProvider);
      this.fcmEventBusProvider = DoubleCheck.provider(new SwitchingProvider<FcmEventBus>(singletonCImpl, 11));
      this.provideDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<KlarFinanceDatabase>(singletonCImpl, 13));
      this.verifiedPhoneRepositoryImplProvider = new SwitchingProvider<>(singletonCImpl, 12);
      this.bindVerifiedPhoneRepositoryProvider = DoubleCheck.provider((Provider) verifiedPhoneRepositoryImplProvider);
      this.qrisRepositoryImplProvider = new SwitchingProvider<>(singletonCImpl, 14);
      this.bindQrisRepositoryProvider = DoubleCheck.provider((Provider) qrisRepositoryImplProvider);
      this.referralRepositoryImplProvider = new SwitchingProvider<>(singletonCImpl, 15);
      this.bindReferralRepositoryProvider = DoubleCheck.provider((Provider) referralRepositoryImplProvider);
      this.locationRepositoryImplProvider = new SwitchingProvider<>(singletonCImpl, 16);
      this.bindLocationRepositoryProvider = DoubleCheck.provider((Provider) locationRepositoryImplProvider);
      this.transjakartaRepositoryImplProvider = new SwitchingProvider<>(singletonCImpl, 17);
    }

    @SuppressWarnings("unchecked")
    private void initialize2(final ApplicationContextModule applicationContextModuleParam) {
      this.bindTransjakartaRepositoryProvider = DoubleCheck.provider((Provider) transjakartaRepositoryImplProvider);
    }

    @Override
    public void injectKlarFinanceApp(KlarFinanceApp klarFinanceApp) {
      injectKlarFinanceApp2(klarFinanceApp);
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    @CanIgnoreReturnValue
    private KlarFinanceApp injectKlarFinanceApp2(KlarFinanceApp instance) {
      KlarFinanceApp_MembersInjector.injectWorkerFactory(instance, hiltWorkerFactory());
      return instance;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.klarfinance.app.core.location.LocationCaptureWorker_AssistedFactory 
          return (T) new LocationCaptureWorker_AssistedFactory() {
            @Override
            public LocationCaptureWorker create(Context context, WorkerParameters params) {
              return new LocationCaptureWorker(context, params, singletonCImpl.submitLocationPingUseCase(), singletonCImpl.logLocationFailureUseCase(), singletonCImpl.locationSchedulerProvider.get());
            }
          };

          case 1: // com.klarfinance.app.data.repository.LocationTrackingRepositoryImpl 
          return (T) new LocationTrackingRepositoryImpl(singletonCImpl.apiServiceProvider.get());

          case 2: // com.klarfinance.app.core.network.ApiService 
          return (T) new ApiService(singletonCImpl.provideOkHttpClientProvider.get(), singletonCImpl.provideJsonProvider.get());

          case 3: // okhttp3.OkHttpClient 
          return (T) NetworkModule_ProvideOkHttpClientFactory.provideOkHttpClient(new HmacInterceptor(), singletonCImpl.authInterceptor(), singletonCImpl.provideChuckerInterceptorProvider.get());

          case 4: // com.klarfinance.app.core.session.SessionManager 
          return (T) new SessionManager();

          case 5: // com.chuckerteam.chucker.api.ChuckerInterceptor 
          return (T) NetworkModule_ProvideChuckerInterceptorFactory.provideChuckerInterceptor(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 6: // kotlinx.serialization.json.Json 
          return (T) NetworkModule_ProvideJsonFactory.provideJson();

          case 7: // com.klarfinance.app.core.location.LocationScheduler 
          return (T) new LocationScheduler(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 8: // com.klarfinance.app.data.repository.AuthRepositoryImpl 
          return (T) new AuthRepositoryImpl(singletonCImpl.apiServiceProvider.get(), singletonCImpl.sessionManagerProvider.get(), singletonCImpl.secureTokenStoreProvider.get());

          case 9: // com.klarfinance.app.core.session.SecureTokenStore 
          return (T) new SecureTokenStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 10: // com.klarfinance.app.data.repository.LoanRepositoryImpl 
          return (T) new LoanRepositoryImpl(singletonCImpl.apiServiceProvider.get());

          case 11: // com.klarfinance.app.core.notification.FcmEventBus 
          return (T) new FcmEventBus();

          case 12: // com.klarfinance.app.data.repository.VerifiedPhoneRepositoryImpl 
          return (T) new VerifiedPhoneRepositoryImpl(singletonCImpl.verifiedPhoneDao());

          case 13: // com.klarfinance.app.data.local.KlarFinanceDatabase 
          return (T) DatabaseModule_ProvideDatabaseFactory.provideDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 14: // com.klarfinance.app.data.repository.QrisRepositoryImpl 
          return (T) new QrisRepositoryImpl(singletonCImpl.apiServiceProvider.get());

          case 15: // com.klarfinance.app.data.repository.ReferralRepositoryImpl 
          return (T) new ReferralRepositoryImpl(singletonCImpl.apiServiceProvider.get());

          case 16: // com.klarfinance.app.data.repository.LocationRepositoryImpl 
          return (T) new LocationRepositoryImpl(singletonCImpl.apiServiceProvider.get());

          case 17: // com.klarfinance.app.data.repository.TransjakartaRepositoryImpl 
          return (T) new TransjakartaRepositoryImpl(singletonCImpl.apiServiceProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
