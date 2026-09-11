package com.api.klarfinance.los.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Event-driven timeout (bukan scheduled job, sesuai permintaan user) - Redis sendiri yang nge-fire
 * event pas key "los:checker:lock:{applicationId}" (TTL 2 jam, checker udah mulai review) atau
 * "los:checker:offer:{applicationId}" (TTL 5 menit, checker belum sempat buka aplikasi yang
 * ditawarin) expired, listener ini yang nangkep. Diregistrasi ke RedisMessageListenerContainer
 * di RedisKeyExpirationConfig.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CheckerLockExpirationListener implements MessageListener {
    private static final String LOCK_KEY_PREFIX = "los:checker:lock:";
    private static final String OFFER_KEY_PREFIX = "los:checker:offer:";

    private final CheckerAssignmentService checkerAssignmentService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = new String(message.getBody(), StandardCharsets.UTF_8);

        if (expiredKey.startsWith(LOCK_KEY_PREFIX)) {
            parseApplicationId(expiredKey, LOCK_KEY_PREFIX).ifPresent(checkerAssignmentService::handleExpiry);
        } else if (expiredKey.startsWith(OFFER_KEY_PREFIX)) {
            parseApplicationId(expiredKey, OFFER_KEY_PREFIX).ifPresent(checkerAssignmentService::handleOfferExpiry);
        }
    }

    private Optional<Integer> parseApplicationId(String expiredKey, String prefix) {
        try {
            return Optional.of(Integer.valueOf(expiredKey.substring(prefix.length())));
        } catch (NumberFormatException e) {
            log.warn("Gagal parse applicationId dari expired key: {}", expiredKey);
            return Optional.empty();
        }
    }
}
