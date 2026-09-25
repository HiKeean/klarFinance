package com.api.klarfinance.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.api.klarfinance.config.AppConfigProperties;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KirimiWhatsappService {
    private final AppConfigProperties properties;
    private final RestClient restClient = RestClient.builder().build();

    public void sendRegistrationMessage(String phone, String identity, String password, String name) {
        AppConfigProperties.Kirimi kirimi = properties.getKirimi();
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("noHp is required to send registration WhatsApp");
        }
        if (kirimi == null || isBlank(kirimi.getUserCode())
                || isBlank(kirimi.getSecret()) || isBlank(kirimi.getDeviceId())) {
            throw new IllegalStateException("Kirimi WhatsApp configuration is incomplete");
        }
        String normalizedPhone = normalizePhone(phone);

        String message = "Selamat bergabung " + name + " ke KlarFinance. \nBerikut adalah rincian identity anda\n"
                + "Identity : " + identity + "\n"
                + "Password : " + password + "\n"
                + "Segera ganti password anda!";

        if (Boolean.FALSE.equals(kirimi.getEnabled())) {
            log.info("[KIRIMI DISABLED] Registration WhatsApp buat {} ({}) gak dikirim beneran, isinya:\n{}", identity, normalizedPhone, message);
            return;
        }

        try {
            restClient.post()
                    .uri(kirimi.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "user_code", kirimi.getUserCode(),
                            "secret", kirimi.getSecret(),
                            "device_id", kirimi.getDeviceId(),
                            "phone", normalizedPhone,
                            "message", message
                    ))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Registration WhatsApp sent for identity {}", identity);
        } catch (Exception exception) {
            log.error("Failed to send registration WhatsApp for identity {}", identity, exception);
            throw new IllegalStateException("Failed to send registration WhatsApp", exception);
        }
    }

    public void sendPasswordResetMessage(String phone, String identity, String newPassword) {
        AppConfigProperties.Kirimi kirimi = properties.getKirimi();
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("noHp is required to send password reset WhatsApp");
        }
        if (kirimi == null || isBlank(kirimi.getUserCode())
                || isBlank(kirimi.getSecret()) || isBlank(kirimi.getDeviceId())) {
            throw new IllegalStateException("Kirimi WhatsApp configuration is incomplete");
        }
        String normalizedPhone = normalizePhone(phone);

        String message = "Password akun KlarFinance Anda (identity: " + identity + ") telah direset oleh admin.\n"
                + "Password baru Anda: " + newPassword + "\n"
                + "Segera login dan ganti password anda!";

        if (Boolean.FALSE.equals(kirimi.getEnabled())) {
            log.info("[KIRIMI DISABLED] Password reset WhatsApp buat {} ({}) gak dikirim beneran, isinya:\n{}", identity, normalizedPhone, message);
            return;
        }

        try {
            restClient.post()
                    .uri(kirimi.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "user_code", kirimi.getUserCode(),
                            "secret", kirimi.getSecret(),
                            "device_id", kirimi.getDeviceId(),
                            "phone", normalizedPhone,
                            "message", message
                    ))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Password reset WhatsApp sent for identity {}", identity);
        } catch (Exception exception) {
            log.error("Failed to send password reset WhatsApp for identity {}", identity, exception);
            throw new IllegalStateException("Failed to send password reset WhatsApp", exception);
        }
    }

    public void sendOtpMessage(String phone, String otp) {
        AppConfigProperties.Kirimi kirimi = properties.getKirimi();
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("noHp is required to send OTP WhatsApp");
        }
        if (kirimi == null || isBlank(kirimi.getUserCode())
                || isBlank(kirimi.getSecret()) || isBlank(kirimi.getDeviceId())) {
            throw new IllegalStateException("Kirimi WhatsApp configuration is incomplete");
        }
        String normalizedPhone = normalizePhone(phone);

        String message = "Kode verifikasi KlarFinance kamu: " + otp + "\n"
                + "Berlaku 5 menit. Jangan bagikan kode ini ke siapa pun, termasuk pihak yang mengaku dari KlarFinance.";

        if (Boolean.FALSE.equals(kirimi.getEnabled())) {
            log.info("[KIRIMI DISABLED] OTP buat {} gak dikirim beneran -> {}", normalizedPhone, otp);
            return;
        }

        try {
            restClient.post()
                    .uri(kirimi.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "user_code", kirimi.getUserCode(),
                            "secret", kirimi.getSecret(),
                            "device_id", kirimi.getDeviceId(),
                            "phone", normalizedPhone,
                            "message", message
                    ))
                    .retrieve()
                    .toBodilessEntity();
            log.info("OTP WhatsApp sent for phone {}", normalizedPhone);
        } catch (Exception exception) {
            log.error("Failed to send OTP WhatsApp for phone {}", normalizedPhone, exception);
            throw new IllegalStateException("Failed to send OTP WhatsApp", exception);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizePhone(String phone) {
        String normalized = phone.replaceAll("[^0-9]", "");

        if (normalized.startsWith("0")) {
            normalized = "62" + normalized.substring(1);
        } else if (normalized.startsWith("8")) {
            normalized = "62" + normalized;
        }

        if (!normalized.startsWith("62") || normalized.length() < 10
                || normalized.length() > 15) {
            throw new IllegalArgumentException(
                    "Invalid WhatsApp phone number. Use Indonesian format, for example 628123456789");
        }

        return normalized;
    }
}
