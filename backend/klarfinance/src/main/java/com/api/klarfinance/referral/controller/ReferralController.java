package com.api.klarfinance.referral.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.api.klarfinance.annotation.NasabahAnnotation;
import com.api.klarfinance.auth.repository.UserRepository;
import com.api.klarfinance.global.ApiResponse;
import com.api.klarfinance.referral.dto.response.ReferralSummaryResponse;
import com.api.klarfinance.referral.service.ReferralService;

import java.security.Principal;

@NasabahAnnotation
@RequestMapping("/referral")
@RequiredArgsConstructor
public class ReferralController {
    private final ReferralService referralService;
    private final UserRepository userRepository;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ReferralSummaryResponse>> summary(Principal principal) {
        var user = userRepository.findByIdentity(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        return ResponseEntity.ok(ApiResponse.success("Referral summary retrieved", referralService.getSummary(user)));
    }
}
