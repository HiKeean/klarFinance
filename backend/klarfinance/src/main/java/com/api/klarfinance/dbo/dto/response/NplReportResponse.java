package com.api.klarfinance.dbo.dto.response;

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
public class NplReportResponse {
    private Long branchId;
    private String branchName;
    private long activeLoanCount;
    private long overdueLoanCount;
    private BigDecimal nplPercent;
    private String nplSeverity;
    private BigDecimal totalOutstandingPenalty;

    // Regency yang jadi territory branch ini - dipakai FE buat mewarnai peta per wilayah.
    private List<Long> regencyIds;
}
