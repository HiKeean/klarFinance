package com.klarfinance.app.presentation.loan

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klarfinance.app.core.scan.InstalledAppsScanner
import com.klarfinance.app.domain.model.LoanRequestResult
import com.klarfinance.app.domain.usecase.GetLimitSummaryUseCase
import com.klarfinance.app.domain.usecase.RequestLoanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class RequestLoanViewModel @Inject constructor(
    private val getLimitSummaryUseCase: GetLimitSummaryUseCase,
    private val requestLoanUseCase: RequestLoanUseCase,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RequestLoanUiState())
    val uiState: StateFlow<RequestLoanUiState> = _uiState.asStateFlow()

    private val _submitted = MutableSharedFlow<LoanRequestResult>()
    val submitted: SharedFlow<LoanRequestResult> = _submitted.asSharedFlow()

    init {
        loadLimit()
    }

    private fun loadLimit() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLimit = true, loadErrorMessage = null) }
            getLimitSummaryUseCase()
                .onSuccess { summary -> _uiState.update { it.copy(isLoadingLimit = false, limitSummary = summary) } }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoadingLimit = false, loadErrorMessage = throwable.message ?: "Gagal memuat data limit")
                    }
                }
        }
    }

    fun onAmountChange(value: String) =
        _uiState.update { it.copy(amountInput = value.filter(Char::isDigit), submitErrorMessage = null) }

    /** Tap salah satu dari 3 kartu preset (dihitung dari plafond nasabah, lihat
     * RequestLoanUiState.amountPresets) - isi langsung field nominal, sama seperti user ngetik
     * manual. */
    fun onAmountPresetSelected(amount: Long) = _uiState.update { it.copy(amountInput = amount.toString()) }

    fun onTenorSelected(tenorMonths: Int) = _uiState.update { it.copy(tenorMonths = tenorMonths, submitErrorMessage = null) }

    fun onBankAccountNumberChange(value: String) =
        _uiState.update { it.copy(bankAccountNumber = value, submitErrorMessage = null) }

    fun onBankCodeSelected(bankCode: String) = _uiState.update { it.copy(bankCode = bankCode) }

    fun onSubmitClick() {
        val state = _uiState.value
        if (!state.isAmountStepValid || !state.isBankStepValid || state.isSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitErrorMessage = null) }

            // Kalau proyeksi utilisasi > 30%, backend WAJIB dapat data scan pinjol/bank
            // terbaru (konfirmasi user - "scan ulang", bukan pakai data lama) supaya BM
            // yang review nanti lihat kondisi terkini, bukan cuma yang dulu direkam pas
            // registrasi. Scan-nya cepat (baca PackageManager lokal, bukan network) jadi
            // aman dijalankan sinkron sebelum submit tanpa loading state terpisah.
            val (pinjolApps, bankApps) = if (state.willRequireReview) {
                withContext(Dispatchers.IO) { InstalledAppsScanner.scan(appContext) }
            } else {
                InstalledAppsScanner.ScanResult(emptyList(), emptyList())
            }

            requestLoanUseCase(
                amount = state.amountValue(),
                tenorMonths = state.tenorMonths,
                bankAccountNumber = state.bankAccountNumber,
                bankCode = state.bankCode,
                pinjolApps = pinjolApps,
                bankApps = bankApps,
            )
                .onSuccess { result ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    _submitted.emit(result)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isSubmitting = false, submitErrorMessage = throwable.message ?: "Gagal mengajukan pinjaman")
                    }
                }
        }
    }
}
