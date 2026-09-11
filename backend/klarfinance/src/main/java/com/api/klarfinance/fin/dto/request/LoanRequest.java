package com.api.klarfinance.fin.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class LoanRequest {
    private BigDecimal amount;
    /** 1, 3, 6, 9, atau 12 bulan. */
    private Integer tenorMonths;
    private String bankAccountNumber;
    private String bankCode;

    /** Hasil scan device app terbaru (InstalledAppsScanner Kotlin) - cuma dibutuhkan/dipakai kalau
     * pengajuan ini melewati 30% dari plafond (lihat LoanInterestPolicy.exceedsReviewThreshold).
     * Optional - kalau kosong tetap diproses (dicatat sebagai belum discan), bukan hard error,
     * biar client lama/bug gak bikin nasabah stuck gak bisa mengajukan sama sekali. */
    private List<String> pinjolApps;
    private List<String> bankApps;
}
