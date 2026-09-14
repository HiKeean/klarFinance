
package com.api.klarfinance.dbo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.api.klarfinance.dbo.dto.response.BranchLoanPageResponse;
import com.api.klarfinance.dbo.dto.response.NplReportResponse;
import com.api.klarfinance.dbo.model.Branch;
import com.api.klarfinance.dbo.model.BranchTerritory;
import com.api.klarfinance.dbo.repository.BranchRepository;
import com.api.klarfinance.dbo.repository.BranchTerritoryRepository;
import com.api.klarfinance.fin.NplSeverity;
import com.api.klarfinance.fin.dto.response.BranchLoanSummary;
import com.api.klarfinance.fin.dto.response.BranchNplSummary;
import com.api.klarfinance.fin.service.FinDashboardService;
import com.api.klarfinance.global.ApiResponsePagination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * NPL report per branch buat webadmin (Superadmin) - peta Indonesia diwarnai per wilayah Branch
 * (konfirmasi user 2026-09-01). Basis-nya SEMUA branch yang punya territory (bukan cuma yang
 * punya pinjaman aktif) - branch tanpa pinjaman tetap muncul, default hijau/0.
 */
@Service
@RequiredArgsConstructor
public class NplReportService {
    private final FinDashboardService finDashboardService;
    private final BranchTerritoryRepository branchTerritoryRepository;
    private final BranchRepository branchRepository;

    public List<NplReportResponse> getReport() {
        List<BranchTerritory> allTerritory = branchTerritoryRepository.findAllByOrderByRegency_NameAsc();

        Map<Long, List<Long>> regencyIdsByBranch = allTerritory.stream()
                .collect(Collectors.groupingBy(
                        bt -> bt.getBranch().getId(),
                        Collectors.mapping(bt -> bt.getRegency().getId(), Collectors.toList())));

        Map<Long, String> branchNameById = allTerritory.stream()
                .collect(Collectors.toMap(
                        bt -> bt.getBranch().getId(),
                        bt -> bt.getBranch().getName(),
                        (a, b) -> a));

        Map<Long, BranchNplSummary> summaryByBranchId = finDashboardService.getAllBranchSummaries().stream()
                .collect(Collectors.toMap(BranchNplSummary::getBranchId, s -> s));

        return regencyIdsByBranch.keySet().stream()
                .map(branchId -> {
                    BranchNplSummary summary = summaryByBranchId.get(branchId);
                    return NplReportResponse.builder()
                            .branchId(branchId)
                            .branchName(branchNameById.get(branchId))
                            .activeLoanCount(summary != null ? summary.getActiveLoanCount() : 0)
                            .overdueLoanCount(summary != null ? summary.getOverdueLoanCount() : 0)
                            .nplPercent(summary != null ? summary.getNplPercent() : BigDecimal.ZERO)
                            .nplSeverity(summary != null ? summary.getNplSeverity() : NplSeverity.GREEN)
                            .totalOutstandingPenalty(summary != null ? summary.getTotalOutstandingPenalty() : BigDecimal.ZERO)
                            .regencyIds(regencyIdsByBranch.get(branchId))
                            .build();
                })
                .sorted(Comparator.comparing(NplReportResponse::getBranchName))
                .toList();
    }

    /** Drill-down waktu branch di-klik di NPL Report (webadmin) - header total asset/jumlah
     * peminjam SELALU dari SEMUA pinjaman branch ini (BranchLoanSummary, tidak terpengaruh
     * search), tabelnya sendiri yang searchable + paginated. */
    public BranchLoanPageResponse getBranchLoanPage(Long branchId, String search, Pageable pageable) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found: " + branchId));

        BranchLoanSummary summary = finDashboardService.getBranchSummary(branchId);
        Page<com.api.klarfinance.fin.dto.response.BranchLoanDetailResponse> loanPage =
                finDashboardService.getBranchLoanDetails(branchId, search, pageable);

        return BranchLoanPageResponse.builder()
                .branchId(branch.getId())
                .branchName(branch.getName())
                .totalAssets(summary.getLoansInRegion())
                .activeBorrowers(summary.getActiveBorrowers())
                .loans(ApiResponsePagination.from(loanPage))
                .build();
    }
}
