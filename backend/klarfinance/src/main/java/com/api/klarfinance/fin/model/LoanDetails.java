package com.api.klarfinance.fin.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dbh_loan_details", schema = "fin")
public class LoanDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "requested_amount", precision = 18, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "total_amount_due", precision = 18, scale = 2)
    private BigDecimal totalAmountDue;

    @Column(name = "admin_fee", precision = 18, scale = 2)
    private BigDecimal adminFee;

    @Column(name = "interest_percent", precision = 8, scale = 4)
    private BigDecimal interestPercent;

    @Column(name = "interest_fee", precision = 18, scale = 2)
    private BigDecimal interestFee;

    @Column(name = "penalty_fee", precision = 18, scale = 2)
    private BigDecimal penaltyFee;

    /** Rp100rb referral reward applied to this loan, if any - see ReferralService/LoanService. */
    @Column(name = "referral_discount", precision = 18, scale = 2)
    private BigDecimal referralDiscount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
