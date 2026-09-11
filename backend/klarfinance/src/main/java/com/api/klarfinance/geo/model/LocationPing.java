package com.api.klarfinance.geo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.api.klarfinance.auth.model.User;

/** A single consented location sample from the nasabah's device - see LocationCaptureWorker
 * (Android) which submits ~3 of these a day when the nasabah has opted in. Raw samples only;
 * RoutineInferenceService is what turns a history of these into a "likely home"/"likely
 * daytime spot" summary, computed on read rather than persisted. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dbh_location_pings", schema = "geo")
public class LocationPing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "accuracy_meters")
    private Double accuracyMeters;

    /** When the device actually captured this fix - used for the time-of-day bucketing
     * (morning/afternoon/night) in RoutineInferenceService, not [createdAt]. */
    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
