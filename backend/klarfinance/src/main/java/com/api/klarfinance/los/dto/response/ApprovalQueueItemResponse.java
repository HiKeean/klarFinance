
package com.api.klarfinance.los.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Item antrean approval BM/Checker - unified supaya BM bisa lihat SATU list gabungan pengajuan
 * limit baru (LimitApplication) dan pengajuan pinjaman >30% yang butuh review ulang
 * (LoanReviewRequest, lihat fin.LoanReviewService) - dibedakan lewat {@link #type}.
 * Field khusus salah satu tipe null kalau tidak relevan untuk tipe lainnya.
 */
@Data
@Builder
public class ApprovalQueueItemResponse {
    /** "LIMIT_APPLICATION" atau "LOAN_REVIEW". */
    private String type;

    private Integer id;
    private String applicationCode;
    private String customerIdentity;
    private String customerName;
    private String status;
    private LocalDateTime createdAt;

    // --- LIMIT_APPLICATION only ---
    private BigDecimal incomeAmount;
    private BigDecimal engineSuggestionLimit;
    private BigDecimal checkerPurposeLimit;
    private BigDecimal finalApprovedLimit;

    // --- LOAN_REVIEW only ---
    private BigDecimal requestedAmount;
    private Integer tenorMonths;
    private BigDecimal usedLimitSnapshot;
    private BigDecimal totalLimitSnapshot;
    private BigDecimal utilizationPercent;

    // --- shared (Pefindo/KOL - LIMIT_APPLICATION dari PefindoInquiry awal, LOAN_REVIEW dari refresh) ---
    private String pefindoScore;
    private Integer pefindoColStatus;

    // Status lock BM (bucket-per-branch) - null kalau belum ada yang buka detail-nya sama sekali.
    private String lockedByIdentity;
    private String lockedByName;
    private Boolean lockedByMe;
}
