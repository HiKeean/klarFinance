package com.api.klarfinance.geo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.klarfinance.auth.model.User;
import com.api.klarfinance.auth.repository.UserRepository;
import com.api.klarfinance.geo.dto.request.LocationPingRequest;
import com.api.klarfinance.geo.model.LocationConsent;
import com.api.klarfinance.geo.model.LocationPing;
import com.api.klarfinance.geo.repository.LocationConsentRepository;
import com.api.klarfinance.geo.repository.LocationPingRepository;
import com.api.klarfinance.global.AppConstant;

import java.security.Principal;
import java.time.LocalDateTime;

/**
 * Nasabah-facing half of the location-routine feature - opt-in/opt-out and submitting raw
 * samples. Deliberately separate from RoutineInferenceService (the read/analysis side) - this
 * class only ever writes, and every write here is consent-gated, so a stray client call (bug,
 * replay, tampered request) can never persist a ping without an active, explicit opt-in on
 * record - see LocationConsent.
 */
@Service
@RequiredArgsConstructor
public class LocationTrackingService {

    private final UserRepository userRepository;
    private final LocationConsentRepository consentRepository;
    private final LocationPingRepository pingRepository;
    private final LocationFailureLogService failureLogService;

    public boolean getConsent(Principal principal) {
        User user = currentNasabah(principal);
        return hasConsent(user.getId());
    }

    @Transactional
    public void setConsent(Principal principal, boolean consent) {
        User user = currentNasabah(principal);
        LocationConsent entity = consentRepository.findByUserId(user.getId())
                .orElseGet(() -> LocationConsent.builder().user(user).build());
        entity.setConsentGiven(consent);
        if (consent) {
            entity.setConsentAt(LocalDateTime.now());
        } else {
            entity.setRevokedAt(LocalDateTime.now());
        }
        consentRepository.save(entity);
    }

    @Transactional
    public void submitPing(Principal principal, LocationPingRequest request) {
        User user = currentNasabah(principal);
        if (!hasConsent(user.getId())) {
            throw new IllegalStateException("Location consent has not been granted");
        }
        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new IllegalArgumentException("latitude and longitude are required");
        }

        pingRepository.save(LocationPing.builder()
                .user(user)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .accuracyMeters(request.getAccuracyMeters())
                .capturedAt(request.getCapturedAt() != null ? request.getCapturedAt() : LocalDateTime.now())
                .build());
    }

    /** No DB write, no validation beyond auth - a client reporting "I couldn't send location"
     * should never itself fail because of an unrelated business rule (e.g. consent not on
     * record yet). See LocationFailureLogService. */
    public void logFailure(Principal principal, String reason) {
        User user = currentNasabah(principal);
        failureLogService.record(user.getId(), reason);
    }

    private boolean hasConsent(Integer userId) {
        return consentRepository.findByUserId(userId)
                .map(LocationConsent::isConsentGiven)
                .orElse(false);
    }

    private User currentNasabah(Principal principal) {
        if (principal == null) throw new IllegalStateException("Authentication required");
        User user = userRepository.findByIdentity(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        if (user.getRole() == null || !AppConstant.ROLE_NASABAH.equalsIgnoreCase(user.getRole().getName())) {
            throw new IllegalStateException("Only Nasabah can use location tracking");
        }
        return user;
    }
}
