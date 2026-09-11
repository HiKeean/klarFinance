package com.api.klarfinance.geo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.api.klarfinance.auth.model.User;

/** One row per nasabah - whether they've opted into periodic location capture for credit-risk
 * routine analysis (see RoutineInferenceService). Consent-gated at the service layer
 * (LocationService#submitPing) so a stray/replayed client call can never persist a ping without
 * an active opt-in, even if the Android-side toggle state and this row somehow drift apart. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dbh_location_consent", schema = "geo")
public class LocationConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "consent_given", nullable = false)
    private boolean consentGiven;

    @Column(name = "consent_at")
    private LocalDateTime consentAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = LocalDateTime.now();
    }
}
