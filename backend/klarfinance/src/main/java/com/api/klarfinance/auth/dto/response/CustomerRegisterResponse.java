package com.api.klarfinance.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerRegisterResponse {
    private String identity;
    private String applicationStatus;
    private BigDecimal suggestedLimit;
    private String message;
}
