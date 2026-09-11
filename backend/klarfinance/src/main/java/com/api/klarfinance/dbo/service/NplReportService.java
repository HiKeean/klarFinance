
package com.api.klarfinance.dbo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.api.klarfinance.dbo.dto.response.NplReportResponse;
import com.api.klarfinance.dbo.model.BranchTerritory;
import com.api.klarfinance.dbo.repository.BranchTerritoryRepository;
import com.api.klarfinance.fin.NplSeverity;
import com.api.klarfinance.fin.dto.response.BranchNplSummary;
import com.api.klarfinance.fin.service.FinDashboardService;

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
}
