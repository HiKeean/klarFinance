package com.api.klarfinance.los.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

import com.api.klarfinance.fin.dto.response.BranchLoanSummary;

@Data
@Builder
public class DashboardSummaryResponse {
    private long needsToReview;

    // Null buat Checker — cuma dihitung buat caller role BM.
    private BigDecimal loansInRegion;
    private Long activeBorrowers;
    private Long delinquentBorrowers;
    private List<BranchLoanSummary.StatusSlice> loanStatusDistribution;

    // NPL branch BM sendiri (konfirmasi user 2026-09-01) - null buat Checker, sama kayak field di atas.
    private BigDecimal nplPercent;
    private String nplSeverity;
    private BigDecimal totalOutstandingPenalty;
}
