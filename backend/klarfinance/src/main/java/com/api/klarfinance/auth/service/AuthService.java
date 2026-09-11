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
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final KirimiWhatsappService kirimiWhatsappService;
    private final UserRepository userRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final RoleRepository roleRepository;
    private final VillageRepository villageRepository;
    private final PasswordEncoder passwordEncoder;
    private final PictureService pictureService;
    private final EngineScoringService engineScoringService;
    private final ReferralService referralService;

    public void requestOtp(String phone) {
        String normalizedPhone = normalizePhone(phone);
        String otp = generateOtp();

        redisTemplate.opsForValue().set(otpKey(normalizedPhone), otp, OTP_TTL);
        kirimiWhatsappService.sendOtpMessage(normalizedPhone, otp);

        log.info("OTP generated and sent for phone {}", normalizedPhone);
    }

    public void verifyOtp(String phone, String otp) {
        String normalizedPhone = normalizePhone(phone);
        String key = otpKey(normalizedPhone);
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp == null) {
            throw new IllegalArgumentException("OTP has expired or was not requested");
        }
        if (!storedOtp.equals(otp)) {
            throw new IllegalArgumentException("Invalid OTP");
        }

        redisTemplate.delete(key);
        redisTemplate.opsForValue().set(otpVerifiedKey(normalizedPhone), "1", OTP_VERIFIED_TTL);
        log.info("OTP verified successfully for phone {}", normalizedPhone);
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
                user, details, request.getClaimedIncome(), request.getPinjolApps(), request.getBankApps());

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

    private String generateOtp() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private String otpKey(String phone) {
        return AppConstant.OTP_KEY_PREFIX + phone;
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
