package com.api.klarfinance.los.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.api.klarfinance.auth.model.CustomerDetails;
import com.api.klarfinance.auth.model.DetailUserInternal;
import com.api.klarfinance.auth.model.User;
import com.api.klarfinance.auth.repository.CustomerDetailsRepository;
import com.api.klarfinance.auth.repository.DetailUserInternalRepository;
import com.api.klarfinance.auth.repository.UserRepository;
import com.api.klarfinance.dbo.repository.BranchTerritoryRepository;
import com.api.klarfinance.fin.service.LoanReviewService;
import com.api.klarfinance.global.PictureService;
import com.api.klarfinance.global.PushNotificationService;
import com.api.klarfinance.los.EngineStatus;
import com.api.klarfinance.los.dto.request.BmDecisionRequest;
import com.api.klarfinance.los.dto.request.CheckerDecisionRequest;
import com.api.klarfinance.los.dto.response.ApprovalQueueItemResponse;
import com.api.klarfinance.los.dto.response.LimitApplicationDetailResponse;
import com.api.klarfinance.los.dto.response.LimitApplicationSearchResponse;
import com.api.klarfinance.los.model.ActiveLimit;
import com.api.klarfinance.los.model.ApplicationLog;
import com.api.klarfinance.los.model.LimitApplication;
import com.api.klarfinance.los.model.PefindoInquiry;
import com.api.klarfinance.los.model.Vida;
import com.api.klarfinance.los.repository.ActiveLimitRepository;
import com.api.klarfinance.los.repository.ApplicationLogRepository;
import com.api.klarfinance.los.repository.LimitApplicationRepository;
import com.api.klarfinance.los.repository.VidaRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LosApprovalService {
    private static final BigDecimal MAX_BM_INCREASE = BigDecimal.valueOf(2_000_000);
    /** Konfirmasi user 2026-08-31: plafond usulan Checker gak boleh jauh dari saran engine. */
    private static final BigDecimal CHECKER_LIMIT_TOLERANCE = new BigDecimal("0.20");

    private final LimitApplicationRepository limitApplicationRepository;
    private final ApplicationLogRepository applicationLogRepository;
    private final ActiveLimitRepository activeLimitRepository;
    private final UserRepository userRepository;
    private final DetailUserInternalRepository detailUserInternalRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final CheckerAssignmentService checkerAssignmentService;
    private final VidaRepository vidaRepository;
    private final BranchTerritoryRepository branchTerritoryRepository;
    private final PictureService pictureService;
    private final PushNotificationService pushNotificationService;
    private final LoanReviewService loanReviewService;

    /**
     * Checker cuma liat SATU aplikasi yang lagi di-lock ke dia (assignment real-time via
     * WebSocket, lihat CheckerAssignmentService) — bukan antrean penuh lagi.
     *
     * REVISI 2026-09-01 (konfirmasi user): BM sekarang cuma liat aplikasi yang wilayah nasabahnya
     * (regency dari CustomerDetails.village) masuk territory branch BM itu (lihat
     * BranchTerritoryService/branch-territory.md di knowledge base). Kalau BM gak punya branch,
     * atau branch-nya gak punya territory sama sekali, dia gak lihat apa-apa (bukan error) - itu
     * nandain gap yang perlu dibenerin lewat halaman Branch Territory (Superadmin).
     */
    public List<ApprovalQueueItemResponse> listQueue(Principal principal) {
        User caller = currentUser(principal, null);
        String role = caller.getRole().getName().toUpperCase();

        if ("CHECKER".equals(role)) {
            return checkerAssignmentService.currentAssignment(caller.getIdentity())
                    .map(applicationId -> {
                        checkerAssignmentService.startReviewIfNeeded(applicationId, caller.getIdentity());
                        return applicationId;
                    })
                    .flatMap(limitApplicationRepository::findById)
                    .map(app -> List.of(toSummary(app, caller)))
                    .orElseGet(List::of);
        }
        if ("BM".equals(role)) {
            Set<Long> territoryRegencyIds = bmTerritoryRegencyIds(caller);
            if (territoryRegencyIds.isEmpty()) return List.of();

            // Bucket per-branch (bm-approval-lock.md): SEMUA aplikasi di territory branch ini
            // kelihatan, termasuk yang lagi di-lock BM lain - toSummary nandain siapa yang pegang.
            List<ApprovalQueueItemResponse> limitApplications = limitApplicationRepository
                    .findByStatusOrderByCreatedAtAsc(EngineStatus.PENDING_BM).stream()
                    .filter(app -> territoryRegencyIds.contains(customerRegencyId(app)))
                    .map(app -> toSummary(app, caller))
                    .toList();

            // Digabung satu list dengan pengajuan pinjaman >30% yang butuh review ulang BM
            // (skip Checker, konfirmasi user) - dibedakan lewat ApprovalQueueItemResponse#type di
            // frontend. los -> fin dependency lewat service publik (bukan repository/entity
            // langsung), konsisten dengan precedent LosDashboardService -> FinDashboardService.
            List<ApprovalQueueItemResponse> loanReviews = loanReviewService.listQueueForBm(caller);

            return java.util.stream.Stream.concat(limitApplications.stream(), loanReviews.stream())
                    .sorted(java.util.Comparator.comparing(ApprovalQueueItemResponse::getCreatedAt))
                    .toList();
        }
        throw new IllegalStateException("Only Checker or BM can view this queue");
    }

    private Set<Long> bmTerritoryRegencyIds(User bm) {
        DetailUserInternal detail = detailUserInternalRepository.findByUserId(bm.getId()).orElse(null);
        if (detail == null || detail.getBranch() == null) return Set.of();

        return branchTerritoryRepository.findByBranchIdOrderByRegency_NameAsc(detail.getBranch().getId()).stream()
                .map(bt -> bt.getRegency().getId())
                .collect(Collectors.toSet());
    }

    private Long customerRegencyId(LimitApplication application) {
        CustomerDetails details = customerDetailsRepository.findByUserId(application.getUser().getId()).orElse(null);
        if (details == null || details.getVillage() == null) return null;
        return details.getVillage().getDistrict().getRegency().getId();
    }

    private ApprovalQueueItemResponse toSummary(LimitApplication application, User caller) {
        User customer = application.getUser();
        CustomerDetails details = customerDetailsRepository.findByUserId(customer.getId()).orElse(null);
        User lockedBm = application.getLockedBm();

        return ApprovalQueueItemResponse.builder()
                .type("LIMIT_APPLICATION")
                .id(application.getId())
                .applicationCode(application.getApplicationCode())
                .customerIdentity(customer.getIdentity())
                .customerName(details != null ? details.getName() : null)
                .incomeAmount(application.getIncomeAmount())
                .engineSuggestionLimit(application.getEngineSuggestionLimit())
                .checkerPurposeLimit(application.getCheckerPurposeLimit())
                .finalApprovedLimit(application.getFinalApprovedLimit())
                .pefindoScore(application.getPefindoInquiry() != null ? application.getPefindoInquiry().getScore() : null)
                .pefindoColStatus(application.getPefindoInquiry() != null ? application.getPefindoInquiry().getColStatus() : null)
                .status(application.getStatus())
                .createdAt(application.getCreatedAt())
                .lockedByIdentity(lockedBm != null ? lockedBm.getIdentity() : null)
                .lockedByName(internalUserName(lockedBm))
                .lockedByMe(lockedBm != null && caller != null && lockedBm.getId().equals(caller.getId()))
                .build();
    }

    private String internalUserName(User user) {
        if (user == null) return null;
        return detailUserInternalRepository.findByUserId(user.getId())
                .map(DetailUserInternal::getName)
                .orElse(user.getIdentity());
    }

    /**
     * Halaman detail Checker/BM. Cuma buat CHECKER/BM.
     *
     * REVISI 2026-09-01 (bm-approval-lock.md, konfirmasi user): khusus caller BM, buka detail ini
     * SEKARANG jadi trigger lock (beda dari Checker yang exclusivity-nya di-handle terpisah lewat
     * CheckerAssignmentService, bukan di sini) - begitu BM buka detail aplikasi yang masih
     * PENDING_BM, aplikasi itu otomatis ke-lock atas nama BM tersebut TANPA TTL, nempel terus
     * sampai dia beneran approve/reject (lihat lockForBmIfNeeded, bmDecide). BM juga dibatasi cuma
     * boleh buka aplikasi yang regency nasabahnya masuk territory branch-nya sendiri.
     */
    @Transactional
    public LimitApplicationDetailResponse getDetail(Integer applicationId, Principal principal) {
        User caller = currentUser(principal, null);
        String role = caller.getRole().getName().toUpperCase();
        if (!"CHECKER".equals(role) && !"BM".equals(role)) {
            throw new IllegalStateException("Only Checker or BM can view this application");
        }

        LimitApplication application = limitApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Limit application not found"));

        // Bucket-per-branch (bm-approval-lock.md): BM cuma boleh buka aplikasi yang regency
        // nasabahnya masuk territory branch-nya - konsisten sama filter di listQueue().
        if ("BM".equals(role)) {
            Set<Long> territoryRegencyIds = bmTerritoryRegencyIds(caller);
            if (!territoryRegencyIds.contains(customerRegencyId(application))) {
                throw new IllegalStateException("Aplikasi ini bukan bagian dari wilayah (territory) Anda");
            }
            lockForBmIfNeeded(application, caller);
        }

        User customer = application.getUser();
        CustomerDetails details = customerDetailsRepository.findByUserId(customer.getId()).orElse(null);
        Vida vida = vidaRepository.findFirstByUserIdOrderByCreatedAtDesc(customer.getId()).orElse(null);
        PefindoInquiry pefindo = application.getPefindoInquiry();
        // "CABANG" = cabang si Checker/BM yang lagi review, bukan cabang nasabah (nasabah gak
        // punya cabang - itu konsep internal staff/DetailUserInternal).
        String branchName = detailUserInternalRepository.findByUserId(caller.getId())
                .map(DetailUserInternal::getBranch)
                .map(branch -> branch != null ? branch.getName() : null)
                .orElse(null);

        BigDecimal engineSuggestion = application.getEngineSuggestionLimit();
        BigDecimal checkerMin = null;
        BigDecimal checkerMax = null;
        if (engineSuggestion != null && engineSuggestion.signum() > 0) {
            checkerMin = engineSuggestion.multiply(BigDecimal.ONE.subtract(CHECKER_LIMIT_TOLERANCE));
            checkerMax = engineSuggestion.multiply(BigDecimal.ONE.add(CHECKER_LIMIT_TOLERANCE));
        }

        int pinjolCount = countApps(application.getDetectedPinjolApps());
        int bankCount = countApps(application.getDetectedBankApps());

        return LimitApplicationDetailResponse.builder()
                .id(application.getId())
                .applicationCode(application.getApplicationCode())
                .status(application.getStatus())
                .customerIdentity(customer.getIdentity())
                .customerName(details != null ? details.getName() : null)
                .customerAddress(details != null ? details.getAddress() : null)
                .customerAddress2(details != null ? details.getAddress2() : null)
                .branchName(branchName)
                .provinceName(details != null && details.getVillage() != null
                        ? details.getVillage().getDistrict().getRegency().getProvince().getName() : null)
                .regencyName(details != null && details.getVillage() != null
                        ? details.getVillage().getDistrict().getRegency().getName() : null)
                .districtName(details != null && details.getVillage() != null
                        ? details.getVillage().getDistrict().getName() : null)
                .villageName(details != null && details.getVillage() != null ? details.getVillage().getName() : null)
                .hasFotoKtp(details != null && StringUtils.hasText(details.getFotoKtp()))
                .hasFotoKyc(details != null && StringUtils.hasText(details.getFotoKyc()))
                .incomeAmount(application.getIncomeAmount())
                .vida(vida == null ? null : LimitApplicationDetailResponse.VidaDetail.builder()
                        .kycStatus(vida.getKycStatus())
                        .kycFaceScore(vida.getKycFaceScore())
                        .kycVendorTrxId(vida.getKycVendorTrxId())
                        .incomeVerificationStatus(vida.getIncomeVerificationStatus())
                        .incomeVerificationSource(vida.getIncomeVerificationSource())
                        .incomeIsEmployed(vida.getIncomeIsEmployed())
                        .incomeVerifiedMonthly(vida.getIncomeVerifiedMonthly())
                        .createdAt(vida.getCreatedAt())
                        .build())
                .pefindo(pefindo == null ? null : LimitApplicationDetailResponse.PefindoDetail.builder()
                        .score(pefindo.getScore())
                        .colStatus(pefindo.getColStatus())
                        .riskLabel(pefindoRiskLabel(pefindo.getColStatus()))
                        .pdfPathFile(pefindo.getPdfPathFile())
                        .createdAt(pefindo.getCreatedAt())
                        .build())
                // TODO: "Positive Apps" & "Judol Apps" belum ada sumber datanya sama sekali di alur
                // onboarding sekarang (cuma pinjolApps & bankApps yang dikirim device app scoring,
                // lihat project_rules.md #5) - ditaruh 0 dulu, bukan angka sungguhan. Perlu
                // diklarifikasi ke user apakah kategori ini beneran mau dikumpulkan.
                .positiveAppsCount(0)
                .pinjolAppsCount(pinjolCount)
                .judolAppsCount(0)
                .bankingAppsCount(bankCount)
                .pinjolApps(splitApps(application.getDetectedPinjolApps()))
                .bankApps(splitApps(application.getDetectedBankApps()))
                .engineScore(application.getEngineScore())
                .engineRiskCategory(application.getEngineRiskCategory())
                .engineRecommendation(application.getEngineRecommendation())
                .engineKeyFactors(application.getEngineKeyFactors() == null
                        ? List.of() : Arrays.asList(application.getEngineKeyFactors().split(",")))
                .engineSuggestionLimit(engineSuggestion)
                .checkerLimitMin(checkerMin)
                .checkerLimitMax(checkerMax)
                .checkerRecommendation(application.getCheckerRecommendation())
                .checkerPurposeLimit(application.getCheckerPurposeLimit())
                .checkerReason(application.getCheckerReason())
                .finalApprovedLimit(application.getFinalApprovedLimit())
                .bmReason(application.getBmReason())
                .createdAt(application.getCreatedAt())
                .lockedByIdentity(application.getLockedBm() != null ? application.getLockedBm().getIdentity() : null)
                .lockedByName(internalUserName(application.getLockedBm()))
                .lockedByMe(application.getLockedBm() != null && application.getLockedBm().getId().equals(caller.getId()))
                .canReview(canReview(application, caller, role))
                .build();
    }

    /**
     * Serve foto KTP/selfie mentah (bukan lewat ApiResponse envelope, lihat controller) - dipakai
     * frontend Checker/BM sebagai <img> src. Auth-nya SAMA PERSIS dengan {@link #getDetail} (role
     * CHECKER/BM + territory check buat BM) - sengaja gak permitAll karena isinya foto KTP+wajah
     * asli nasabah, dan sengaja terima applicationId+type (bukan filename mentah) biar akses
     * tetap lewat scoping otorisasi aplikasi ini, bukan nebak-nebak nama file orang lain.
     */
    public Resource getPicture(Integer applicationId, String type, Principal principal) {
        User caller = currentUser(principal, null);
        String role = caller.getRole().getName().toUpperCase();
        if (!"CHECKER".equals(role) && !"BM".equals(role)) {
            throw new IllegalStateException("Only Checker or BM can view this application");
        }

        LimitApplication application = limitApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Limit application not found"));

        if ("BM".equals(role)) {
            Set<Long> territoryRegencyIds = bmTerritoryRegencyIds(caller);
            if (!territoryRegencyIds.contains(customerRegencyId(application))) {
                throw new IllegalStateException("Aplikasi ini bukan bagian dari wilayah (territory) Anda");
            }
        }

        CustomerDetails details = customerDetailsRepository.findByUserId(application.getUser().getId())
                .orElseThrow(() -> new IllegalStateException("Customer detail not found"));

        String filename = switch (type.toLowerCase(Locale.ROOT)) {
            case "ktp" -> details.getFotoKtp();
            case "kyc" -> details.getFotoKyc();
            default -> throw new IllegalArgumentException("Tipe foto tidak dikenal: " + type);
        };
        if (!StringUtils.hasText(filename)) {
            throw new IllegalArgumentException("Foto belum diupload nasabah");
        }

        try {
            return pictureService.loadImage(filename);
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membaca file foto", e);
        }
    }

    /**
     * Nyalain lock BM (bm-approval-lock.md) begitu dia buka detail - TANPA TTL, cuma no-op kalau
     * udah ada yang megang (baik dirinya sendiri atau BM lain) atau status udah lewat PENDING_BM.
     */
    private void lockForBmIfNeeded(LimitApplication application, User bm) {
        if (!EngineStatus.PENDING_BM.equals(application.getStatus())) return;
        if (application.getLockedBm() == null) {
            application.setLockedBm(bm);
            application.setLockedAt(java.time.LocalDateTime.now());
            limitApplicationRepository.save(application);
        }
    }

    private boolean canReview(LimitApplication application, User caller, String role) {
        if (!"BM".equals(role)) return true;
        return EngineStatus.PENDING_BM.equals(application.getStatus())
                && application.getLockedBm() != null
                && application.getLockedBm().getId().equals(caller.getId());
    }

    /**
     * Inquiry (konfirmasi user): cari aplikasi berdasarkan App ID, nama, atau nomor HP. Aplikasi
     * yang belum masuk proses pengecekan (RETAKE_PHOTO) sengaja dikecualikan di level repository.
     */
    public List<LimitApplicationSearchResponse> search(String q, Principal principal) {
        User caller = currentUser(principal, null);
        if (!StringUtils.hasText(q)) return List.of();

        List<LimitApplication> results = limitApplicationRepository.search(q.trim());

        // Inquiry(BM) (konfirmasi user 2026-09-01): dibatasi cuma ke bucket branch/territory BM
        // itu sendiri - beda dari Inquiry biasa punya Checker yang tetap global tanpa batas.
        String role = caller.getRole() != null ? caller.getRole().getName().toUpperCase() : null;
        if ("BM".equals(role)) {
            Set<Long> territoryRegencyIds = bmTerritoryRegencyIds(caller);
            results = results.stream()
                    .filter(app -> territoryRegencyIds.contains(customerRegencyId(app)))
                    .toList();
        }

        return results.stream()
                .map(app -> {
                    User customer = app.getUser();
                    CustomerDetails details = customerDetailsRepository.findByUserId(customer.getId()).orElse(null);
                    return LimitApplicationSearchResponse.builder()
                            .id(app.getId())
                            .applicationCode(app.getApplicationCode())
                            .customerName(details != null ? details.getName() : null)
                            .customerIdentity(customer.getIdentity())
                            .status(app.getStatus())
                            .build();
                })
                .toList();
    }

    /** Checker klik tombol "Istirahat" di halaman /approval - lihat CheckerAssignmentService#takeBreak. */
    public void checkerBreak(Principal principal) {
        User checker = currentUser(principal, "CHECKER");
        checkerAssignmentService.takeBreak(checker.getIdentity());
    }

    private int countApps(String joined) {
        if (!StringUtils.hasText(joined)) return 0;
        return joined.split(",").length;
    }

    private List<String> splitApps(String joined) {
        if (!StringUtils.hasText(joined)) return List.of();
        return List.of(joined.split(","));
    }

    private String pefindoRiskLabel(Integer colStatus) {
        if (colStatus == null || colStatus == 1) return "LOW RISK";
        if (colStatus == 5) return "HIGH RISK";
        return "MEDIUM RISK";
    }

    /**
     * REVISI 2026-08-31 (konfirmasi user): keputusan Checker - APPROVE maupun REJECT - sekarang
     * cuma REKOMENDASI, bukan final. Status selalu lanjut ke PENDING_BM, BM yang final mutusin
     * APPROVED/REJECTED (lihat {@link #bmDecide}). Dulu REJECT langsung final-REJECTED tanpa
     * lewat BM sama sekali - itu udah gak berlaku.
     */
    @Transactional
    public void checkerDecide(Integer applicationId, CheckerDecisionRequest request, Principal principal) {
        User checker = currentUser(principal, "CHECKER");
        LimitApplication application = limitApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Limit application not found"));

        if (!EngineStatus.PENDING_CHECKER.equals(application.getStatus())) {
            throw new IllegalStateException("Application is not pending Checker review");
        }

        String currentOwner = checkerAssignmentService.currentOwner(applicationId).orElse(null);
        if (currentOwner != null && !currentOwner.equals(checker.getIdentity())) {
            throw new IllegalStateException("Aplikasi ini sudah dialihkan ke Checker lain");
        }

        String action = normalizeAction(request.getAction());
        if ("APPROVE".equals(action)) {
            BigDecimal purposeLimit = request.getPurposeLimit();
            if (purposeLimit == null || purposeLimit.signum() <= 0) {
                throw new IllegalArgumentException("purposeLimit is required to approve");
            }
            validateAgainstEngineSuggestion(application.getEngineSuggestionLimit(), purposeLimit);
            application.setCheckerPurposeLimit(purposeLimit);
        } else {
            if (!StringUtils.hasText(request.getReason())) {
                throw new IllegalArgumentException("reason is required to reject");
            }
            application.setCheckerPurposeLimit(null);
        }

        application.setCheckerRecommendation(action);
        application.setCheckerReason(request.getReason());
        application.setStatus(EngineStatus.PENDING_BM);

        limitApplicationRepository.save(application);
        logAction(application, checker, action, request.getReason());
        checkerAssignmentService.release(applicationId);
    }

    /** Konfirmasi user 2026-08-31: plafond usulan Checker gak boleh lebih dari ±20% saran engine. */
    private void validateAgainstEngineSuggestion(BigDecimal engineSuggestion, BigDecimal purposeLimit) {
        if (engineSuggestion == null || engineSuggestion.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Engine tidak memberi saran limit untuk aplikasi ini, Checker tidak bisa approve dengan nominal - silakan reject");
        }
        BigDecimal min = engineSuggestion.multiply(BigDecimal.ONE.subtract(CHECKER_LIMIT_TOLERANCE));
        BigDecimal max = engineSuggestion.multiply(BigDecimal.ONE.add(CHECKER_LIMIT_TOLERANCE));
        if (purposeLimit.compareTo(min) < 0 || purposeLimit.compareTo(max) > 0) {
            throw new IllegalArgumentException(
                    "purposeLimit harus dalam rentang ±20% dari saran engine (Rp " + min.toPlainString() + " - Rp " + max.toPlainString() + ")");
        }
    }

    @Transactional
    public void bmDecide(Integer applicationId, BmDecisionRequest request, Principal principal) {
        User bm = currentUser(principal, "BM");
        LimitApplication application = limitApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Limit application not found"));

        if (!EngineStatus.PENDING_BM.equals(application.getStatus())) {
            throw new IllegalStateException("Application is not pending BM approval");
        }

        // Lock ownership (bm-approval-lock.md): kalau udah kepegang BM lain, tolak. Kalau belum
        // ada yang lock sama sekali (edge case - biasanya udah ke-lock otomatis pas getDetail),
        // klaim sekarang buat caller ini.
        User existingLock = application.getLockedBm();
        if (existingLock != null && !existingLock.getId().equals(bm.getId())) {
            throw new IllegalStateException("Aplikasi ini sedang direview BM lain");
        }
        if (existingLock == null) {
            application.setLockedBm(bm);
            application.setLockedAt(java.time.LocalDateTime.now());
        }

        String action = normalizeAction(request.getAction());
        if ("APPROVE".equals(action)) {
            BigDecimal finalLimit = request.getFinalLimit();
            if (finalLimit == null || finalLimit.signum() <= 0) {
                throw new IllegalArgumentException("finalLimit is required to approve");
            }
            // Checker bisa aja merekomendasikan REJECT (checkerPurposeLimit null) tapi BM tetap
            // boleh override approve - fallback ke saran engine sebagai baseline cap +2jt.
            BigDecimal checkerLimit = application.getCheckerPurposeLimit() != null
                    ? application.getCheckerPurposeLimit()
                    : (application.getEngineSuggestionLimit() != null ? application.getEngineSuggestionLimit() : BigDecimal.ZERO);
            BigDecimal maxAllowed = checkerLimit.add(MAX_BM_INCREASE);
            if (finalLimit.compareTo(maxAllowed) > 0) {
                throw new IllegalArgumentException("finalLimit cannot exceed checker's limit by more than Rp 2,000,000");
            }
            if (finalLimit.compareTo(checkerLimit) != 0 && !StringUtils.hasText(request.getReason())) {
                throw new IllegalArgumentException("reason is required when adjusting the checker's limit");
            }

            application.setFinalApprovedLimit(finalLimit);
            application.setBmReason(request.getReason());
            application.setStatus(EngineStatus.APPROVED);
            application.setBm(bm);
            limitApplicationRepository.save(application);

            activateLimit(application, bm, finalLimit);
            notifyDecision(application, "Pengajuan Disetujui",
                    "Selamat! Pengajuan pinjaman Anda disetujui dengan plafond Rp " + finalLimit.toPlainString() + ".",
                    "APPROVED");
        } else {
            if (!StringUtils.hasText(request.getReason())) {
                throw new IllegalArgumentException("reason is required to reject");
            }
            application.setBmReason(request.getReason());
            application.setStatus(EngineStatus.REJECTED);
            application.setBm(bm);
            limitApplicationRepository.save(application);
            notifyDecision(application, "Pengajuan Ditolak",
                    "Mohon maaf, pengajuan pinjaman Anda belum bisa kami setujui saat ini.",
                    "REJECTED");
        }

        logAction(application, bm, action, request.getReason());
    }

    private void activateLimit(LimitApplication application, User bm, BigDecimal finalLimit) {
        DetailUserInternal bmDetail = detailUserInternalRepository.findByUserId(bm.getId())
                .orElseThrow(() -> new IllegalStateException("BM has no branch assigned"));

        ActiveLimit activeLimit = ActiveLimit.builder()
                .user(application.getUser())
                .totalLimit(finalLimit)
                .usedLimit(BigDecimal.ZERO)
                .availableLimit(finalLimit)
                .branch(bmDetail.getBranch())
                .isActive(true)
                .build();
        activeLimitRepository.save(activeLimit);
    }

    /** Push notification (FCM) ke nasabah - lihat PushNotificationService, gagal kirim gak pernah
     * throw jadi ini aman dipanggil setelah save() tanpa perlu di-wrap try/catch di sini.
     * @param status "APPROVED"/"REJECTED" - dikirim sebagai data payload (type=LOAN_APPROVAL)
     *               di luar notification title/body, supaya client bisa refresh AccountState-nya
     *               sendiri kalau notifikasi ini diterima pas app lagi dibuka (foreground). */
    private void notifyDecision(LimitApplication application, String title, String body, String status) {
        customerDetailsRepository.findByUserId(application.getUser().getId())
                .ifPresent(details -> pushNotificationService.send(details.getFcmToken(), title, body,
                        Map.of("type", "LOAN_APPROVAL", "status", status)));
    }

    private void logAction(LimitApplication application, User actor, String action, String notes) {
        ApplicationLog log = ApplicationLog.builder()
                .application(application)
                .internalUser(actor)
                .action(action)
                .notes(notes)
                .build();
        applicationLogRepository.save(log);
    }

    private String normalizeAction(String action) {
        String normalized = action == null ? "" : action.trim().toUpperCase();
        if (!"APPROVE".equals(normalized) && !"REJECT".equals(normalized)) {
            throw new IllegalArgumentException("action must be APPROVE or REJECT");
        }
        return normalized;
    }

    private User currentUser(Principal principal, String expectedRole) {
        if (principal == null) throw new IllegalStateException("Authentication required");
        User user = userRepository.findByIdentity(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        if (user.getRole() == null) throw new IllegalStateException("User has no role assigned");
        if (expectedRole != null && !expectedRole.equalsIgnoreCase(user.getRole().getName())) {
            throw new IllegalStateException("Only " + expectedRole + " can perform this action");
        }
        return user;
    }
}
