package com.klarfinance.app.presentation.qris.amount

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klarfinance.app.core.navigation.Screen
import com.klarfinance.app.domain.model.QrisConfirmResult
import com.klarfinance.app.domain.usecase.ConfirmQrisUseCase
import com.klarfinance.app.domain.usecase.GetLimitSummaryUseCase
import com.klarfinance.app.domain.usecase.ScanQrisUseCase
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

/**
 * Scoped per QrisAmountScreen (bukan graph-shared seperti RequestLoanViewModel) - cuma satu
 * data yang perlu "dibawa" dari langkah scan (merchantCode, dari QR mentah) dan itu cukup lewat
 * nav arg biasa (SavedStateHandle), gak perlu ViewModel dibagi lintas 2 layar.
 */
@HiltViewModel
class QrisViewModel @Inject constructor(
    private val scanQrisUseCase: ScanQrisUseCase,
    private val confirmQrisUseCase: ConfirmQrisUseCase,
    private val getLimitSummaryUseCase: GetLimitSummaryUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val merchantCode: String = checkNotNull(savedStateHandle[Screen.QrisAmount.ARG_MERCHANT_CODE])

    private val _uiState = MutableStateFlow(QrisUiState())
    val uiState: StateFlow<QrisUiState> = _uiState.asStateFlow()

    private val _submitted = MutableSharedFlow<QrisConfirmResult>()
    val submitted: SharedFlow<QrisConfirmResult> = _submitted.asSharedFlow()

    init {
        scan()
        loadLimit()
    }

    private fun scan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanErrorMessage = null) }
            scanQrisUseCase(merchantCode)
                .onSuccess { info ->
                    _uiState.update { it.copy(isScanning = false, token = info.token, merchantName = info.merchantName) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isScanning = false, scanErrorMessage = throwable.message ?: "QR tidak valid")
                    }
                }
        }
    }

    private fun loadLimit() {
        viewModelScope.launch {
            getLimitSummaryUseCase().onSuccess { summary -> _uiState.update { it.copy(limitSummary = summary) } }
            // Kegagalan load limit gak diperlakukan fatal di sini - remainingQrisQuota cuma jadi
            // null (preview kuota gak tampil), backend tetap validasi ulang beneran pas confirm.
        }
    }

    fun onAmountChange(value: String) =
        _uiState.update { it.copy(amountInput = value.filter(Char::isDigit), submitErrorMessage = null) }

    fun onSubmitClick() {
        val state = _uiState.value
        val token = state.token ?: return
        if (!state.isFormValid || state.isSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitErrorMessage = null) }
            confirmQrisUseCase(token, state.amountValue())
                .onSuccess { result ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    _submitted.emit(result)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isSubmitting = false, submitErrorMessage = throwable.message ?: "Gagal memproses pembayaran")
                    }
                }
        }
    }
}
