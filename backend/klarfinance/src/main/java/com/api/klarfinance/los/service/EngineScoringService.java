package com.api.klarfinance.los.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.klarfinance.auth.model.CustomerDetails;
import com.api.klarfinance.auth.model.User;
import com.api.klarfinance.los.EngineStatus;
import com.api.klarfinance.los.model.ApplicationLog;
import com.api.klarfinance.los.model.LimitApplication;
import com.api.klarfinance.los.repository.ApplicationLogRepository;
import com.api.klarfinance.los.repository.LimitApplicationRepository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Single entry point other modules must go through to run Engine Scoring -
 * per the modular-monolith rule, the auth module calls this service, never
 * los repositories/entities directly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EngineScoringService {

    private final MockVidaService mockVidaService;
    private final MockPefindoService mockPefindoService;
    private final EngineScoringDomain engineScoringDomain;
    private final LimitApplicationRepository limitApplicationRepository;
    private final ApplicationLogRepository applicationLogRepository;
    private final CheckerAssignmentService checkerAssignmentService;
    private final ApplicationCodeGenerator applicationCodeGenerator;

    @Transactional
    public EngineScoringResult runScoring(
            User user,
            CustomerDetails details,
            List<String> pinjolApps,
            List<String> bankApps) {

        VidaResult vidaResult = mockVidaService.check(details);

        LimitApplication.LimitApplicationBuilder builder = LimitApplication.builder()
                .user(user)
                .incomeAmount(vidaResult.verifiedMonthlyIncome() == null ? null : new BigDecimal(vidaResult.verifiedMonthlyIncome()))
                .detectedPinjolApps(join(pinjolApps))
                .detectedBankApps(join(bankApps));

        EngineDecision decision;
        if (!vidaResult.isApproved()) {
            // UNCLEAR -> RETAKE_PHOTO, REJECTED -> EngineStatus.REJECTED (lihat EngineScoringDomain) -
            // keduanya hard stop, gak lewat Pefindo karena identitasnya sendiri belum lolos.
            decision = engineScoringDomain.decide(vidaResult, null, 0);
        } else {
            PefindoResult pefindoResult = mockPefindoService.check();
            int pinjolCount = pinjolApps == null ? 0 : pinjolApps.size();
            decision = engineScoringDomain.decide(vidaResult, pefindoResult, pinjolCount);
            builder.pefindoInquiry(pefindoResult.inquiry());
        }

        builder.engineSuggestionLimit(decision.suggestedLimit())
                .engineRecommendation(decision.recommendation())
                .engineRiskCategory(decision.riskCategory())
                .engineKeyFactors(decision.keyFactors() == null ? null : String.join(",", decision.keyFactors()))
                .engineScore(decision.engineScore());

        // App ID (Inquiry) cuma buat aplikasi yang beneran masuk proses pengecekan -
        // RETAKE_PHOTO (Vida belum lolos) gak pernah masuk antrean Checker sama sekali.
        if (!EngineStatus.RETAKE_PHOTO.equals(decision.status())) {
            builder.applicationCode(applicationCodeGenerator.generate());
        }

        LimitApplication application = builder.status(decision.status()).build();
        limitApplicationRepository.save(application);

        applicationLogRepository.save(ApplicationLog.builder()
                .application(application)
                .action("ENGINE_SCORED")
                .notes("Engine scoring result: " + decision.status()
                        + (decision.suggestedLimit() != null ? ", suggestedLimit=" + decision.suggestedLimit() : ""))
                .build());

        log.info("Engine scoring finished for user {} -> {}", user.getId(), decision.status());

        if (EngineStatus.PENDING_CHECKER.equals(application.getStatus())) {
            checkerAssignmentService.assignNewApplication(application);
        }

        return new EngineScoringResult(decision.status(), decision.suggestedLimit());
    }

    private String join(List<String> values) {
        return values == null || values.isEmpty() ? null : String.join(",", values);
    }
}
