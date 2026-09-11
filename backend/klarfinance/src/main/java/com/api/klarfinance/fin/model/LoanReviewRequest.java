package com.api.klarfinance.fin.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.api.klarfinance.auth.model.User;
import com.api.klarfinance.los.model.ActiveLimit;

/**
 * Pengajuan pinjaman yang melewati 30% dari plafond ({@link com.api.klarfinance.fin.service.LoanInterestPolicy#exceedsReviewThreshold})
 * - ditahan di sini (bukan langsung jadi {@link Loan}) sampai BM memutuskan, lihat
 * LoanReviewService. Skip Checker sepenuhnya (konfirmasi user) - langsung ke antrean BM,
 * mirror pola lock+status milik LimitApplication tapi entity terpisah karena bentuk datanya
 * beda (ini pengajuan LOAN, bukan pengajuan LIMIT).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dbh_loan_review_request", schema = "fin")
public class LoanReviewRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_limit_id", nullable = false)
    private ActiveLimit activeLimit;

    @Column(name = "requested_amount", precision = 18, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "tenor_months")
    private Integer tenorMonths;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "bank_code")
    private String bankCode;

    /** Snapshot ActiveLimit pas request ini dibuat - dipakai buat tampilan BM, bukan sumber
     * kebenaran limit terkini (itu tetap di ActiveLimit langsung, di-re-check ulang di bmDecide). */
    @Column(name = "used_limit_snapshot", precision = 18, scale = 2)
    private BigDecimal usedLimitSnapshot;

    @Column(name = "total_limit_snapshot", precision = 18, scale = 2)
    private BigDecimal totalLimitSnapshot;

    @Column(name = "utilization_percent", precision = 5, scale = 2)
    private BigDecimal utilizationPercent;

    @Column(name = "detected_pinjol_apps")
    private String detectedPinjolApps;

    @Column(name = "detected_bank_apps")
    private String detectedBankApps;

    /** Hasil refresh Pefindo (MockPefindoService) pas request ini dibuat - KOL/score terbaru,
     * terpisah dari PefindoInquiry yang nempel ke LimitApplication awal. */
    @Column(name = "pefindo_score")
    private String pefindoScore;

    @Column(name = "pefindo_col_status")
    private Integer pefindoColStatus;

    /** PENDING_BM/APPROVED/REJECTED - reuse konstanta com.api.authssohmac.los.EngineStatus. */
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locked_bm_id")
    private User lockedBm;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bm_id")
    private User bm;

    @Column(name = "bm_reason")
    private String bmReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
