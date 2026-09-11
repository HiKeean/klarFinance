package com.api.klarfinance.qris.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.api.klarfinance.annotation.NasabahAnnotation;
import com.api.klarfinance.global.ApiResponse;
import com.api.klarfinance.qris.dto.request.QrisConfirmRequest;
import com.api.klarfinance.qris.dto.request.QrisScanRequest;
import com.api.klarfinance.qris.dto.response.QrisConfirmResponse;
import com.api.klarfinance.qris.dto.response.QrisScanResponse;
import com.api.klarfinance.qris.service.QrisService;

import java.security.Principal;

@NasabahAnnotation
@RequestMapping("/qris/transactions")
@RequiredArgsConstructor
public class QrisTransactionController {
    private final QrisService qrisService;

    @PostMapping("/scan")
    public ResponseEntity<ApiResponse<QrisScanResponse>> scan(@RequestBody QrisScanRequest request, Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("QR scanned successfully", qrisService.scan(request, principal)));
    }

    @PostMapping("/{token}/confirm")
    public ResponseEntity<ApiResponse<QrisConfirmResponse>> confirm(
            @PathVariable String token, @RequestBody QrisConfirmRequest request, Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Payment confirmed successfully", qrisService.confirm(token, request, principal)));
    }
}
