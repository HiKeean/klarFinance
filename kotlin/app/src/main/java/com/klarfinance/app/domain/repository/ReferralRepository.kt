package com.klarfinance.app.domain.repository

import com.klarfinance.app.domain.model.ReferralSummary

interface ReferralRepository {
    /** GET api/v1/nasabah/referral/summary - own referral code plus invite/reward counts. */
    suspend fun getSummary(): Result<ReferralSummary>
}
