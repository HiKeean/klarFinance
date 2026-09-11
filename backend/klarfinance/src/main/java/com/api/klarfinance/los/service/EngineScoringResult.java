package com.api.klarfinance.los.service;

import java.math.BigDecimal;

public record EngineScoringResult(String status, BigDecimal suggestedLimit) {
}
