package com.api.klarfinance.geo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes a "location not sended" entry to the dedicated failure-log Redis (see
 * LocationFailureLogRedisConfig) whenever the nasabah's location couldn't be turned on/captured -
 * called from LocationTrackingService.logFailure, which the Kotlin app hits instead of showing a
 * snackbar for this case (user's explicit call 2026-09-04: no snackbar, log only).
 *
 * Each entry gets its own key (userId + timestamp) with a 1-day TTL, so failures naturally expire
 * without needing a cleanup job. The connection to this Redis is opened LAZILY on first use (not
 * eagerly as a bean - see LocationFailureLogRedisConfig) and reused afterwards. A failure here -
 * connecting OR writing - must NEVER break the caller's actual request (setConsent/
 * permission-denied are not location-log operations themselves), so every path is swallowed and
 * logged as a warning instead of propagated.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationFailureLogService {

    private static final long TTL_SECONDS = Duration.ofDays(1).toSeconds();
    private static final String MESSAGE = "location not sended";

    private final RedisClient locationFailureLogRedisClient;
    private final ObjectMapper objectMapper;

    private volatile StatefulRedisConnection<String, String> connection;

    public void record(Integer userId, String reason) {
        try {
            String key = "location-failure-log:" + userId + ":" + Instant.now().toEpochMilli();

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("datestamp", LocalDateTime.now().toString());
            entry.put("userId", userId);
            entry.put("message", MESSAGE);
            entry.put("reason", reason);

            String value = objectMapper.writeValueAsString(entry);
            connection().sync().setex(key, TTL_SECONDS, value);
        } catch (Exception e) {
            log.warn("Failed to write location failure log for user {}: {}", userId, e.getMessage());
        }
    }

    private synchronized StatefulRedisConnection<String, String> connection() {
        if (connection == null || !connection.isOpen()) {
            connection = locationFailureLogRedisClient.connect(StringCodec.UTF8);
        }
        return connection;
    }
}
