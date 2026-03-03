package com.example.oa.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 登录 Token 存储：Redis，支持多实例共享。
 * Key: oa:auth:token:{token}, Value: userId, TTL: 24h（可配置）。
 */
@Component
public class AuthTokenStore {

    private static final String DEFAULT_PREFIX = "oa:auth:token:";
    private static final int DEFAULT_TTL_HOURS = 24;

    private final StringRedisTemplate redis;
    private final String prefix;
    private final Duration ttl;

    public AuthTokenStore(
            StringRedisTemplate redis,
            @Value("${app.auth.token-prefix:oa:auth:token:}") String prefix,
            @Value("${app.auth.token-ttl-hours:24}") int ttlHours) {
        this.redis = redis;
        this.prefix = prefix != null && !prefix.isBlank() ? prefix : DEFAULT_PREFIX;
        this.ttl = Duration.ofHours(ttlHours > 0 ? ttlHours : DEFAULT_TTL_HOURS);
    }

    public void put(String token, Long userId) {
        if (token == null || userId == null) return;
        redis.opsForValue().set(prefix + token.trim(), String.valueOf(userId), ttl);
    }

    public Long get(String token) {
        if (token == null || token.isBlank()) return null;
        String v = redis.opsForValue().get(prefix + token.trim());
        if (v == null) return null;
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void remove(String token) {
        if (token != null && !token.isBlank()) {
            redis.delete(prefix + token.trim());
        }
    }
}
