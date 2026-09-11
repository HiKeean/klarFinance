package com.api.klarfinance.los.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.api.klarfinance.los.repository.LimitApplicationRepository;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * App ID nasabah-facing (konfirmasi user): 8 digit - YYMMDD (tanggal pengajuan) + 2 digit random.
 * Beda dari primary key `id` internal yang tetap dipakai buat routing/API (App ID cuma buat
 * ditampilkan & dicari di halaman Inquiry).
 */
@Component
@RequiredArgsConstructor
public class ApplicationCodeGenerator {
    private static final DateTimeFormatter DATE_PART = DateTimeFormatter.ofPattern("yyMMdd");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_ATTEMPTS = 20;

    private final LimitApplicationRepository limitApplicationRepository;

    public String generate() {
        String datePart = LocalDate.now().format(DATE_PART);
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = datePart + String.format("%02d", RANDOM.nextInt(100));
            if (!limitApplicationRepository.existsByApplicationCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Gagal generate App ID unik setelah " + MAX_ATTEMPTS + " percobaan");
    }
}
