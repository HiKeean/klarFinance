package com.klarfinance.app.domain.repository

import com.klarfinance.app.domain.model.LimitSummary
import com.klarfinance.app.domain.model.LoanRequestResult

interface LoanRepository {
    /** GET api/v1/nasabah/loan/limit - fails (Result.failure) if the caller has no active
     * limit yet, same as the backend endpoint (see LoanService#getMyLimitSummary). */
    suspend fun getLimitSummary(): Result<LimitSummary>

    /** POST api/v1/nasabah/loan - lihat LoanRequestResult.reviewRequired, pengajuan bisa langsung
     * cair ATAU ditahan buat review BM tergantung utilisasi plafond setelahnya. */
    suspend fun requestLoan(
        amount: Long,
        tenorMonths: Int,
        bankAccountNumber: String,
        bankCode: String,
        pinjolApps: List<String> = emptyList(),
        bankApps: List<String> = emptyList(),
    ): Result<LoanRequestResult>
}
