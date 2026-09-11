package com.api.klarfinance.los.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import com.api.klarfinance.auth.model.User;

import static jakarta.persistence.GenerationType.IDENTITY;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dbh_vida", schema = "los")
public class Vida {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "kyc_status")
    private String kycStatus;

    @Column(name = "kyc_face_score")
    private String kycFaceScore;

    @Column(name = "kyc_vendor_trx_id")
    private String kycVendorTrxId;

    @Column(name = "income_verification_status")
    private String incomeVerificationStatus;

    @Column(name = "income_verification_source")
    private String incomeVerificationSource;

    @Column(name = "income_is_employed")
    private String incomeIsEmployed;

    @Column(name = "income_verified_monthly")
    private String incomeVerifiedMonthly;

    @Column(name = "raw_response")
    private String raw_response;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
