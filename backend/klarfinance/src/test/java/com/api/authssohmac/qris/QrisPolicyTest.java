package com.api.authssohmac.qris;

import org.junit.jupiter.api.Test;

import com.api.klarfinance.qris.QrisPolicy;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QrisPolicyTest {

    @Test
    void quotaIsCappedAtOneMillionEvenForLargePlafond() {
        // Plafond 20jt -> 30% = 6jt, tapi kuota QRIS SELALU dipotong ke Rp1jt (konfirmasi user).
        assertEquals(new BigDecimal("1000000.00"), QrisPolicy.quota(new BigDecimal("20000000")));
    }

    @Test
    void quotaFollowsThirtyPercentWhenBelowTheCap() {
        // Plafond 2jt -> 30% = 600rb, masih di bawah cap 1jt jadi kuota-nya 600rb, bukan 1jt.
        assertEquals(new BigDecimal("600000.00"), QrisPolicy.quota(new BigDecimal("2000000")));
    }

    @Test
    void eligibilityThresholdIsExactlyTwoMillionInclusive() {
        assertTrue(QrisPolicy.isEligible(new BigDecimal("2000000")));
        assertFalse(QrisPolicy.isEligible(new BigDecimal("1999999")));
    }
}
