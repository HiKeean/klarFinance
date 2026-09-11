package com.api.klarfinance.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.api.klarfinance.annotation.InternalAnnotation;
import com.api.klarfinance.auth.dto.AuthenticationRequest;
import com.api.klarfinance.auth.dto.RegisterRequest;
import com.api.klarfinance.auth.dto.request.ChangePasswordRequest;
import com.api.klarfinance.auth.dto.request.EditProfileRequest;
import com.api.klarfinance.auth.dto.request.FcmTokenRequest;
import com.api.klarfinance.auth.dto.request.RefreshTokenRequest;
import com.api.klarfinance.auth.dto.request.VerifyPasswordRequest;
import com.api.klarfinance.auth.dto.response.LoginResponse;
import com.api.klarfinance.auth.dto.response.ProfileResponse;
import com.api.klarfinance.auth.service.AuthenticationInternalService;
import com.api.klarfinance.global.ApiResponse;

import java.security.Principal;

@InternalAnnotation
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationInternalController {
    private final AuthenticationInternalService service;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login successful", service.login(request)));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Object>> register(@RequestBody RegisterRequest request) {
        service.register(request); return ResponseEntity.ok(ApiResponse.success("Registration successful", null));
    }

    /** Public (no Authorization header expected/checked) - the refresh token itself, validated
     * against the DB + JWT signature/expiry, is the credential here. Used by the Kotlin app's
     * fingerprint-gated session restore (see kotlin-nasabah-app knowledge). */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", service.refresh(request.getRefreshToken())));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(Principal principal, HttpServletRequest request) {
        service.logout(principal, request); return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    @PatchMapping("/change-password")
    public ResponseEntity<ApiResponse<Object>> changePassword(@RequestBody ChangePasswordRequest request, Principal principal) {
        service.changePassword(request, principal); return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    /** Step-up auth generik dipakai sebelum konfirmasi transaksi (pinjaman/QRIS) kalau nasabah
     * belum aktifkan fingerprint - lihat AuthenticationInternalService#verifyPassword. */
    @PostMapping("/verify-password")
    public ResponseEntity<ApiResponse<Object>> verifyPassword(@RequestBody VerifyPasswordRequest request, Principal principal) {
        service.verifyPassword(request, principal); return ResponseEntity.ok(ApiResponse.success("Password verified successfully", null));
    }

    /** Registrasi/update FCM token buat push notification approve/reject (nasabah-only, no-op
     * kalau caller-nya staff - lihat AuthenticationInternalService#updateFcmToken). */
    @PatchMapping("/fcm-token")
    public ResponseEntity<ApiResponse<Object>> fcmToken(@RequestBody FcmTokenRequest request, Principal principal) {
        service.updateFcmToken(request, principal); return ResponseEntity.ok(ApiResponse.success("FCM token updated successfully", null));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> profile(Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Profile fetched successfully", service.profile(principal)));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> edit(@RequestBody EditProfileRequest request, Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", service.edit(request, principal)));
    }
}
