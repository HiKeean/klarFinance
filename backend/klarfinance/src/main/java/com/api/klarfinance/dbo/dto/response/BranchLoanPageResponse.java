package com.api.klarfinance.dbo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import com.api.klarfinance.fin.dto.response.BranchLoanDetailResponse;
import com.api.klarfinance.global.ApiResponsePagination;

/** Drill-down NPL Report satu branch (webadmin) - header ("total asset yang dikelola" = total
 * pinjaman aktif branch ini, jumlah peminjam) + tabel pinjaman per-nasabah yang bisa dicari &
 * dipaginasi. Lihat NplReportController#branchLoans. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchLoanPageResponse {
    private Long branchId;
    private String branchName;

    /** Total nominal seluruh pinjaman aktif (requestedAmount) di branch ini - TIDAK terpengaruh
     * filter search tabel, selalu total branch secara keseluruhan. */
    private BigDecimal totalAssets;
    private long activeBorrowers;

    private ApiResponsePagination<BranchLoanDetailResponse> loans;
}
