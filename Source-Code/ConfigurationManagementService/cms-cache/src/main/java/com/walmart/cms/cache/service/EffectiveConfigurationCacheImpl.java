package com.walmart.cms.cache.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmart.cms.cache.service.EffectiveConfigurationCache;
import com.walmart.cms.cache.exception.CacheAccessException;
import com.walmart.cms.cache.exception.CacheSerializationException;
import com.walmart.cms.cache.model.EffectiveConfiguration;
import com.walmart.cms.cache.model.EffectiveConfigurationCacheKey;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.Cursor;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-backed implementation. Values are JSON, not Java native serialization,
 * making the cache payload inspectable and independent of Java class serialization.
 */
@Repository
public class EffectiveConfigurationCacheImpl implements EffectiveConfigurationCache {

    private static final String KEY_PREFIX = "cms:effective-config:v1:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public EffectiveConfigurationCacheImpl(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<EffectiveConfiguration> get(EffectiveConfigurationCacheKey key) {
        String redisKey = toRedisKey(key);
        try {
            String json = redisTemplate.opsForValue().get(redisKey);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, EffectiveConfiguration.class));
        } catch (JsonProcessingException e) {
            // A corrupt cache entry is treated as a miss after removing it.
            safeDelete(redisKey);
            throw new CacheSerializationException("Unable to deserialize CMS cache entry", e);
        } catch (RuntimeException e) {
            if (e instanceof CacheSerializationException) {
                throw e;
            }
            throw new CacheAccessException("Redis GET failed for CMS effective configuration", e);
        }
    }

    @Override
    public void put(EffectiveConfigurationCacheKey key,
                    EffectiveConfiguration value,
                    Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Cache TTL must be greater than zero");
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(toRedisKey(key), json, ttl);
        } catch (JsonProcessingException e) {
            throw new CacheSerializationException("Unable to serialize CMS cache entry", e);
        } catch (RuntimeException e) {
            throw new CacheAccessException("Redis SET failed for CMS effective configuration", e);
        }
    }

    @Override
    public boolean evict(EffectiveConfigurationCacheKey key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.delete(toRedisKey(key)));
        } catch (RuntimeException e) {
            throw new CacheAccessException("Redis DELETE failed for CMS effective configuration", e);
        }
    }


    @Override
    public long evictScope(String organizationId, String applicationId, String environment, String region) {
        String pattern = KEY_PREFIX + normalize(organizationId) + ":"
                + segment(applicationId) + ":" + segment(environment) + ":"
                + segment(region) + ":*:*";
        try {
            Long deleted = redisTemplate.execute((RedisCallback<Long>) connection -> {
                long count = 0;
                try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(500).build())) {
                    while (cursor.hasNext()) {
                        byte[] key = cursor.next();
                        if (connection.del(key) > 0) count++;
                    }
                }
                return count;
            });
            return deleted == null ? 0L : deleted;
        } catch (RuntimeException e) {
            throw new CacheAccessException("Redis scope invalidation failed for CMS effective configuration", e);
        }
    }

    private String segment(String value) {
        return value == null ? "*" : normalize(value);
    }

    @Override
    public boolean exists(EffectiveConfigurationCacheKey key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(toRedisKey(key)));
        } catch (RuntimeException e) {
            throw new CacheAccessException("Redis EXISTS failed for CMS effective configuration", e);
        }
    }

    private void safeDelete(String redisKey) {
        try {
            redisTemplate.delete(redisKey);
        } catch (RuntimeException ignored) {
            // Preserve the original deserialization failure.
        }
    }

    private String toRedisKey(EffectiveConfigurationCacheKey key) {
        return KEY_PREFIX
                + key.organizationId() + ":"
                + key.applicationId() + ":"
                + normalize(key.environment()) + ":"
                + normalize(key.region()) + ":"
                + normalize(key.rolloutTrack()) + ":"
                + key.versionSetFingerprint();
    }

    private String normalize(String value) {
        return value.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "_");
    }
}
