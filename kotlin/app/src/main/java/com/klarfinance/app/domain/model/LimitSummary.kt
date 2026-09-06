package com.klarfinance.app.domain.model

/** The nasabah's current plafond breakdown, backing Home's "Available Loan" card once
 * AccountState is ACTIVE - see LoanRepository.getLimitSummary. Amounts are whole Rupiah (no
 * decimals in practice - see backend LoanInterestPolicy), kept as Long to match how the card's
 * formatRupiah()/progress-bar math already worked with the placeholder numbers it replaces. */
data class LimitSummary(
    val totalLimit: Long,
    val usedLimit: Long,
    val availableLimit: Long,
    /** Kuota QRIS kumulatif (QrisPolicy.quota di backend) dan yang sudah kepakai - null kalau
     * nasabah belum eligible (plafond < Rp2jt). */
    val qrisQuota: Long? = null,
    val qrisUsedAmount: Long? = null,
) {
    val isQrisEligible: Boolean get() = qrisQuota != null
}
