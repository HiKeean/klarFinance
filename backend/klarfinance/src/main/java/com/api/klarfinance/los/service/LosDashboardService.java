package com.api.klarfinance.los.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.api.klarfinance.auth.model.DetailUserInternal;
import com.api.klarfinance.auth.model.User;
import com.api.klarfinance.auth.repository.DetailUserInternalRepository;
import com.api.klarfinance.auth.repository.UserRepository;
import com.api.klarfinance.fin.dto.response.BranchLoanSummary;
import com.api.klarfinance.fin.service.FinDashboardService;
import com.api.klarfinance.los.EngineStatus;
import com.api.klarfinance.los.dto.response.DashboardSummaryResponse;
import com.api.klarfinance.los.repository.LimitApplicationRepository;

import java.security.Principal;

@Service
@RequiredArgsConstructor
public class LosDashboardService {
    private final LimitApplicationRepository limitApplicationRepository;
    private final UserRepository userRepository;
    private final DetailUserInternalRepository detailUserInternalRepository;
    private final FinDashboardService finDashboardService;
    private final LosApprovalService losApprovalService;

    public DashboardSummaryResponse getSummary(Principal principal) {
        boolean isBm = isBm(principal);

        // BM's "needs to review" harus jumlah PENDING_BM di bucket territory branch-nya sendiri
        // (sama persis logic listQueue()), BUKAN antrean Checker global - dua role beda queue.
        long needsToReview = isBm
                ? losApprovalService.listQueue(principal).size()
                : limitApplicationRepository.countByStatus(EngineStatus.PENDING_CHECKER);

        DashboardSummaryResponse.DashboardSummaryResponseBuilder response = DashboardSummaryResponse.builder()
                .needsToReview(needsToReview);

        if (isBm) {
            Long branchId = resolveCallerBranchId(principal);
            BranchLoanSummary branchSummary = finDashboardService.getBranchSummary(branchId);
            response.loansInRegion(branchSummary.getLoansInRegion())
                    .activeBorrowers(branchSummary.getActiveBorrowers())
                    .delinquentBorrowers(branchSummary.getDelinquentBorrowers())
                    .loanStatusDistribution(branchSummary.getLoanStatusDistribution())
                    .nplPercent(branchSummary.getNplPercent())
                    .nplSeverity(branchSummary.getNplSeverity())
                    .totalOutstandingPenalty(branchSummary.getTotalOutstandingPenalty());
        }

        return response.build();
    }

    private boolean isBm(Principal principal) {
        if (principal == null) return false;
        return userRepository.findByIdentity(principal.getName())
                .map(User::getRole)
                .map(role -> "BM".equalsIgnoreCase(role.getName()))
                .orElse(false);
    }

    private Long resolveCallerBranchId(Principal principal) {
        User user = userRepository.findByIdentity(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        DetailUserInternal detail = detailUserInternalRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("BM has no branch assigned"));
        return detail.getBranch() != null ? detail.getBranch().getId() : null;
    }
}
