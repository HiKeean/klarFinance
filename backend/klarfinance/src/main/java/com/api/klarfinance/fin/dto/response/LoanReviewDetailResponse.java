package com.api.klarfinance.fin.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class LoanReviewDetailResponse {
    private Integer id;
    private String status;

    private String customerIdentity;
    private String customerName;
    private String customerAddress;
    private String provinceName;
    private String regencyName;

    private BigDecimal requestedAmount;
    private Integer tenorMonths;
    private String bankAccountNumber;
    private String bankCode;

    private BigDecimal usedLimitSnapshot;
    private BigDecimal totalLimitSnapshot;
    private BigDecimal utilizationPercent;

    /** usedLimitSnapshot + requestedAmount - "kalau pengajuan ini disetujui, total pokok utang
     * nasabah jadi berapa" (konfirmasi user 2026-09-07, biar BM lihat angka absolut, bukan cuma
     * persentase utilisasi). */
    private BigDecimal projectedTotalDebt;

    private String pefindoScore;
    private Integer pefindoColStatus;
    private String pefindoRiskLabel;

    private Integer pinjolAppsCount;
    private Integer bankingAppsCount;
    /** Nama aplikasi pinjol/bank yang terdeteksi (bukan cuma jumlahnya) - konfirmasi user
     * 2026-09-07 supaya BM lihat "ada pinjol/bank apa aja", bukan cuma angka count. Kosong kalau
     * tidak ada yang terdeteksi. */
    private List<String> pinjolApps;
    private List<String> bankApps;

    private String bmReason;
    private LocalDateTime createdAt;

    private String lockedByIdentity;
    private String lockedByName;
    private Boolean lockedByMe;
    private Boolean canReview;
}
