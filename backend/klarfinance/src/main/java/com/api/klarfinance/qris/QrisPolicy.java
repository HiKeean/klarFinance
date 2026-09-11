package com.api.klarfinance.qris;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Aturan kuota QRIS (konfirmasi user) - BEDA TOTAL dari aturan ">30% pengajuan pinjaman tunai"
 * (LoanInterestPolicy.exceedsReviewThreshold) - itu tetap berlaku apa adanya buat pinjaman
 * tunai, TIDAK dipakai untuk QRIS. QRIS motong plafond utama yang SAMA (ActiveLimit), cuma
 * dibatasi kuota terpisah yang lebih ketat di dalamnya.
 */
public final class QrisPolicy {
    private QrisPolicy() {}

    /** Plafond total nasabah harus >= ini buat bisa pakai QRIS sama sekali - di bawah ini fitur
     * QRIS terkunci total, bukan sekadar dapat kuota kecil. */
    public static final BigDecimal MIN_ELIGIBLE_PLAFOND = BigDecimal.valueOf(2_000_000);

    /** Kuota QRIS selalu dipotong ke nominal ini walau 30% dari plafond nasabah jauh lebih besar.
     * Scale 2 (bukan valueOf polos) biar konsisten sama hasil quota() yang selalu 2 desimal,
     * terlepas dari cabang mana yang menang di .min(). */
    public static final BigDecimal MAX_QRIS_QUOTA = new BigDecimal("1000000.00");

    private static final BigDecimal QRIS_QUOTA_PERCENT = BigDecimal.valueOf(30);

    public static boolean isEligible(BigDecimal totalLimit) {
        return totalLimit != null && totalLimit.compareTo(MIN_ELIGIBLE_PLAFOND) >= 0;
    }

    /** Kuota KUMULATIF (bukan per-transaksi) = min(30% x plafond total, Rp1.000.000). */
    public static BigDecimal quota(BigDecimal totalLimit) {
        if (totalLimit == null || totalLimit.signum() <= 0) return BigDecimal.ZERO;
        BigDecimal percentOfTotal = totalLimit
                .multiply(QRIS_QUOTA_PERCENT)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return percentOfTotal.min(MAX_QRIS_QUOTA);
    }
}
