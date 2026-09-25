package com.api.klarfinance.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api.klarfinance.auth.dto.request.CustomerRegisterRequest;
import com.api.klarfinance.auth.dto.request.PasswordResetRequestDto;
import com.api.klarfinance.auth.dto.request.RequestOtpRequest;
import com.api.klarfinance.auth.dto.request.VerifyFirebasePhoneRequest;
import com.api.klarfinance.auth.dto.request.VerifyOtpRequest;
import com.api.klarfinance.auth.dto.response.CustomerRegisterResponse;
import com.api.klarfinance.auth.service.AuthService;
import com.api.klarfinance.auth.service.PasswordResetRequestService;
import com.api.klarfinance.global.ApiResponse;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetRequestService passwordResetRequestService;

    @PostMapping("/request-otp")
    public ResponseEntity<ApiResponse<Object>> requestOtp(@Valid @RequestBody RequestOtpRequest request,
                                                          HttpServletRequest httpRequest) {
        String channel = authService.requestOtp(request.getPhone(), clientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully", Map.of("channel", channel)));
    }

    @PostMapping("/verify-firebase-phone")
    public ResponseEntity<ApiResponse<Object>> verifyFirebasePhone(@Valid @RequestBody VerifyFirebasePhoneRequest request) {
        authService.verifyFirebasePhone(request.getPhone(), request.getIdToken());
        return ResponseEntity.ok(ApiResponse.success("Phone verified successfully", null));
    }

    /** Backend di belakang openresty/nginx yang menambahkan IP klien di akhir X-Forwarded-For
     * ($proxy_add_x_forwarded_for) - ambil entri terakhir, entri awal bisa dipalsukan klien. */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            return parts[parts.length - 1].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Object>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request.getPhone(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.success("OTP verified successfully", null));
    }

    @GetMapping("/check-phone")
    public ResponseEntity<ApiResponse<Boolean>> checkPhone(@RequestParam String phone) {
        boolean registered = authService.isRegistered(phone);
        return ResponseEntity.ok(ApiResponse.success("Phone checked successfully", registered));
    }

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CustomerRegisterResponse>> register(
            @RequestParam String phone,
            @ModelAttribute CustomerRegisterRequest request) {
        CustomerRegisterResponse response = authService.register(phone, request);
        return ResponseEntity.ok(ApiResponse.success("Registration successful", response));
    }

    /** Dipicu dari tombol "Lupa Password" di halaman login (frontend Checker&BM, muncul setelah
     * 3x salah password) - masuk antrean PENDING di webadmin buat di-approve/reject Superadmin. */
    @PostMapping("/password-reset-requests")
    public ResponseEntity<ApiResponse<Object>> requestPasswordReset(@Valid @RequestBody PasswordResetRequestDto request) {
        passwordResetRequestService.submitRequest(request.getIdentity());
        return ResponseEntity.ok(ApiResponse.success("Permintaan reset password terkirim, tunggu persetujuan admin", null));
    }
}
