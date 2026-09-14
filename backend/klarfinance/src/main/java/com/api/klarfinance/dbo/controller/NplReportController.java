package com.api.klarfinance.dbo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.api.klarfinance.annotation.AdminAnnotation;
import com.api.klarfinance.dbo.dto.response.BranchLoanPageResponse;
import com.api.klarfinance.dbo.dto.response.NplReportResponse;
import com.api.klarfinance.dbo.service.NplReportService;
import com.api.klarfinance.global.ApiResponse;

import java.util.List;

/** Superadmin-only (lihat SecurityConfiguration - @AdminAnnotation -> /api/v1/admin/** -> hasAuthority("SUPERADMIN")). */
@AdminAnnotation
@RequiredArgsConstructor
@RequestMapping("/dbo/npl-report")
public class NplReportController {
    private final NplReportService nplReportService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NplReportResponse>>> report() {
        return ResponseEntity.ok(ApiResponse.success("NPL report fetched successfully", nplReportService.getReport()));
    }

    /** Drill-down klik branch - tabel pinjaman nasabah branch itu, searchable + paginated
     * (lihat NplReportService#getBranchLoanPage). */
    @GetMapping("/{branchId}/loans")
    public ResponseEntity<ApiResponse<BranchLoanPageResponse>> branchLoans(
            @PathVariable Long branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                "Branch loan detail fetched successfully",
                nplReportService.getBranchLoanPage(branchId, search, pageable)));
    }
}
