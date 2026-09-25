package com.api.klarfinance.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.klarfinance.auth.dto.request.CustomerRegisterRequest;
import com.api.klarfinance.auth.dto.response.CustomerRegisterResponse;
import com.api.klarfinance.auth.model.CustomerDetails;
import com.api.klarfinance.auth.model.Role;
import com.api.klarfinance.auth.model.User;
import com.api.klarfinance.auth.repository.CustomerDetailsRepository;
import com.api.klarfinance.auth.repository.RoleRepository;
import com.api.klarfinance.auth.repository.UserRepository;
import com.api.klarfinance.dbo.model.Village;
import com.api.klarfinance.dbo.repository.VillageRepository;
import com.api.klarfinance.global.AppConstant;
import com.api.klarfinance.global.PictureService;
import com.api.klarfinance.global.TooManyRequestsException;
import com.api.klarfinance.los.service.EngineScoringResult;
import com.api.klarfinance.los.service.EngineScoringService;
import com.api.klarfinance.referral.service.ReferralService;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration OTP_VERIFIED_TTL = Duration.ofMinutes(15);
    // Batas kirim OTP per nomor - spam request-otp ke banyak nomor/berulang adalah pola yang paling
    // cepat bikin nomor WhatsApp Kirimi kena banned.
    private static final Duration OTP_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration OTP_COUNT_WINDOW = Duration.ofHours(1);
    private static final long OTP_MAX_PER_WINDOW = 5;
    // Per IP lebih longgar dari per nomor - banyak nasabah di belakang CGNAT operator seluler berbagi IP.
    private static final long OTP_MAX_PER_IP_PER_WINDOW = 30;
    private static final long OTP_MAX_VERIFY_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final KirimiWhatsappService kirimiWhatsappService;
    private final FirebasePhoneTokenVerifier firebasePhoneTokenVerifier;
    private final UserRepository userRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final RoleRepository roleRepository;
    private final VillageRepository villageRepository;
    private final PasswordEncoder passwordEncoder;
    private final PictureService pictureService;
    private final EngineScoringService engineScoringService;
    private final ReferralService referralService;

    /** @return channel OTP: {@link AppConstant#OTP_CHANNEL_WHATSAPP}, atau
     * {@link AppConstant#OTP_CHANNEL_FIREBASE_SMS} kalau kirim WhatsApp gagal (mis. nomor Kirimi kena banned). */
    public String requestOtp(String phone, String clientIp) {
        String normalizedPhone = normalizePhone(phone);
        enforceIpRateLimit(clientIp);
        enforceOtpRateLimit(normalizedPhone);
        String otp = generateOtp();

        redisTemplate.opsForValue().set(otpKey(normalizedPhone), otp, OTP_TTL);
        redisTemplate.delete(otpAttemptKey(normalizedPhone));
        try {
            kirimiWhatsappService.sendOtpMessage(normalizedPhone, otp);
        } catch (IllegalStateException e) {
            redisTemplate.delete(otpKey(normalizedPhone));
            log.warn("OTP WhatsApp gagal untuk {}, fallback ke Firebase SMS", normalizedPhone);
            return AppConstant.OTP_CHANNEL_FIREBASE_SMS;
        }

        log.info("OTP generated and sent for phone {}", normalizedPhone);
        return AppConstant.OTP_CHANNEL_WHATSAPP;
    }

    public void verifyFirebasePhone(String phone, String idToken) {
        String normalizedPhone = normalizePhone(phone);
        String tokenPhone = firebasePhoneTokenVerifier.verifyAndGetPhone(idToken).replaceAll("[^0-9]", "");
        if (!normalizedPhone.equals(tokenPhone)) {
            throw new IllegalArgumentException("Nomor HP tidak sesuai dengan nomor yang diverifikasi via SMS");
        }
        markOtpVerified(normalizedPhone);
        log.info("Phone verified via Firebase SMS: {}", normalizedPhone);
    }

    public void verifyOtp(String phone, String otp) {
        String normalizedPhone = normalizePhone(phone);
        String key = otpKey(normalizedPhone);
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp == null) {
            throw new IllegalArgumentException("OTP has expired or was not requested");
        }
        if (!storedOtp.equals(otp)) {
            String attemptKey = otpAttemptKey(normalizedPhone);
            Long attempts = redisTemplate.opsForValue().increment(attemptKey);
            if (attempts != null && attempts == 1) {
                redisTemplate.expire(attemptKey, OTP_TTL);
            }
            if (attempts != null && attempts >= OTP_MAX_VERIFY_ATTEMPTS) {
                // OTP dibuang biar 6 digit gak bisa di-brute-force selama masa berlaku 5 menit.
                redisTemplate.delete(key);
                redisTemplate.delete(attemptKey);
                throw new IllegalArgumentException("Terlalu banyak percobaan salah, silakan minta OTP baru");
            }
            throw new IllegalArgumentException("Invalid OTP");
        }

        redisTemplate.delete(key);
        redisTemplate.delete(otpAttemptKey(normalizedPhone));
        markOtpVerified(normalizedPhone);
        log.info("OTP verified successfully for phone {}", normalizedPhone);
    }

    private void markOtpVerified(String normalizedPhone) {
        redisTemplate.opsForValue().set(otpVerifiedKey(normalizedPhone), "1", OTP_VERIFIED_TTL);
    }

    public boolean isRegistered(String phone) {
        return userRepository.findByIdentity(normalizePhone(phone)).isPresent();
    }

    @Transactional
    public CustomerRegisterResponse register(String phone, CustomerRegisterRequest request) {
        String normalizedPhone = normalizePhone(phone);
        String verifiedKey = otpVerifiedKey(normalizedPhone);

        if (redisTemplate.opsForValue().get(verifiedKey) == null) {
            throw new IllegalArgumentException("Phone number has not been OTP-verified. Please verify OTP first");
        }
        if (userRepository.findByIdentity(normalizedPhone).isPresent()) {
            throw new IllegalArgumentException("This phone number is already registered. Please login instead");
        }
        require(request.getPassword(), "password");
        require(request.getName(), "name");

        Village village = request.getVillageId() == null
                ? null
                : villageRepository.findById(request.getVillageId())
                        .orElseThrow(() -> new IllegalArgumentException("Village not found"));

        String fotoKtpPath = saveImage(request.getFotoKtp(), "foto KTP");
        String fotoKycPath = saveImage(request.getFotoKyc(), "foto KYC");

        Role nasabahRole = roleRepository.findByName(AppConstant.ROLE_NASABAH)
                .orElseGet(() -> roleRepository.save(Role.builder().name(AppConstant.ROLE_NASABAH).build()));

        User user = userRepository.save(User.builder()
                .identity(normalizedPhone)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(nasabahRole)
                .build());

        CustomerDetails details = customerDetailsRepository.save(CustomerDetails.builder()
                .user(user)
                .nik(request.getNik())
                .npwp(request.getNpwp())
                .name(request.getName())
                .email(request.getEmail())
                .address(request.getAddress())
                .address2(request.getAddress2())
                .dob(request.getDob())
                .village(village)
                .fotoKtp(fotoKtpPath)
                .fotoKyc(fotoKycPath)
                .build());

        referralService.onCustomerRegistered(details, request.getReferralCode());

        redisTemplate.delete(verifiedKey);

        EngineScoringResult scoringResult = engineScoringService.runScoring(
                user, details, request.getPinjolApps(), request.getBankApps());

        log.info("Customer registered and scored: phone={}, status={}", normalizedPhone, scoringResult.status());

        return CustomerRegisterResponse.builder()
                .identity(normalizedPhone)
                .applicationStatus(scoringResult.status())
                .suggestedLimit(scoringResult.suggestedLimit())
                .message(messageFor(scoringResult.status()))
                .build();
    }

    private String saveImage(org.springframework.web.multipart.MultipartFile file, String label) {
        try {
            return pictureService.saveImage(file);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save " + label, e);
        }
    }

    private String messageFor(String status) {
        return switch (status) {
            case "RETAKE_PHOTO" -> "Please retake your KTP/KYC photo and try again";
            case "REJECTED" -> "Your application could not be approved at this time";
            default -> "Registration successful, your application is being reviewed";
        };
    }

    private void require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private void enforceIpRateLimit(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return;
        }
        String countKey = AppConstant.OTP_IP_COUNT_KEY_PREFIX + clientIp;
        Long count = redisTemplate.opsForValue().increment(countKey);
        if (count != null && count == 1) {
            redisTemplate.expire(countKey, OTP_COUNT_WINDOW);
        }
        if (count != null && count > OTP_MAX_PER_IP_PER_WINDOW) {
            throw new TooManyRequestsException("Terlalu banyak permintaan OTP, coba lagi dalam 1 jam");
        }
    }

    private void enforceOtpRateLimit(String phone) {
        String cooldownKey = AppConstant.OTP_COOLDOWN_KEY_PREFIX + phone;
        Boolean cooldownSet = redisTemplate.opsForValue().setIfAbsent(cooldownKey, "1", OTP_COOLDOWN);
        if (!Boolean.TRUE.equals(cooldownSet)) {
            Long ttl = redisTemplate.getExpire(cooldownKey);
            long wait = ttl == null || ttl < 1 ? OTP_COOLDOWN.toSeconds() : ttl;
            throw new TooManyRequestsException("Tunggu " + wait + " detik sebelum minta OTP lagi");
        }

        String countKey = AppConstant.OTP_HOURLY_COUNT_KEY_PREFIX + phone;
        Long count = redisTemplate.opsForValue().increment(countKey);
        if (count != null && count == 1) {
            redisTemplate.expire(countKey, OTP_COUNT_WINDOW);
        }
        if (count != null && count > OTP_MAX_PER_WINDOW) {
            throw new TooManyRequestsException("Terlalu banyak permintaan OTP, coba lagi dalam 1 jam");
        }
    }

    private String generateOtp() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private String otpKey(String phone) {
        return AppConstant.OTP_KEY_PREFIX + phone;
    }

    private String otpAttemptKey(String phone) {
        return AppConstant.OTP_ATTEMPT_KEY_PREFIX + phone;
    }

    private String otpVerifiedKey(String phone) {
        return AppConstant.OTP_VERIFIED_KEY_PREFIX + phone;
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("phone is required");
        }
        String normalized = phone.replaceAll("[^0-9]", "");

        if (normalized.startsWith("0")) {
            normalized = "62" + normalized.substring(1);
        } else if (normalized.startsWith("8")) {
            normalized = "62" + normalized;
        }

        if (!normalized.startsWith("62") || normalized.length() < 10 || normalized.length() > 15) {
            throw new IllegalArgumentException(
                    "Invalid phone number. Use Indonesian format, for example 628123456789");
        }

        return normalized;
    }
}
