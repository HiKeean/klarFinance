package com.api.klarfinance.los.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.api.klarfinance.auth.model.User;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dbh_limit_applications", schema = "los")
public class LimitApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** App ID nasabah-facing (beda dari PK internal `id`) - format YYMMDD + 2 digit random, lihat ApplicationCodeGenerator. */
    @Column(name = "application_code", unique = true, length = 8)
    private String applicationCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bm_id")
    private User bm;

    /**
     * BM approval lock (bucket-per-branch, lihat bm-approval-lock.md di knowledge base) - nyala
     * otomatis begitu SEBUAH BM buka halaman detail aplikasi ini selagi masih PENDING_BM. TANPA
     * TTL (beda dari lock Checker yang di Redis + 2 jam) - tetap nempel ke BM ini sampai
     * beneran diputuskan (approve/reject), bukan cuma sampai "aktif direview".
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locked_bm_id")
    private User lockedBm;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pefindo_id")
    private PefindoInquiry pefindoInquiry;

    @Column(name = "income_amount", precision = 18, scale = 2)
    private BigDecimal incomeAmount;

    @Column(name = "engine_suggestion_limit", precision = 18, scale = 2)
    private BigDecimal engineSuggestionLimit;

    @Column(name = "checker_purpose_limit", precision = 18, scale = 2)
    private BigDecimal checkerPurposeLimit;

    @Column(name = "final_approved_limit", precision = 18, scale = 2)
    private BigDecimal finalApprovedLimit;

    private String status;

    @Column(name = "detected_pinjol_apps")
    private String detectedPinjolApps;

    @Column(name = "detected_bank_apps")
    private String detectedBankApps;

    /** APPROVE/REJECT - rekomendasi Checker. Keputusan FINAL selalu di BM (lihat LosApprovalService). */
    @Column(name = "checker_recommendation")
    private String checkerRecommendation;

    @Column(name = "checker_reason")
    private String checkerReason;

    @Column(name = "bm_reason")
    private String bmReason;

    /** Field naratif "Engine Scoring" (halaman detail Checker) - diisi sekali pas EngineScoringService jalan. */
    @Column(name = "engine_recommendation")
    private String engineRecommendation;

    @Column(name = "engine_risk_category")
    private String engineRiskCategory;

    @Column(name = "engine_key_factors")
    private String engineKeyFactors;

    @Column(name = "engine_score")
    private Integer engineScore;

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
