package com.klarfinance.app.presentation.referral

import com.klarfinance.app.domain.model.ReferralSummary

data class ReferralUiState(
    val isLoading: Boolean = true,
    val summary: ReferralSummary? = null,
    val loadErrorMessage: String? = null,
)
