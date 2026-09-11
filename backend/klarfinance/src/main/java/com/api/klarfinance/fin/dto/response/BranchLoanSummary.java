package com.api.klarfinance.fin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchLoanSummary {
    private BigDecimal loansInRegion;
    private long activeBorrowers;
    private long delinquentBorrowers;
    private List<StatusSlice> loanStatusDistribution;

    // NPL per branch (konfirmasi user 2026-09-01): overdueLoanCount / activeLoanCount x 100.
    // Dihitung dari jumlah PINJAMAN (bukan jumlah nasabah/borrower - beda dari activeBorrowers
    // di atas kalau ada nasabah dengan >1 pinjaman aktif).
    private long activeLoanCount;
    private long overdueLoanCount;
    private BigDecimal nplPercent;
    private String nplSeverity;
    private BigDecimal totalOutstandingPenalty;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusSlice {
        private String label;
        private int percent;
    }
}
