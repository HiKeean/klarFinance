package com.api.klarfinance.fin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.api.klarfinance.annotation.InternalAnnotation;
import com.api.klarfinance.fin.dto.request.LoanReviewDecisionRequest;
import com.api.klarfinance.fin.dto.response.LoanReviewDetailResponse;
import com.api.klarfinance.fin.service.LoanReviewService;
import com.api.klarfinance.global.ApiResponse;

import java.security.Principal;

/** Approval BM untuk pengajuan pinjaman >30% dari plafond - lihat LoanReviewService. Item-nya
 * juga muncul digabung ke antrean BM utama lewat GET /internal/los/limit-applications (lihat
 * LosApprovalService#listQueue), endpoint di sini khusus buat detail + keputusan per item. */
@InternalAnnotation
@RequestMapping("/fin/loan-reviews")
@RequiredArgsConstructor
public class LoanReviewController {
    private final LoanReviewService loanReviewService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LoanReviewDetailResponse>> detail(
            @PathVariable Integer id, Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Detail fetched successfully", loanReviewService.getDetail(id, principal)));
    }

    @PostMapping("/{id}/bm-decision")
    public ResponseEntity<ApiResponse<Object>> bmDecision(
            @PathVariable Integer id,
            @RequestBody LoanReviewDecisionRequest request,
            Principal principal) {
        loanReviewService.bmDecide(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success("BM decision recorded successfully", null));
    }
}
