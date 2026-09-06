package com.klarfinance.app.presentation.loan

import com.klarfinance.app.domain.model.LimitSummary
import kotlin.math.roundToLong

/** Bank tujuan pencairan - belum ada master data bank di backend (lihat LoanRequest.bankCode di
 * backend, cuma String bebas), jadi daftar statis sederhana dulu, cukup buat MVP. */
data class BankOption(val code: String, val label: String)

val BANK_OPTIONS = listOf(
    BankOption("BCA", "BCA"),
    BankOption("BRI", "BRI"),
    BankOption("MANDIRI", "Mandiri"),
    BankOption("BNI", "BNI"),
    BankOption("PERMATA", "Permata"),
)

/** Cuma 1/3/6/9/12 - PERSIS tabel bunga resmi (LoanInterestPolicy di backend, konfirmasi user).
 * Referensi desain sempat menunjukkan opsi "2 bulan" juga, sengaja TIDAK diikutkan karena backend
 * tidak punya rate resmi untuk tenor itu - submit dengan tenor di luar daftar ini selalu ditolak
 * backend (IllegalArgumentException "Unsupported tenor"). */
val TENOR_OPTIONS = listOf(1, 3, 6, 9, 12)

/** Tenor yang ditandai "POPULER" di UI (murni hint visual, bukan aturan bisnis apapun). */
const val POPULAR_TENOR = 12

private const val REVIEW_THRESHOLD_PERCENT = 30.0

/** Mirror LoanInterestPolicy.MONTHLY_RATE_PERCENT_BY_TENOR (backend) - bunga flat per bulan
 * berdasarkan tenor, dikali jumlah bulan buat total bunga selama tenor (tidak majemuk). */
private val MONTHLY_RATE_PERCENT_BY_TENOR = mapOf(
    1 to 3.5,
    3 to 3.3,
    6 to 3.1,
    9 to 3.0,
    12 to 3.0,
)

/** Biaya admin (konfirmasi user 2026-09-06) - 1% dari nominal pinjaman, dipotong dari dana yang
 * cair ke nasabah, BUKAN ditambahkan ke tagihan (lihat LoanService.finalizeLoan di backend). */
private const val ADMIN_FEE_PERCENT = 1.0

data class RequestLoanUiState(
    val limitSummary: LimitSummary? = null,
    val isLoadingLimit: Boolean = true,
    val loadErrorMessage: String? = null,

    val amountInput: String = "",
    val tenorMonths: Int = TENOR_OPTIONS.first(),
    val bankAccountNumber: String = "",
    val bankCode: String = BANK_OPTIONS.first().code,

    val isSubmitting: Boolean = false,
    val submitErrorMessage: String? = null,
) {
    private val amount: Long get() = amountInput.filter(Char::isDigit).toLongOrNull() ?: 0L

    /** Langkah 1 (nominal + tenor) - tenor selalu punya default jadi cukup cek nominal. */
    val isAmountStepValid: Boolean
        get() = amount > 0 && limitSummary?.let { amount <= it.availableLimit } != false

    /** Langkah 2 (rekening tujuan) - dipanggil pas mau submit beneran. */
    val isBankStepValid: Boolean
        get() = bankAccountNumber.isNotBlank()

    /** 3 pilihan cepat nominal, DIHITUNG dari plafond TERSEDIA nasabah masing-masing (bukan
     * angka universal seperti di referensi desain) - 25%/50%/100% dari availableLimit,
     * dibulatkan ke bawah ke kelipatan Rp50.000 biar angkanya rapi. Kosong selama limit belum
     * kemuat/nasabah belum py plafond aktif. */
    val amountPresets: List<Long>
        get() {
            val available = limitSummary?.availableLimit ?: return emptyList()
            if (available <= 0) return emptyList()
            return listOf(0.25, 0.5, 1.0)
                .map { fraction -> roundDownToNearest((available * fraction).toLong(), 50_000L) }
                .filter { it > 0 }
                .distinct()
        }

    /** Preset yang ditandai "REC" - preset TERBESAR yang tetap menjaga utilisasi plafond di
     * ambang aman (<=30%, lihat LoanInterestPolicy.exceedsReviewThreshold di backend), biar
     * rekomendasi defaultnya gak "kejebak" masuk antrean review BM tambahan. Kalau semua preset
     * (termasuk yang terkecil) tetap di atas 30%, preset terkecil yang direkomendasikan - tidak
     * ada opsi yang genuinely aman buat nasabah ini. */
    val recommendedAmount: Long?
        get() {
            val summary = limitSummary ?: return null
            val presets = amountPresets
            if (presets.isEmpty() || summary.totalLimit <= 0) return presets.maxOrNull()
            val safePresets = presets.filter { preset ->
                (summary.usedLimit + preset) * 100.0 / summary.totalLimit <= REVIEW_THRESHOLD_PERCENT
            }
            return safePresets.maxOrNull() ?: presets.minOrNull()
        }

    /** Utilisasi plafond SETELAH pengajuan ini (kalau disetujui) - dipakai buat preview di UI
     * dan buat nentuin perlu-tidaknya scan ulang pinjol sebelum submit (mirror backend
     * LoanInterestPolicy.exceedsReviewThreshold, konfirmasi user 30%). */
    val projectedUtilizationPercent: Double?
        get() {
            val total = limitSummary?.totalLimit ?: return null
            if (total <= 0) return null
            return (limitSummary.usedLimit + amount) * 100.0 / total
        }

    val willRequireReview: Boolean
        get() = (projectedUtilizationPercent ?: 0.0) > REVIEW_THRESHOLD_PERCENT

    /** Biaya admin 1% dari nominal pinjaman - dipotong dari dana yang cair, lihat
     * [ADMIN_FEE_PERCENT]. */
    val adminFee: Long
        get() = (amount * ADMIN_FEE_PERCENT / 100.0).roundToLong()

    /** Nominal yang benar-benar cair ke rekening nasabah setelah dipotong biaya admin. */
    val netAmountReceived: Long
        get() = amount - adminFee

    /** Total bunga flat selama tenor yang dipilih - mirror LoanService.createLoan di backend
     * (amount * rate% * tenorMonths), dihitung dari nominal pinjaman PENUH, bukan nominal yang
     * diterima. */
    val totalInterest: Long
        get() {
            val monthlyRate = MONTHLY_RATE_PERCENT_BY_TENOR[tenorMonths] ?: return 0L
            return (amount * monthlyRate / 100.0 * tenorMonths).roundToLong()
        }

    fun amountValue(): Long = amount
}

private fun roundDownToNearest(value: Long, step: Long): Long = (value / step) * step
