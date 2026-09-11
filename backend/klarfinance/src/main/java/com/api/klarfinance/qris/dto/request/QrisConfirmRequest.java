package com.api.klarfinance.qris.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class QrisConfirmRequest {
    private BigDecimal amount;
}
