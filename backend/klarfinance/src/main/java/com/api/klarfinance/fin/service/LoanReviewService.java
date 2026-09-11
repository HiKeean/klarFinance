package com.api.klarfinance.fin.service;

import lombok.RequiredArgsConstructor;
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
import com.api.klarfinance.fin.dto.request.LoanReviewDecisionRequest;
import com.api.klarfinance.fin.dto.response.LoanReviewDetailResponse;
import com.api.klarfinance.fin.model.LoanReviewRequest;
import com.api.klarfinance.fin.repository.LoanReviewRequestRepository;
import com.api.klarfinance.global.PushNotificationService;
import com.api.klarfinance.los.EngineStatus;
import com.api.klarfinance.los.dto.response.ApprovalQueueItemResponse;
import com.api.klarfinance.los.model.ActiveLimit;
import com.api.klarfinance.los.repository.ActiveLimitRepository;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Approval BM untuk pengajuan pinjaman yang melewati 30% dari plafond (LoanReviewRequest) -
 * SKIP Checker sepenuhnya (konfirmasi user), langsung ke antrean BM. Mirror pola
 * lock-on-open-detail + filter territory milik los.LosApprovalService/bm-approval-lock.md, tapi
 * duplikasi kecil di sini dianggap pragmatis (fin sudah punya precedent akses langsung repo
 * auth/dbo lewat LosDashboardService) daripada memaksakan coupling lintas-module yang canggung
 * untuk logic sekecil ini.
 */
@Service
@RequiredArgsConstructor
public class LoanReviewService {
    private final LoanReviewRequestRepository loanReviewRequestRepository;
    private final ActiveLimitRepository activeLimitRepository;
    private final UserRepository userRepository;
    private final DetailUserInternalRepository detailUserInternalRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final BranchTerritoryRepository branchTerritoryRepository;
    private final PushNotificationService pushNotificationService;
    private final LoanService loanService;

    public List<ApprovalQueueItemResponse> listQueueForBm(User caller) {
        Set<Long> territoryRegencyIds = bmTerritoryRegencyIds(caller);
        if (territoryRegencyIds.isEmpty()) return List.of();

        return loanReviewRequestRepository.findByStatusOrderByCreatedAtAsc(EngineStatus.PENDING_BM).stream()
                .filter(review -> territoryRegencyIds.contains(customerRegencyId(review.getUser())))
                .map(review -> toQueueItem(review, caller))
                .toList();
    }

    private ApprovalQueueItemResponse toQueueItem(LoanReviewRequest review, User caller) {
        User customer = review.getUser();
        CustomerDetails details = customerDetailsRepository.findByUserId(customer.getId()).orElse(null);
        User lockedBm = review.getLockedBm();

        return ApprovalQueueItemResponse.builder()
                .type("LOAN_REVIEW")
                .id(review.getId())
                .customerIdentity(customer.getIdentity())
                .customerName(details != null ? details.getName() : null)
                .status(review.getStatus())
                .createdAt(review.getCreatedAt())
                .requestedAmount(review.getRequestedAmount())
                .tenorMonths(review.getTenorMonths())
                .usedLimitSnapshot(review.getUsedLimitSnapshot())
                .totalLimitSnapshot(review.getTotalLimitSnapshot())
                .utilizationPercent(review.getUtilizationPercent())
                .pefindoScore(review.getPefindoScore())
                .pefindoColStatus(review.getPefindoColStatus())
                .lockedByIdentity(lockedBm != null ? lockedBm.getIdentity() : null)
                .lockedByName(internalUserName(lockedBm))
                .lockedByMe(lockedBm != null && caller != null && lockedBm.getId().equals(caller.getId()))
                .build();
    }

    @Transactional
    public LoanReviewDetailResponse getDetail(Integer id, Principal principal) {
        User caller = currentBm(principal);
        LoanReviewRequest review = loanReviewRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Loan review request not found"));

        Set<Long> territoryRegencyIds = bmTerritoryRegencyIds(caller);
        if (!territoryRegencyIds.contains(customerRegencyId(review.getUser()))) {
            throw new IllegalStateException("Pengajuan ini bukan bagian dari wilayah (territory) Anda");
        }
        lockForBmIfNeeded(review, caller);

        User customer = review.getUser();
        CustomerDetails details = customerDetailsRepository.findByUserId(customer.getId()).orElse(null);

        return LoanReviewDetailResponse.builder()
                .id(review.getId())
                .status(review.getStatus())
                .customerIdentity(customer.getIdentity())
                .customerName(details != null ? details.getName() : null)
                .customerAddress(details != null ? details.getAddress() : null)
                .provinceName(details != null && details.getVillage() != null
                        ? details.getVillage().getDistrict().getRegency().getProvince().getName() : null)
                .regencyName(details != null && details.getVillage() != null
                        ? details.getVillage().getDistrict().getRegency().getName() : null)
                .requestedAmount(review.getRequestedAmount())
                .tenorMonths(review.getTenorMonths())
                .bankAccountNumber(review.getBankAccountNumber())
                .bankCode(review.getBankCode())
                .usedLimitSnapshot(review.getUsedLimitSnapshot())
                .totalLimitSnapshot(review.getTotalLimitSnapshot())
                .utilizationPercent(review.getUtilizationPercent())
                .projectedTotalDebt(review.getUsedLimitSnapshot().add(review.getRequestedAmount()))
                .pefindoScore(review.getPefindoScore())
                .pefindoColStatus(review.getPefindoColStatus())
                .pefindoRiskLabel(pefindoRiskLabel(review.getPefindoColStatus()))
                .pinjolAppsCount(countApps(review.getDetectedPinjolApps()))
                .bankingAppsCount(countApps(review.getDetectedBankApps()))
                .pinjolApps(splitApps(review.getDetectedPinjolApps()))
                .bankApps(splitApps(review.getDetectedBankApps()))
                .bmReason(review.getBmReason())
                .createdAt(review.getCreatedAt())
                .lockedByIdentity(review.getLockedBm() != null ? review.getLockedBm().getIdentity() : null)
                .lockedByName(internalUserName(review.getLockedBm()))
                .lockedByMe(review.getLockedBm() != null && review.getLockedBm().getId().equals(caller.getId()))
                .canReview(canReview(review, caller))
                .build();
    }

    @Transactional
    public void bmDecide(Integer id, LoanReviewDecisionRequest request, Principal principal) {
        User bm = currentBm(principal);
        LoanReviewRequest review = loanReviewRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Loan review request not found"));

        if (!EngineStatus.PENDING_BM.equals(review.getStatus())) {
            throw new IllegalStateException("Loan review request is not pending BM approval");
        }

        User existingLock = review.getLockedBm();
        if (existingLock != null && !existingLock.getId().equals(bm.getId())) {
            throw new IllegalStateException("Pengajuan ini sedang direview BM lain");
        }
        if (existingLock == null) {
            review.setLockedBm(bm);
            review.setLockedAt(LocalDateTime.now());
        }

        String action = normalizeAction(request.getAction());
        if ("APPROVE".equals(action)) {
            // Re-validasi TERKINI - limit tersedia nasabah bisa berubah sejak snapshot diambil
            // (mis. ada pinjaman lain yang diproses di antara submit dan review ini).
            ActiveLimit activeLimit = activeLimitRepository.findById(review.getActiveLimit().getId())
                    .orElseThrow(() -> new IllegalStateException("Active limit not found"));
            if (review.getRequestedAmount().compareTo(activeLimit.getAvailableLimit()) > 0) {
                throw new IllegalStateException(
                        "Limit tersedia nasabah sudah berubah dan tidak lagi cukup untuk pengajuan ini - tidak bisa diproses");
            }

            loanService.finalizeLoan(activeLimit, review.getUser(), review.getRequestedAmount(),
                    review.getTenorMonths(), review.getBankAccountNumber(), review.getBankCode());

            review.setStatus(EngineStatus.APPROVED);
            review.setBm(bm);
            review.setBmReason(request.getReason());
            loanReviewRequestRepository.save(review);
            notifyDecision(review, "Pengajuan Disetujui",
                    "Pengajuan pinjaman tambahan Anda sebesar Rp " + review.getRequestedAmount().toPlainString() + " telah disetujui.",
                    "APPROVED");
        } else {
            if (!StringUtils.hasText(request.getReason())) {
                throw new IllegalArgumentException("reason is required to reject");
            }
            review.setStatus(EngineStatus.REJECTED);
            review.setBm(bm);
            review.setBmReason(request.getReason());
            loanReviewRequestRepository.save(review);
            notifyDecision(review, "Pengajuan Ditolak",
                    "Mohon maaf, pengajuan pinjaman tambahan Anda belum bisa kami setujui saat ini.",
                    "REJECTED");
        }
    }

    private void lockForBmIfNeeded(LoanReviewRequest review, User bm) {
        if (!EngineStatus.PENDING_BM.equals(review.getStatus())) return;
        if (review.getLockedBm() == null) {
            review.setLockedBm(bm);
            review.setLockedAt(LocalDateTime.now());
            loanReviewRequestRepository.save(review);
        }
    }

    private boolean canReview(LoanReviewRequest review, User caller) {
        return EngineStatus.PENDING_BM.equals(review.getStatus())
                && review.getLockedBm() != null
                && review.getLockedBm().getId().equals(caller.getId());
    }

    private void notifyDecision(LoanReviewRequest review, String title, String body, String status) {
        customerDetailsRepository.findByUserId(review.getUser().getId())
                .ifPresent(details -> pushNotificationService.send(details.getFcmToken(), title, body,
                        Map.of("type", "LOAN_REVIEW_DECISION", "status", status)));
    }

    private Set<Long> bmTerritoryRegencyIds(User bm) {
        DetailUserInternal detail = detailUserInternalRepository.findByUserId(bm.getId()).orElse(null);
        if (detail == null || detail.getBranch() == null) return Set.of();

        return branchTerritoryRepository.findByBranchIdOrderByRegency_NameAsc(detail.getBranch().getId()).stream()
                .map(bt -> bt.getRegency().getId())
                .collect(Collectors.toSet());
    }

    private Long customerRegencyId(User customer) {
        CustomerDetails details = customerDetailsRepository.findByUserId(customer.getId()).orElse(null);
        if (details == null || details.getVillage() == null) return null;
        return details.getVillage().getDistrict().getRegency().getId();
    }

    private String internalUserName(User user) {
        if (user == null) return null;
        return detailUserInternalRepository.findByUserId(user.getId())
                .map(DetailUserInternal::getName)
                .orElse(user.getIdentity());
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

    private String normalizeAction(String action) {
        String normalized = action == null ? "" : action.trim().toUpperCase();
        if (!"APPROVE".equals(normalized) && !"REJECT".equals(normalized)) {
            throw new IllegalArgumentException("action must be APPROVE or REJECT");
        }
        return normalized;
    }

    private User currentBm(Principal principal) {
        if (principal == null) throw new IllegalStateException("Authentication required");
        User user = userRepository.findByIdentity(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        if (user.getRole() == null || !"BM".equalsIgnoreCase(user.getRole().getName())) {
            throw new IllegalStateException("Only BM can perform this action");
        }
        return user;
    }
}
