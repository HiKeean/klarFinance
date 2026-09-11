package com.api.klarfinance.los.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CheckerDecisionRequest {
    /** APPROVE atau REJECT */
    private String action;
    /** Wajib kalau action = APPROVE — plafond hasil analisa Checker. */
    private BigDecimal purposeLimit;
    private String reason;
}
