package com.api.klarfinance.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import com.api.klarfinance.los.service.CheckerLockExpirationListener;

/**
 * Nyalain Redis keyspace notification (event "expired") biar CheckerLockExpirationListener bisa
 * react begitu lock 2-jam Checker habis, tanpa perlu scheduled job (sesuai permintaan user).
 * `notify-keyspace-events` di-set programatik di sini - gak perlu ubah redis.conf server.
 */
@Configuration
@RequiredArgsConstructor
public class RedisKeyExpirationConfig {
    private final RedisConnectionFactory redisConnectionFactory;

    @PostConstruct
    public void enableKeyspaceNotifications() {
        redisConnectionFactory.getConnection().serverCommands().setConfig("notify-keyspace-events", "Ex");
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(CheckerLockExpirationListener listener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(listener, new PatternTopic("__keyevent@*__:expired"));
        return container;
    }
}
