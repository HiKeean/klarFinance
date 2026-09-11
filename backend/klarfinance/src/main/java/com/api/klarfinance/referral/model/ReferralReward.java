package com.api.klarfinance.referral.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.api.klarfinance.auth.model.User;
import com.api.klarfinance.fin.model.Loan;

/** Rp100.000 discount owed to a referrer once their invitee qualifies (see ReferralInvite). Sits
 * AVAILABLE until the referrer requests their own next loan, at which point LoanService applies
 * it (status -> APPLIED, appliedLoan set) - see LoanService.requestLoan. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dbh_referral_reward", schema = "referral")
public class ReferralReward {

    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_APPLIED = "APPLIED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referrer_user_id", nullable = false)
    private User referrerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referral_invite_id", nullable = false)
    private ReferralInvite referralInvite;

    @Column(name = "discount_amount", precision = 18, scale = 2)
    private BigDecimal discountAmount;

    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applied_loan_id")
    private Loan appliedLoan;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
