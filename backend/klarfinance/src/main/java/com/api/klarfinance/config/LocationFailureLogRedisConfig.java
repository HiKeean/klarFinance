package com.api.klarfinance.config;

import io.lettuce.core.RedisClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Separate Redis instance (Upstash) used ONLY for "location not sended" failure telemetry - see
 * LocationFailureLogService. Deliberately NOT reusing the main app.redis.* connection/RedisConfig
 * bean: this is a distinct credential the user provided specifically for this purpose, and a
 * plain Lettuce RedisClient (rather than Spring Data Redis's RedisConnectionFactory/
 * RedisTemplate) avoids any bean ambiguity with the app's primary auto-configured
 * StringRedisTemplate (OTP cache, sessions, etc.).
 *
 * Only the CLIENT is a bean here (cheap - RedisClient.create just parses the URI, no I/O). The
 * actual connection is opened lazily by LocationFailureLogService on first use, NOT eagerly here
 * - an eager `@Bean` StatefulRedisConnection was tried first and broke the ENTIRE app's startup
 * the moment this Redis instance was unreachable (wrong scheme/TLS, network block, etc.), which is
 * unacceptable for what's supposed to be best-effort telemetry, not a critical dependency.
 */
@Configuration
public class LocationFailureLogRedisConfig {

    @Value("${app.location-failure-log.redis-url}")
    private String redisUrl;

    @Bean(destroyMethod = "shutdown")
    public RedisClient locationFailureLogRedisClient() {
        return RedisClient.create(redisUrl);
    }
}
