package com.api.klarfinance.fin.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class LoanResponse {
    private Integer loanId;
    private BigDecimal requestedAmount;
    /** Nominal yang benar-benar cair ke rekening nasabah, setelah dipotong biaya admin (lihat
     * LoanInterestPolicy.calculateAdminFee) - null untuk pencairan QRIS/merchant. */
    private BigDecimal disbursedAmount;
    private BigDecimal adminFee;
    private BigDecimal totalAmountDue;
    private Integer tenorMonths;
    private BigDecimal installmentAmount;
    private LocalDateTime firstDueDate;
    private BigDecimal referralDiscountApplied;

    /** true kalau pengajuan ini melewati 30% dari plafond (LoanInterestPolicy.exceedsReviewThreshold)
     * - belum jadi Loan sama sekali, ditahan sebagai LoanReviewRequest sampai BM memutuskan. Field
     * di atas (loanId/totalAmountDue/dst) semuanya null kalau ini true. */
    private boolean reviewRequired;
    private Integer reviewRequestId;
    private String message;
}
