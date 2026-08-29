package com.klarfinance.app.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klarfinance.app.domain.usecase.RequestOtpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val requestOtpUseCase: RequestOtpUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _otpRequested = MutableSharedFlow<String>()
    val otpRequested: SharedFlow<String> = _otpRequested.asSharedFlow()

    fun onCountryCodeChange(code: String) {
        _uiState.update { it.copy(countryCode = code) }
    }

    fun onPhoneNumberChange(number: String) {
        _uiState.update { it.copy(phoneNumber = number.filter(Char::isDigit), errorMessage = null) }
    }

    fun onContinueClick() {
        val state = _uiState.value
        if (!state.isContinueEnabled) return
        val fullPhone = "${state.countryCode.removePrefix("+")}${state.phoneNumber.trimStart('0')}"

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            requestOtpUseCase(fullPhone)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    _otpRequested.emit(fullPhone)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = throwable.message ?: "Failed to send OTP")
                    }
                }
        }
    }
}
