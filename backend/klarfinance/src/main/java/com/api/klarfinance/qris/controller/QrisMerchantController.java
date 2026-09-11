package com.api.klarfinance.qris.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.klarfinance.global.ApiResponse;
import com.api.klarfinance.qris.dto.request.GenerateMerchantRequest;
import com.api.klarfinance.qris.dto.response.MerchantResponse;
import com.api.klarfinance.qris.service.QrisService;

/** Plain @RestController (bukan @Nasabah/@Internal/@Admin) -> resolve ke /api/v1/qris/merchants
 * (lihat WebConfig#configurePathMatch). SENGAJA publik tanpa login (permitAll di
 * SecurityConfiguration + HMAC-exempt di HmacSignatureFilter) - halaman generator toko cuma
 * HTML/JS polos tanpa kemampuan hitung signature, sesuai keputusan user "mockup, simple aja". */
@RestController
@RequestMapping("/qris/merchants")
@RequiredArgsConstructor
public class QrisMerchantController {
    private final QrisService qrisService;

    @PostMapping
    public ResponseEntity<ApiResponse<MerchantResponse>> generate(@RequestBody GenerateMerchantRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Merchant created successfully", qrisService.generateMerchant(request)));
    }
}
