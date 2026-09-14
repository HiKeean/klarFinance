package com.api.klarfinance.fin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Satu baris pinjaman nasabah di tabel drill-down NPL Report per branch (webadmin) - lihat
 * FinDashboardService#getBranchLoanDetails. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchLoanDetailResponse {
    private Integer loanId;
    private String nasabahName;
    private BigDecimal loanAmount;

    /** LoanStatusBucket.CURRENT / OVERDUE, atau "Lunas" kalau tidak ada installment UNPAID lagi. */
    private String status;

    /** 0 kalau status bukan OVERDUE. */
    private long daysOverdue;
}
