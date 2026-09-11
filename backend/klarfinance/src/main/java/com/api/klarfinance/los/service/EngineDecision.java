package com.api.klarfinance.los.service;

import java.math.BigDecimal;
import java.util.List;

public record EngineDecision(
        String status,
        BigDecimal suggestedLimit,
        String recommendation,
        String riskCategory,
        List<String> keyFactors,
        Integer engineScore) {

    public static final String RECOMMENDATION_APPROVED = "APPROVED";
    public static final String RECOMMENDATION_REJECTED = "REJECTED";

    public static final String RISK_LOW = "LOW";
    public static final String RISK_MEDIUM = "MEDIUM";
    public static final String RISK_HIGH = "HIGH";
}
