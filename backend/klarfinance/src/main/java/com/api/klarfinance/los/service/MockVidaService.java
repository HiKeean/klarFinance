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

    public VidaResult check(CustomerDetails details) {
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
                ? mockVerifiedIncome()
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

    /** Income comes only from Vida (never from the client): random Rp3jt-Rp15jt, rounded to Rp100rb. */
    private String mockVerifiedIncome() {
        long hundredThousands = 30 + RANDOM.nextInt(121);
        return BigDecimal.valueOf(hundredThousands * 100_000L).toPlainString();
    }
}
