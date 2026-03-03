package com.example.oa.user.service;

import com.example.oa.user.dto.UserInfoVo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * 用户信息二级缓存：L1 Caffeine（进程内）、L2 Redis（跨实例）。
 * 读：L1 -> L2 -> loader；写：同时写 L1 与 L2。
 */
@Slf4j
@Component
public class TwoLevelUserInfoCache {

    private static final String L2_KEY_PREFIX = "oa:user:info:";

    private final Cache<Long, UserInfoVo> l1;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration l2Ttl;

    public TwoLevelUserInfoCache(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            @Value("${app.cache.user-info.l1-max-size:500}") int l1MaxSize,
            @Value("${app.cache.user-info.l1-ttl-seconds:300}") int l1TtlSeconds,
            @Value("${app.cache.user-info.l2-ttl-seconds:600}") int l2TtlSeconds) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.l2Ttl = Duration.ofSeconds(l2TtlSeconds > 0 ? l2TtlSeconds : 600);
        this.l1 = Caffeine.newBuilder()
                .maximumSize(l1MaxSize > 0 ? l1MaxSize : 500)
                .expireAfterWrite(l1TtlSeconds > 0 ? l1TtlSeconds : 300, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 先查 L1，再查 L2，都没有则调用 loader 并回填 L1、L2。
     */
    public UserInfoVo get(Long userId, java.util.function.Supplier<UserInfoVo> loader) {
        if (userId == null) return null;
        UserInfoVo vo = l1.getIfPresent(userId);
        if (vo != null) return vo;
        String key = L2_KEY_PREFIX + userId;
        String json = redis.opsForValue().get(key);
        if (json != null) {
            vo = parse(json);
            if (vo != null) {
                l1.put(userId, vo);
                return vo;
            }
        }
        vo = loader.get();
        if (vo != null) put(userId, vo);
        return vo;
    }

    public void put(Long userId, UserInfoVo vo) {
        if (userId == null || vo == null) return;
        l1.put(userId, vo);
        String key = L2_KEY_PREFIX + userId;
        String json = serialize(vo);
        if (json != null) redis.opsForValue().set(key, json, l2Ttl);
    }

    public void evict(Long userId) {
        if (userId == null) return;
        l1.invalidate(userId);
        redis.delete(L2_KEY_PREFIX + userId);
    }

    private String serialize(UserInfoVo vo) {
        try {
            return objectMapper.writeValueAsString(vo);
        } catch (JsonProcessingException e) {
            log.warn("serialize UserInfoVo failed", e);
            return null;
        }
    }

    private UserInfoVo parse(String json) {
        try {
            return objectMapper.readValue(json, UserInfoVo.class);
        } catch (JsonProcessingException e) {
            log.warn("parse UserInfoVo failed: {}", json, e);
            return null;
        }
    }
}
