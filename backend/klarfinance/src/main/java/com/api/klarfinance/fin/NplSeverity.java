package com.api.klarfinance.fin;

import java.math.BigDecimal;

/**
 * Threshold NPL per branch (konfirmasi user 2026-09-01): dibawah 2% hijau, 2-5% kuning, diatas 5%
 * merah. nplPercent = (jumlah pinjaman Overdue / jumlah pinjaman aktif di branch itu) x 100.
 */
public final class NplSeverity {
    private NplSeverity() {}

    public static final String GREEN = "GREEN";
    public static final String YELLOW = "YELLOW";
    public static final String RED = "RED";

    private static final BigDecimal GREEN_MAX_EXCLUSIVE = BigDecimal.valueOf(2);
    private static final BigDecimal YELLOW_MAX_INCLUSIVE = BigDecimal.valueOf(5);

    public static String classify(BigDecimal nplPercent) {
        if (nplPercent.compareTo(GREEN_MAX_EXCLUSIVE) < 0) return GREEN;
        if (nplPercent.compareTo(YELLOW_MAX_INCLUSIVE) <= 0) return YELLOW;
        return RED;
    }
}
