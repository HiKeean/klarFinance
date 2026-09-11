package com.api.klarfinance.los.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.api.klarfinance.annotation.InternalAnnotation;
import com.api.klarfinance.global.ApiResponse;
import com.api.klarfinance.los.dto.response.DashboardSummaryResponse;
import com.api.klarfinance.los.service.LosApprovalService;
import com.api.klarfinance.los.service.LosDashboardService;

import java.security.Principal;

@InternalAnnotation
@RequestMapping("/los")
@RequiredArgsConstructor
public class LosController {
    private final LosDashboardService dashboardService;
    private final LosApprovalService approvalService;

    @GetMapping("/dashboard-summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> dashboardSummary(Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Dashboard summary fetched successfully", dashboardService.getSummary(principal)));
    }

    @PostMapping("/checker/break")
    public ResponseEntity<ApiResponse<Object>> checkerBreak(Principal principal) {
        approvalService.checkerBreak(principal);
        return ResponseEntity.ok(ApiResponse.success("Checker break recorded successfully", null));
    }
}
