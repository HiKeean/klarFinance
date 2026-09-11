package com.api.klarfinance.los.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.api.klarfinance.annotation.InternalAnnotation;
import com.api.klarfinance.global.ApiResponse;
import com.api.klarfinance.los.dto.request.BmDecisionRequest;
import com.api.klarfinance.los.dto.request.CheckerDecisionRequest;
import com.api.klarfinance.los.dto.response.ApprovalQueueItemResponse;
import com.api.klarfinance.los.dto.response.LimitApplicationDetailResponse;
import com.api.klarfinance.los.dto.response.LimitApplicationSearchResponse;
import com.api.klarfinance.los.service.LosApprovalService;

import java.security.Principal;
import java.util.List;

@InternalAnnotation
@RequestMapping("/los/limit-applications")
@RequiredArgsConstructor
public class LosApprovalController {
    private final LosApprovalService approvalService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ApprovalQueueItemResponse>>> queue(Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Queue fetched successfully", approvalService.listQueue(principal)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LimitApplicationDetailResponse>> detail(
            @PathVariable Integer id, Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Detail fetched successfully", approvalService.getDetail(id, principal)));
    }

    /** Serve foto KTP/selfie mentah (bukan lewat ApiResponse envelope) - dipakai frontend
     * Checker/BM sebagai <img> src. {@code type} = "ktp" atau "kyc", auth+scoping lihat
     * LosApprovalService#getPicture. */
    @GetMapping("/{id}/picture/{type}")
    public ResponseEntity<Resource> picture(
            @PathVariable Integer id, @PathVariable String type, Principal principal) {
        Resource resource = approvalService.getPicture(id, type, principal);
        MediaType contentType = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(contentType).body(resource);
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<LimitApplicationSearchResponse>>> search(
            @RequestParam(required = false) String q, Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Search fetched successfully", approvalService.search(q, principal)));
    }

    @PostMapping("/{id}/checker-decision")
    public ResponseEntity<ApiResponse<Object>> checkerDecision(
            @PathVariable Integer id,
            @RequestBody CheckerDecisionRequest request,
            Principal principal) {
        approvalService.checkerDecide(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success("Checker decision recorded successfully", null));
    }

    @PostMapping("/{id}/bm-decision")
    public ResponseEntity<ApiResponse<Object>> bmDecision(
            @PathVariable Integer id,
            @RequestBody BmDecisionRequest request,
            Principal principal) {
        approvalService.bmDecide(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success("BM decision recorded successfully", null));
    }
}
