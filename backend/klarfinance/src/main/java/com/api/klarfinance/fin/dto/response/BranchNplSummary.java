package com.api.klarfinance.fin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchNplSummary {
    private Long branchId;
    private String branchName;
    private long activeLoanCount;
    private long overdueLoanCount;
    private BigDecimal nplPercent;
    private String nplSeverity;
    private BigDecimal totalOutstandingPenalty;
}
