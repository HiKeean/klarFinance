package com.api.klarfinance.dbo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.api.klarfinance.annotation.AdminAnnotation;
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
}
