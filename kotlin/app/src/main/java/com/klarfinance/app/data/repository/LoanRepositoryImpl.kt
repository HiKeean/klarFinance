package com.klarfinance.app.data.repository

import com.klarfinance.app.core.network.ApiService
import com.klarfinance.app.data.dto.LimitSummaryResponseDto
import com.klarfinance.app.data.dto.LoanRequestDto
import com.klarfinance.app.data.dto.LoanResponseDto
import com.klarfinance.app.domain.model.LimitSummary
import com.klarfinance.app.domain.model.LoanRequestResult
import com.klarfinance.app.domain.repository.LoanRepository
import javax.inject.Inject

class LoanRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
) : LoanRepository {

    override suspend fun getLimitSummary(): Result<LimitSummary> = runCatching {
        val response = apiService.get<LimitSummaryResponseDto>("api/v1/nasabah/loan/limit")
        val data = response.data ?: throw IllegalStateException("Unexpected response from server")
        LimitSummary(
            totalLimit = (data.totalLimit ?: 0.0).toLong(),
            usedLimit = (data.usedLimit ?: 0.0).toLong(),
            availableLimit = (data.availableLimit ?: 0.0).toLong(),
            qrisQuota = data.qrisQuota?.toLong(),
            qrisUsedAmount = data.qrisUsedAmount?.toLong(),
        )
    }

    override suspend fun requestLoan(
        amount: Long,
        tenorMonths: Int,
        bankAccountNumber: String,
        bankCode: String,
        pinjolApps: List<String>,
        bankApps: List<String>,
    ): Result<LoanRequestResult> = runCatching {
        val body = LoanRequestDto(
            amount = amount.toDouble(),
            tenorMonths = tenorMonths,
            bankAccountNumber = bankAccountNumber,
            bankCode = bankCode,
            pinjolApps = pinjolApps,
            bankApps = bankApps,
        )
        val response = apiService.post<LoanResponseDto, LoanRequestDto>("api/v1/nasabah/loan", body)
        val data = response.data ?: throw IllegalStateException("Unexpected response from server")
        LoanRequestResult(
            loanId = data.loanId,
            disbursedAmount = data.disbursedAmount?.toLong(),
            adminFee = data.adminFee?.toLong(),
            totalAmountDue = data.totalAmountDue?.toLong(),
            installmentAmount = data.installmentAmount?.toLong(),
            tenorMonths = data.tenorMonths,
            reviewRequired = data.reviewRequired,
            reviewRequestId = data.reviewRequestId,
            message = data.message,
        )
    }
}
