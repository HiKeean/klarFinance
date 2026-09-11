package com.api.klarfinance.los.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.api.klarfinance.auth.model.CustomerDetails;
import com.api.klarfinance.los.model.Vida;
import com.api.klarfinance.los.repository.VidaRepository;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class MockVidaService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final VidaRepository vidaRepository;

    public VidaResult check(CustomerDetails details, BigDecimal claimedIncome) {
        int roll = RANDOM.nextInt(100);
        String status;
        if (roll < 10) {
            status = VidaResult.UNCLEAR;
        } else if (roll < 25) {
            status = VidaResult.REJECTED;
        } else {
            status = VidaResult.APPROVED;
        }

        boolean employed = status.equals(VidaResult.APPROVED) && RANDOM.nextInt(100) < 85;
        String verifiedIncome = status.equals(VidaResult.APPROVED)
                ? verifyIncome(claimedIncome)
                : null;
        Integer faceScore = status.equals(VidaResult.UNCLEAR) ? 30 + RANDOM.nextInt(20) : 70 + RANDOM.nextInt(30);

        Vida vida = Vida.builder()
                .user(details.getUser())
                .kycStatus(status)
                .kycFaceScore(String.valueOf(faceScore))
                .kycVendorTrxId("VIDA-MOCK-" + System.currentTimeMillis())
                .incomeVerificationStatus(status.equals(VidaResult.APPROVED) ? "VERIFIED" : "NOT_VERIFIED")
                .incomeVerificationSource("MOCK_VIDA")
                .incomeIsEmployed(String.valueOf(employed))
                .incomeVerifiedMonthly(verifiedIncome)
                .raw_response("{\"mock\":true,\"status\":\"" + status + "\"}")
                .build();
        vidaRepository.save(vida);

        log.info("Mock Vida check for user {} -> {}", details.getUser().getId(), status);
        return new VidaResult(status, employed, verifiedIncome);
    }

    private String verifyIncome(BigDecimal claimedIncome) {
        BigDecimal base = claimedIncome == null ? BigDecimal.valueOf(4_000_000) : claimedIncome;
        double variance = 0.8 + RANDOM.nextDouble() * 0.4;
        return base.multiply(BigDecimal.valueOf(variance)).setScale(0, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
