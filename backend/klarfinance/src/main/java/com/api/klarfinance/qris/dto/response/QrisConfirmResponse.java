package com.api.klarfinance.qris.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class QrisConfirmResponse {
    private Integer loanId;
    private String merchantName;
    private BigDecimal requestedAmount;
    private BigDecimal totalAmountDue;
    private BigDecimal installmentAmount;
    private LocalDateTime dueDate;
}
