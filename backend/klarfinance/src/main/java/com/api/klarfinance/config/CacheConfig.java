package com.api.klarfinance.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.BatchStrategies;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Configuration
public class CacheConfig implements CachingConfigurer, DisposableBean {

    public static final String PROVINCES = "provinces";
    public static final String REGENCIES = "regencies";
    public static final String DISTRICTS = "districts";
    public static final String VILLAGES = "villages";
    public static final String API_KEYS = "api-keys";
    public static final String ROLE_MENUS = "role-menus";

    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(2);

    private LettuceConnectionFactory cacheConnectionFactory;

    @Bean
    public CacheManager cacheManager(@Value("${app.cache.redis-url}") String redisUrl,
                                     RedisCacheConfiguration defaults) {
        URI uri = URI.create(redisUrl);
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(uri.getHost(), uri.getPort() == -1 ? 6379 : uri.getPort());
        if (uri.getUserInfo() != null) {
            String[] credentials = uri.getUserInfo().split(":", 2);
            standalone.setUsername(credentials[0]);
            if (credentials.length > 1) {
                standalone.setPassword(RedisPassword.of(credentials[1]));
            }
        }

        LettuceClientConfiguration.LettuceClientConfigurationBuilder client = LettuceClientConfiguration.builder()
                .commandTimeout(COMMAND_TIMEOUT);
        if ("rediss".equalsIgnoreCase(uri.getScheme())) {
            client.useSsl();
        }

        cacheConnectionFactory = new LettuceConnectionFactory(standalone, client.build());
        cacheConnectionFactory.afterPropertiesSet();

        Duration oneDay = Duration.ofHours(24);
        Map<String, RedisCacheConfiguration> perCache = Map.of(
                PROVINCES, defaults.entryTtl(oneDay),
                REGENCIES, defaults.entryTtl(oneDay),
                DISTRICTS, defaults.entryTtl(oneDay),
                VILLAGES, defaults.entryTtl(oneDay),
                API_KEYS, defaults.entryTtl(Duration.ofMinutes(10))
        );

        RedisCacheWriter writer = RedisCacheWriter.nonLockingRedisCacheWriter(cacheConnectionFactory, BatchStrategies.scan(100));
        return RedisCacheManager.builder(writer)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(perCache)
                .transactionAware()
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache GET failed [{}] key={}: {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("Cache PUT failed [{}] key={}: {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache EVICT failed [{}] key={}: {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("Cache CLEAR failed [{}]: {}", cache.getName(), e.getMessage());
            }
        };
    }

    @Override
    public void destroy() {
        if (cacheConnectionFactory != null) {
            cacheConnectionFactory.destroy();
        }
    }
}
