package com.api.klarfinance.referral.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.api.klarfinance.auth.model.User;

/** One row per invitee who registered using someone else's referral code (see
 * CustomerDetails.referralCode). qualifiedAt stays null until the invitee's own loan reaches the
 * minimum amount (ReferralService.onLoanRequested) - that's what triggers a ReferralReward for
 * the referrer, not the registration itself. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dbh_referral_invite", schema = "referral")
public class ReferralInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referrer_user_id", nullable = false)
    private User referrerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitee_user_id", nullable = false, unique = true)
    private User inviteeUser;

    @Column(name = "code_used")
    private String codeUsed;

    @Column(name = "qualified_at")
    private LocalDateTime qualifiedAt;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
