package com.api.klarfinance.los.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class LimitApplicationDetailResponse {
    private Integer id;
    private String applicationCode;
    private String status;

    // Informasi Konsumen
    private String customerIdentity;
    private String customerName;
    private String customerAddress;
    private String customerAddress2;
    private String branchName;
    private String provinceName;
    private String regencyName;
    private String districtName;
    private String villageName;

    // Ada/enggaknya foto KTP/selfie (nasabah upload pas register) - false kalau nasabah belum
    // pernah upload (mis. data lama sebelum foto jadi wajib). Sengaja boolean, bukan URL string -
    // frontend construct URL fetch-nya sendiri dari applicationId (lihat
    // LosApprovalService#getPicture buat endpoint-nya) biar gak ambigu base URL mana yang dipakai
    // buat gabungin path relatif. Endpoint-nya butuh auth yang sama dengan detail ini (CHECKER/BM)
    // - bukan permitAll, karena isinya foto KTP+wajah asli.
    private Boolean hasFotoKtp;
    private Boolean hasFotoKyc;

    private BigDecimal incomeAmount;

    private VidaDetail vida;
    private PefindoDetail pefindo;

    // Analisa Aplikasi - lihat catatan di LosApprovalService#toDetail soal keterbatasan datanya.
    private Integer positiveAppsCount;
    private Integer pinjolAppsCount;
    private Integer judolAppsCount;
    private Integer bankingAppsCount;
    private List<String> pinjolApps;
    private List<String> bankApps;

    // Engine Scoring
    private Integer engineScore;
    private String engineRiskCategory;
    private String engineRecommendation;
    private List<String> engineKeyFactors;
    private BigDecimal engineSuggestionLimit;

    // Rentang plafond yang boleh diajukan Checker (±20% dari saran engine)
    private BigDecimal checkerLimitMin;
    private BigDecimal checkerLimitMax;

    // Rekomendasi Checker - null selama masih PENDING_CHECKER (belum diputusin)
    private String checkerRecommendation;
    private BigDecimal checkerPurposeLimit;
    private String checkerReason;

    // Keputusan final BM - null selama belum APPROVED/REJECTED oleh BM
    private BigDecimal finalApprovedLimit;
    private String bmReason;

    private LocalDateTime createdAt;

    // Status lock BM (bucket-per-branch) - lihat LosApprovalService#getDetail.
    private String lockedByIdentity;
    private String lockedByName;
    private Boolean lockedByMe;
    private Boolean canReview;

    @Data
    @Builder
    public static class VidaDetail {
        private String kycStatus;
        private String kycFaceScore;
        private String kycVendorTrxId;
        private String incomeVerificationStatus;
        private String incomeVerificationSource;
        private String incomeIsEmployed;
        private String incomeVerifiedMonthly;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    public static class PefindoDetail {
        private String score;
        private Integer colStatus;
        private String riskLabel;
        private String pdfPathFile;
        private LocalDateTime createdAt;
    }
}
