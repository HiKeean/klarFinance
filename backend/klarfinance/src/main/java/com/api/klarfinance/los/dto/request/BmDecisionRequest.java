package com.api.klarfinance.los.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BmDecisionRequest {
    /** APPROVE atau REJECT */
    private String action;
    /** Wajib kalau action = APPROVE — plafond final. Maks +2.000.000 dari checkerPurposeLimit. */
    private BigDecimal finalLimit;
    /** Wajib kalau finalLimit beda dari checkerPurposeLimit, atau kalau REJECT. */
    private String reason;
}
