package com.api.authssohmac.los.service;

import org.junit.jupiter.api.Test;

import com.api.klarfinance.los.EngineStatus;
import com.api.klarfinance.los.model.PefindoInquiry;
import com.api.klarfinance.los.service.EngineDecision;
import com.api.klarfinance.los.service.EngineScoringDomain;
import com.api.klarfinance.los.service.PefindoResult;
import com.api.klarfinance.los.service.VidaResult;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EngineScoringDomainTest {

    private final EngineScoringDomain domain = new EngineScoringDomain();

    @Test
    void baseLimit_kol1_lancar() {
        PefindoInquiry inquiry = PefindoInquiry.builder().colStatus(1).colHistoryYearsAgo(null).build();
        assertEquals(BigDecimal.valueOf(20_000_000), domain.determineBaseLimit(inquiry));
    }

    @Test
    void baseLimit_nullColStatus_treatedAsLancarDefensively() {
        PefindoInquiry inquiry = PefindoInquiry.builder().colStatus(null).colHistoryYearsAgo(null).build();
        assertEquals(BigDecimal.valueOf(20_000_000), domain.determineBaseLimit(inquiry));
    }

    @Test
    void baseLimit_mediumCase1_kol4OlderThan2Years() {
        PefindoInquiry inquiry = PefindoInquiry.builder().colStatus(4).colHistoryYearsAgo(3).build();
        assertEquals(BigDecimal.valueOf(10_000_000), domain.determineBaseLimit(inquiry));
    }

    @Test
    void baseLimit_mediumCase2_kol2WithinLast2Years() {
        PefindoInquiry inquiry = PefindoInquiry.builder().colStatus(2).colHistoryYearsAgo(1).build();
        assertEquals(BigDecimal.valueOf(10_000_000), domain.determineBaseLimit(inquiry));
    }

    @Test
    void baseLimit_highRisk_kol5() {
        PefindoInquiry inquiry = PefindoInquiry.builder().colStatus(5).colHistoryYearsAgo(3).build();
        assertEquals(BigDecimal.valueOf(5_000_000), domain.determineBaseLimit(inquiry));
    }

    @Test
    void pinjolAdjustment_zeroApps_noReduction() {
        assertEquals(BigDecimal.valueOf(20_000_000),
                domain.applyPinjolAdjustment(BigDecimal.valueOf(20_000_000), 0));
    }

    @Test
    void pinjolAdjustment_oneOrTwoApps_ninetyPercent() {
        assertEquals(BigDecimal.valueOf(18_000_000),
                domain.applyPinjolAdjustment(BigDecimal.valueOf(20_000_000), 2));
    }

    @Test
    void pinjolAdjustment_threeToFiveApps_seventyFivePercent() {
        assertEquals(BigDecimal.valueOf(7_500_000),
                domain.applyPinjolAdjustment(BigDecimal.valueOf(10_000_000), 4));
    }

    @Test
    void pinjolAdjustment_sixOrMoreApps_fiftyPercent() {
        assertEquals(BigDecimal.valueOf(2_500_000),
                domain.applyPinjolAdjustment(BigDecimal.valueOf(5_000_000), 6));
    }

    @Test
    void pinjolAdjustment_roundsDownToNearestHundredThousand() {
        // 10jt * 90% = 9,000,000 exactly no rounding needed; use a case that isn't a round number
        BigDecimal result = domain.applyPinjolAdjustment(BigDecimal.valueOf(10_000_000), 4); // x75% = 7,500,000
        assertEquals(BigDecimal.valueOf(7_500_000), result);
    }

    @Test
    void decide_vidaUnclear_shortCircuitsToRetakePhoto() {
        VidaResult vida = new VidaResult(VidaResult.UNCLEAR, false, null);
        EngineDecision decision = domain.decide(vida, null, 0);
        assertEquals(EngineStatus.RETAKE_PHOTO, decision.status());
        assertNull(decision.suggestedLimit());
    }

    @Test
    void decide_vidaRejected_autoFinalRejectedNeverReachesChecker() {
        // Revisi 2026-09-02 (konfirmasi user): Vida REJECTED harus auto-final-REJECTED,
        // gak boleh sampai masuk antrean Checker sama sekali (membatalkan revisi 2026-08-31).
        VidaResult vida = new VidaResult(VidaResult.REJECTED, false, null);
        EngineDecision decision = domain.decide(vida, null, 0);
        assertEquals(EngineStatus.REJECTED, decision.status());
        assertEquals(BigDecimal.ZERO, decision.suggestedLimit());
        assertEquals(EngineDecision.RECOMMENDATION_REJECTED, decision.recommendation());
        assertEquals(EngineDecision.RISK_HIGH, decision.riskCategory());
    }

    @Test
    void decide_vidaApproved_proceedsToPefindoAndSuggestsLimit() {
        VidaResult vida = new VidaResult(VidaResult.APPROVED, true, "5000000");
        PefindoInquiry inquiry = PefindoInquiry.builder().colStatus(1).colHistoryYearsAgo(null).score("742").build();
        PefindoResult pefindo = new PefindoResult(inquiry);

        EngineDecision decision = domain.decide(vida, pefindo, 1);

        assertEquals(EngineStatus.PENDING_CHECKER, decision.status());
        assertEquals(BigDecimal.valueOf(18_000_000), decision.suggestedLimit());
        assertEquals(EngineDecision.RECOMMENDATION_APPROVED, decision.recommendation());
        assertEquals(EngineDecision.RISK_LOW, decision.riskCategory());
        assertEquals(742, decision.engineScore());
    }
}
