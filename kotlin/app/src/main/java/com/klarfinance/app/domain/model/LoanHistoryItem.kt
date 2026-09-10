package com.klarfinance.app.domain.model

/** One row of the "History" page - covers BOTH tarik tunai (LOAN) and bayar QRIS (QRIS_PAYMENT),
 * since backend stores both as the same Loan entity (see LoanService#getMyLoanHistory). */
enum class LoanHistoryType {
    LOAN,
    QRIS_PAYMENT,
    ;

    companion object {
        fun fromBackend(value: String?): LoanHistoryType =
            entries.find { it.name == value } ?: LOAN
    }
}

enum class LoanHistoryStatus {
    ACTIVE,
    OVERDUE,
    PAID_OFF,
    ;

    companion object {
        fun fromBackend(value: String?): LoanHistoryStatus =
            entries.find { it.name == value } ?: ACTIVE
    }
}

data class LoanHistoryItem(
    val loanId: Int,
    val type: LoanHistoryType,
    /** Terisi cuma kalau [type]=QRIS_PAYMENT. */
    val merchantName: String?,
    val requestedAmount: Long,
    val totalAmountDue: Long,
    val tenorMonths: Int,
    val status: LoanHistoryStatus,
    val paidInstallments: Int,
    val totalInstallments: Int,
    /** Null kalau [status]=PAID_OFF. */
    val nextDueDate: String?,
    val nextDueAmount: Long?,
    val createdAt: String?,
    /** Jadwal LENGKAP semua cicilan (bukan cuma yang berikutnya), urut installmentNumber - buat
     * dialog detail pas item History di-tap. */
    val installments: List<LoanInstallment>,
)

data class LoanInstallment(
    val installmentNumber: Int,
    val dueDate: String?,
    val amount: Long,
    /** Backend cuma punya dua nilai per cicilan (UNPAID/PAID, lihat InstallmentStatus) - beda
     * dari [LoanHistoryStatus] di level Loan yang juga punya OVERDUE, jadi disimpan sebagai
     * boolean sederhana di sini, bukan reuse enum yang sama. */
    val isPaid: Boolean,
    val paidAt: String?,
)
