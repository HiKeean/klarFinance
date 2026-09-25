package com.api.klarfinance.deskcall.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.api.klarfinance.annotation.AdminAnnotation;
import com.api.klarfinance.deskcall.DeskcallCallService;
import com.api.klarfinance.deskcall.dto.StartCallResponse;
import com.api.klarfinance.global.ApiResponse;

/** Superadmin-only (@AdminAnnotation -> /api/v1/admin/**). Tombol Call di drill-down NPL Report (demo deskcall). */
@AdminAnnotation
@RequiredArgsConstructor
@RequestMapping("/deskcall")
public class DeskcallController {
    private final DeskcallCallService deskcallCallService;

    @PostMapping("/loans/{loanId}/calls")
    public ResponseEntity<ApiResponse<StartCallResponse>> startCall(@PathVariable Integer loanId) {
        return ResponseEntity.ok(ApiResponse.success("Panggilan dikirim ke HP nasabah", deskcallCallService.startCall(loanId)));
    }
}
