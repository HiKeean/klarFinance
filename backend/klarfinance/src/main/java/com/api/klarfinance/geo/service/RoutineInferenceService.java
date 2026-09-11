package com.api.klarfinance.geo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.api.klarfinance.geo.dto.response.RoutineLocationResponse;
import com.api.klarfinance.geo.dto.response.RoutineSummaryResponse;
import com.api.klarfinance.geo.model.LocationPing;
import com.api.klarfinance.geo.repository.LocationPingRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Turns a nasabah's raw location ping history into a rough "where do they actually live / where
 * are they on a normal day" summary for Checker/BM review - read-only, computed on demand
 * (nothing here is persisted), see LocationController#routine.
 *
 * <p>Deliberately simple for what this is: no real geocoding (coordinates only, no street
 * address - that needs a geocoding API/key this project doesn't have), no proper clustering
 * algorithm (DBSCAN etc) - just "round each ping to a ~110m grid cell, the cell with the most
 * hits wins". Good enough to demo the concept and to flag an obvious mismatch (e.g. inferred
 * home is nowhere near the KTP-registered village), not a production-grade geolocation engine.
 */
@Service
@RequiredArgsConstructor
public class RoutineInferenceService {

    private static final int LOOKBACK_DAYS = 5;
    /** Minimum repeat visits to the same grid cell before it's called a "pattern" rather than
     * one-off noise - below this, dominantCluster() returns null (not enough data yet). */
    private static final int MIN_SAMPLES_FOR_PATTERN = 2;
    /** Rounds lat/lng to 3 decimal places (~111m at the equator) - close enough to treat as
     * "the same place" (same building/block) without being so coarse that a whole
     * neighborhood collapses into one cell. */
    private static final double GRID_PRECISION = 1000.0;

    private final LocationPingRepository pingRepository;

    public RoutineSummaryResponse inferRoutine(Integer userId) {
        LocalDateTime since = LocalDateTime.now().minusDays(LOOKBACK_DAYS);
        List<LocationPing> pings = pingRepository.findByUserIdAndCapturedAtAfterOrderByCapturedAtAsc(userId, since);

        List<LocationPing> nightPings = pings.stream().filter(p -> isNight(p.getCapturedAt())).toList();
        List<LocationPing> daytimePings = pings.stream().filter(p -> !isNight(p.getCapturedAt())).toList();

        long daysCovered = pings.stream().map(p -> p.getCapturedAt().toLocalDate()).distinct().count();

        return RoutineSummaryResponse.builder()
                .totalPingsAnalyzed(pings.size())
                .daysCovered((int) daysCovered)
                .homeLocation(dominantCluster(nightPings))
                .daytimeLocation(dominantCluster(daytimePings))
                .build();
    }

    /** 18:00-05:00 counts as "night" (matches the "pagi/siang/malam" 3x/day capture cadence -
     * the night capture is what's most likely to catch someone at their actual residence). */
    private boolean isNight(LocalDateTime capturedAt) {
        int hour = capturedAt.getHour();
        return hour >= 18 || hour < 5;
    }

    private RoutineLocationResponse dominantCluster(List<LocationPing> pings) {
        if (pings.isEmpty()) return null;

        Map<String, List<LocationPing>> clusters = pings.stream()
                .collect(Collectors.groupingBy(p -> gridKey(p.getLatitude(), p.getLongitude())));

        List<LocationPing> largest = clusters.values().stream()
                .max(Comparator.comparingInt(List::size))
                .orElse(List.of());

        if (largest.size() < MIN_SAMPLES_FOR_PATTERN) return null;

        double avgLat = largest.stream().mapToDouble(LocationPing::getLatitude).average().orElse(0);
        double avgLng = largest.stream().mapToDouble(LocationPing::getLongitude).average().orElse(0);

        return RoutineLocationResponse.builder()
                .latitude(avgLat)
                .longitude(avgLng)
                .sampleCount(largest.size())
                .confidence((double) largest.size() / pings.size())
                .build();
    }

    private String gridKey(double lat, double lng) {
        return Math.round(lat * GRID_PRECISION) + ":" + Math.round(lng * GRID_PRECISION);
    }
}
