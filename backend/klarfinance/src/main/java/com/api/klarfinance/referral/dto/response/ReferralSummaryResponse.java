package com.api.klarfinance.referral.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ReferralSummaryResponse {
    private String referralCode;
    private BigDecimal minimumLoanAmount;
    private BigDecimal rewardAmount;
    private long totalInvited;
    private long totalQualified;
    private long availableRewardsCount;
    private BigDecimal availableDiscountTotal;
}
