package com.api.klarfinance.geo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.api.klarfinance.annotation.NasabahAnnotation;
import com.api.klarfinance.geo.dto.request.LocationConsentRequest;
import com.api.klarfinance.geo.dto.request.LocationFailureLogRequest;
import com.api.klarfinance.geo.dto.request.LocationPingRequest;
import com.api.klarfinance.geo.dto.response.LocationConsentResponse;
import com.api.klarfinance.geo.service.LocationTrackingService;
import com.api.klarfinance.global.ApiResponse;

import java.security.Principal;

@NasabahAnnotation
@RequestMapping("/location")
@RequiredArgsConstructor
public class LocationTrackingController {
    private final LocationTrackingService locationService;

    @GetMapping("/consent")
    public ResponseEntity<ApiResponse<LocationConsentResponse>> getConsent(Principal principal) {
        boolean consent = locationService.getConsent(principal);
        return ResponseEntity.ok(ApiResponse.success("Consent state retrieved",
                LocationConsentResponse.builder().consentGiven(consent).build()));
    }

    @PatchMapping("/consent")
    public ResponseEntity<ApiResponse<Void>> setConsent(@RequestBody LocationConsentRequest request, Principal principal) {
        locationService.setConsent(principal, request.isConsent());
        return ResponseEntity.ok(ApiResponse.success("Consent updated", null));
    }

    @PostMapping("/pings")
    public ResponseEntity<ApiResponse<Void>> submitPing(@RequestBody LocationPingRequest request, Principal principal) {
        locationService.submitPing(principal, request);
        return ResponseEntity.ok(ApiResponse.success("Location recorded", null));
    }

    @PostMapping("/failure-log")
    public ResponseEntity<ApiResponse<Void>> logFailure(@RequestBody LocationFailureLogRequest request, Principal principal) {
        locationService.logFailure(principal, request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Logged", null));
    }
}
