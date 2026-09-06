package com.klarfinance.app.presentation.splash

import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klarfinance.app.core.security.BiometricAuthHelper
import com.klarfinance.app.core.session.SecureTokenStore
import com.klarfinance.app.domain.model.AccountState
import com.klarfinance.app.domain.usecase.RefreshSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SPLASH_DELAY_MS = 1500L
private const val TAG = "SplashViewModel"

/**
 * Fingerprint-gated session restore: if "Sidik Jari" was enabled on Account (a refresh token
 * is stored in [SecureTokenStore]), prompt biometric and redeem it for a fresh session instead
 * of always landing on [AccountState.GUEST]. Any failure along the way (no token, biometric
 * unavailable/cancelled, refresh rejected by the backend) falls back to GUEST - this is a
 * bonus fast-path, not a requirement to open the app.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val secureTokenStore: SecureTokenStore,
    private val refreshSessionUseCase: RefreshSessionUseCase,
) : ViewModel() {

    private val _resolved = MutableSharedFlow<AccountState>()
    val resolved: SharedFlow<AccountState> = _resolved.asSharedFlow()

    fun resolveSession(activity: FragmentActivity) {
        viewModelScope.launch {
            delay(SPLASH_DELAY_MS)

            val storedRefreshToken = secureTokenStore.getRefreshToken()
            if (storedRefreshToken == null) {
                Log.w(TAG, "No stored refresh token - falling back to GUEST")
                _resolved.emit(AccountState.GUEST)
                return@launch
            }
            if (!BiometricAuthHelper.isAvailable(activity)) {
                Log.w(TAG, "Biometric hardware unavailable/not enrolled - falling back to GUEST")
                _resolved.emit(AccountState.GUEST)
                return@launch
            }

            val biometricResult = BiometricAuthHelper.authenticate(
                activity = activity,
                title = "Masuk ke KlarFinance",
                subtitle = "Gunakan sidik jari untuk melanjutkan sesi kamu",
            )
            if (biometricResult.isFailure) {
                Log.w(TAG, "Biometric prompt failed/cancelled", biometricResult.exceptionOrNull())
                _resolved.emit(AccountState.GUEST)
                return@launch
            }

            refreshSessionUseCase(storedRefreshToken)
                .onSuccess { accountState -> _resolved.emit(accountState) }
                .onFailure { throwable ->
                    // Stale/revoked token (e.g. logged out elsewhere, or already redeemed) -
                    // stop trying so this doesn't prompt biometric forever on a dead token.
                    Log.e(TAG, "Refresh token redemption failed - clearing stored token", throwable)
                    secureTokenStore.clear()
                    _resolved.emit(AccountState.GUEST)
                }
        }
    }
}
