package com.api.klarfinance.geo.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LocationPingRequest {
    private Double latitude;
    private Double longitude;
    private Double accuracyMeters;
    /** Optional - server falls back to receipt time (LocationService#submitPing) if the client
     * omits it, so a slightly stale/offline-queued client doesn't need special handling here. */
    private LocalDateTime capturedAt;
}
