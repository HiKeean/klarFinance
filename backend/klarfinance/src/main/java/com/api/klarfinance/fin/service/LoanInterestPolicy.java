package com.api.klarfinance.fin.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Placeholder business parameter (dikonfirmasi user 2026-08-31) — belum ada di PRD/BRD/project_rules.md.
 * Ganti kalau Product sudah menetapkan rate resmi.
 * Bunga flat per bulan berdasarkan tenor, tidak majemuk (bukan reducing balance).
 */
public final class LoanInterestPolicy {
    private LoanInterestPolicy() {}

    private static final Map<Integer, BigDecimal> MONTHLY_RATE_PERCENT_BY_TENOR = Map.of(
            1, BigDecimal.valueOf(3.5),
            3, BigDecimal.valueOf(3.3),
            6, BigDecimal.valueOf(3.1),
            9, BigDecimal.valueOf(3.0),
            12, BigDecimal.valueOf(3.0)
    );

    /** Denda keterlambatan (konfirmasi user 2026-09-01): 2%/hari, flat dari nominal cicilan yang
     * overdue (bukan compounding, bukan dari total sisa pinjaman). Gak ada grace period lagi -
     * mulai kena begitu due date lewat (day 1 overdue). */
    private static final BigDecimal DAILY_LATE_PENALTY_RATE_PERCENT = BigDecimal.valueOf(2);

    /** Biaya admin (konfirmasi user 2026-09-06): 1% dari nominal pinjaman, dipotong dari dana
     * yang cair ke nasabah (drawdownAmount) - BUKAN ditambahkan ke totalAmountDue. Nasabah tetap
     * membayar bunga dari nominal pinjaman PENUH, cuma menerima lebih sedikit di muka. Hanya
     * berlaku untuk pencairan bank transfer (LoanService.finalizeLoan) - QRIS/merchant
     * (finalizeLoanForMerchant) tidak dipotong karena dana cair ke merchant, bukan ke nasabah. */
    private static final BigDecimal ADMIN_FEE_PERCENT = BigDecimal.valueOf(1);

    /** Konfirmasi user: kalau (pinjaman berjalan + pinjaman baru) sudah melewati 30% dari plafond,
     * pengajuan wajib direview ulang BM (skip Checker) - bukan langsung cair, lihat LoanService.
     * (Sempat direvisi jadi 50% pada 2026-09-07 lalu dikembalikan ke 30% pada hari yang sama -
     * konfirmasi user, bukan revert yang keliru.) */
    public static final BigDecimal REVIEW_THRESHOLD_PERCENT = BigDecimal.valueOf(30);

    public static boolean isValidTenor(Integer tenorMonths) {
        return tenorMonths != null && MONTHLY_RATE_PERCENT_BY_TENOR.containsKey(tenorMonths);
    }

    public static BigDecimal monthlyRatePercent(Integer tenorMonths) {
        BigDecimal rate = MONTHLY_RATE_PERCENT_BY_TENOR.get(tenorMonths);
        if (rate == null) throw new IllegalArgumentException("Unsupported tenor: " + tenorMonths + " months");
        return rate;
    }

    public static BigDecimal calculateAdminFee(BigDecimal amount) {
        if (amount == null) return BigDecimal.ZERO;
        return amount.multiply(ADMIN_FEE_PERCENT).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateLatePenalty(BigDecimal overdueInstallmentAmount, long daysOverdue) {
        if (overdueInstallmentAmount == null || daysOverdue <= 0) return BigDecimal.ZERO;
        return overdueInstallmentAmount
                .multiply(DAILY_LATE_PENALTY_RATE_PERCENT)
                .multiply(BigDecimal.valueOf(daysOverdue))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /** (usedLimit + amount baru) / totalLimit x 100, dibulatkan 2 desimal. */
    public static BigDecimal utilizationPercentAfter(BigDecimal usedLimit, BigDecimal amount, BigDecimal totalLimit) {
        if (totalLimit == null || totalLimit.signum() <= 0) return BigDecimal.ZERO;
        BigDecimal used = usedLimit == null ? BigDecimal.ZERO : usedLimit;
        BigDecimal requested = amount == null ? BigDecimal.ZERO : amount;
        return used.add(requested)
                .multiply(BigDecimal.valueOf(100))
                .divide(totalLimit, 2, RoundingMode.HALF_UP);
    }

    public static boolean exceedsReviewThreshold(BigDecimal usedLimit, BigDecimal amount, BigDecimal totalLimit) {
        return utilizationPercentAfter(usedLimit, amount, totalLimit).compareTo(REVIEW_THRESHOLD_PERCENT) > 0;
    }
}
