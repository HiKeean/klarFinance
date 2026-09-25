package com.api.klarfinance.auth.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Verifikasi Firebase ID token dari Firebase Phone Auth (fallback OTP via SMS kalau WhatsApp
 * Kirimi gagal/kena banned). Dipisah dari AuthService supaya bisa di-mock di unit test.
 */
@Slf4j
@Component
public class FirebasePhoneTokenVerifier {

    /** @return nomor HP terverifikasi dari klaim phone_number token (format E.164, mis. +628123...) */
    public String verifyAndGetPhone(String idToken) {
        if (FirebaseApp.getApps().isEmpty()) {
            throw new IllegalStateException("Verifikasi SMS belum tersedia, coba lagi nanti");
        }
        try {
            FirebaseToken token = FirebaseAuth.getInstance().verifyIdToken(idToken);
            Object phone = token.getClaims().get("phone_number");
            if (phone == null) {
                throw new IllegalArgumentException("Token Firebase tidak berisi nomor HP");
            }
            return phone.toString();
        } catch (FirebaseAuthException e) {
            log.warn("Firebase ID token ditolak: {}", e.getMessage());
            throw new IllegalArgumentException("Verifikasi SMS tidak valid atau sudah kedaluwarsa");
        }
    }
}
