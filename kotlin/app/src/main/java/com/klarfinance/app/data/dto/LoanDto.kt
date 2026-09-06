package com.klarfinance.app.data.dto

import kotlinx.serialization.Serializable

/** Mirrors backend LimitSummaryResponse (fin/dto/response) - GET api/v1/nasabah/loan/limit. */
@Serializable
data class LimitSummaryResponseDto(
    val totalLimit: Double? = null,
    val usedLimit: Double? = null,
    val availableLimit: Double? = null,
    val qrisQuota: Double? = null,
    val qrisUsedAmount: Double? = null,
)

/** Mirrors backend LoanRequest (fin/dto/request) - POST api/v1/nasabah/loan. pinjolApps/bankApps
 * cuma genuinely dipakai backend kalau pengajuan ini melewati 30% dari plafond (lihat
 * LoanInterestPolicy.exceedsReviewThreshold) - tetap dikirim selalu, backend yang menentukan
 * relevan atau tidak. */
@Serializable
data class LoanRequestDto(
    val amount: Double,
    val tenorMonths: Int,
    val bankAccountNumber: String,
    val bankCode: String,
    val pinjolApps: List<String> = emptyList(),
    val bankApps: List<String> = emptyList(),
)

/** Mirrors backend LoanResponse (fin/dto/response). Semua field selain reviewRequired/message
 * null kalau reviewRequired=true (pengajuan ditahan sebagai LoanReviewRequest, belum jadi Loan). */
@Serializable
data class LoanResponseDto(
    val loanId: Int? = null,
    val requestedAmount: Double? = null,
    val disbursedAmount: Double? = null,
    val adminFee: Double? = null,
    val totalAmountDue: Double? = null,
    val tenorMonths: Int? = null,
    val installmentAmount: Double? = null,
    val firstDueDate: String? = null,
    val referralDiscountApplied: Double? = null,
    val reviewRequired: Boolean = false,
    val reviewRequestId: Int? = null,
    val message: String? = null,
)
