package com.klarfinance.app.data.repository

import com.klarfinance.app.core.network.ApiService
import com.klarfinance.app.data.dto.ReferralSummaryResponseDto
import com.klarfinance.app.domain.model.ReferralSummary
import com.klarfinance.app.domain.repository.ReferralRepository
import javax.inject.Inject

class ReferralRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
) : ReferralRepository {

    override suspend fun getSummary(): Result<ReferralSummary> = runCatching {
        val response = apiService.get<ReferralSummaryResponseDto>("api/v1/nasabah/referral/summary")
        val data = response.data ?: throw IllegalStateException("Unexpected response from server")
        ReferralSummary(
            referralCode = data.referralCode.orEmpty(),
            minimumLoanAmount = (data.minimumLoanAmount ?: 0.0).toLong(),
            rewardAmount = (data.rewardAmount ?: 0.0).toLong(),
            totalInvited = data.totalInvited ?: 0L,
            totalQualified = data.totalQualified ?: 0L,
            availableRewardsCount = data.availableRewardsCount ?: 0L,
            availableDiscountTotal = (data.availableDiscountTotal ?: 0.0).toLong(),
        )
    }
}
