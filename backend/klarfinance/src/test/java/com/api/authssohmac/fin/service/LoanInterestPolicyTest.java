package com.api.authssohmac.fin.service;

import org.junit.jupiter.api.Test;

import com.api.klarfinance.fin.NplSeverity;
import com.api.klarfinance.fin.service.LoanInterestPolicy;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoanInterestPolicyTest {

    @Test
    void latePenaltyIsFlatTwoPercentPerDayOfOverdueInstallment() {
        // installment 1.000.000, 3 hari overdue -> 1.000.000 * 2% * 3 = 60.000
        assertEquals(new BigDecimal("60000.00"),
                LoanInterestPolicy.calculateLatePenalty(new BigDecimal("1000000"), 3));

        // day 1 overdue (no grace period)
        assertEquals(new BigDecimal("20000.00"),
                LoanInterestPolicy.calculateLatePenalty(new BigDecimal("1000000"), 1));

        // not overdue yet -> 0
        assertEquals(BigDecimal.ZERO, LoanInterestPolicy.calculateLatePenalty(new BigDecimal("1000000"), 0));
    }

    @Test
    void nplSeverityThresholds() {
        assertEquals(NplSeverity.GREEN, NplSeverity.classify(new BigDecimal("0")));
        assertEquals(NplSeverity.GREEN, NplSeverity.classify(new BigDecimal("1.9")));
        assertEquals(NplSeverity.YELLOW, NplSeverity.classify(new BigDecimal("2")));
        assertEquals(NplSeverity.YELLOW, NplSeverity.classify(new BigDecimal("5")));
        assertEquals(NplSeverity.RED, NplSeverity.classify(new BigDecimal("5.1")));
        assertEquals(NplSeverity.RED, NplSeverity.classify(new BigDecimal("100")));
    }

    @Test
    void loanReviewThresholdMatchesUserExample() {
        // Contoh resmi user: plafond 20jt, sudah pinjam 5.5jt, mau pinjam 1jt lagi ->
        // (5.500.000 + 1.000.000) / 20.000.000 = 32.5% -> di atas 30%, wajib review BM.
        BigDecimal usedLimit = new BigDecimal("5500000");
        BigDecimal amount = new BigDecimal("1000000");
        BigDecimal totalLimit = new BigDecimal("20000000");

        assertEquals(new BigDecimal("32.50"),
                LoanInterestPolicy.utilizationPercentAfter(usedLimit, amount, totalLimit));
        assertTrue(LoanInterestPolicy.exceedsReviewThreshold(usedLimit, amount, totalLimit));
    }

    @Test
    void loanReviewThresholdAtExactlyThirtyPercentDoesNotTrigger() {
        // Tepat 30% (bukan "melebihi") -> masih boleh langsung cair, cuma di ATAS 30% yang wajib review.
        BigDecimal usedLimit = new BigDecimal("2000000");
        BigDecimal amount = new BigDecimal("4000000");
        BigDecimal totalLimit = new BigDecimal("20000000");

        assertEquals(new BigDecimal("30.00"),
                LoanInterestPolicy.utilizationPercentAfter(usedLimit, amount, totalLimit));
        assertFalse(LoanInterestPolicy.exceedsReviewThreshold(usedLimit, amount, totalLimit));
    }
}
