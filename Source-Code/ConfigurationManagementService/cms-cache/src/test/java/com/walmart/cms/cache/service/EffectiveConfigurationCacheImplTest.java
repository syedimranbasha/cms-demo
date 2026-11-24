package com.walmart.cms.cache.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmart.cms.cache.exception.CacheAccessException;
import com.walmart.cms.cache.exception.CacheSerializationException;
import com.walmart.cms.cache.model.ConfigurationVersionMetadata;
import com.walmart.cms.cache.model.EffectiveConfiguration;
import com.walmart.cms.cache.model.EffectiveConfigurationCacheKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EffectiveConfigurationCacheImplTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private EffectiveConfigurationCacheImpl cache;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cache = new EffectiveConfigurationCacheImpl(redisTemplate, objectMapper);
    }

    @Test
    void getReturnsEmptyWhenRedisKeyDoesNotExist() {
        when(valueOperations.get(anyString())).thenReturn(null);

        assertEquals(Optional.empty(), cache.get(sampleKey()));
    }

    @Test
    void getDeserializesCachedConfiguration() throws Exception {
        EffectiveConfiguration expected = sampleValue();
        String json = objectMapper.writeValueAsString(expected);
        when(valueOperations.get(anyString())).thenReturn(json);

        Optional<EffectiveConfiguration> result = cache.get(sampleKey());

        assertTrue(result.isPresent());
        assertEquals(expected.effectiveConfig(), result.get().effectiveConfig());
        assertEquals(expected.versions(), result.get().versions());
    }

    @Test
    void getDeletesCorruptEntryAndThrowsSerializationException() {
        when(valueOperations.get(anyString())).thenReturn("{invalid-json");

        assertThrows(CacheSerializationException.class, () -> cache.get(sampleKey()));
        verify(redisTemplate).delete(anyString());
    }

    @Test
    void getWrapsRedisFailure() {
        when(valueOperations.get(anyString())).thenThrow(new IllegalStateException("Redis unavailable"));

        assertThrows(CacheAccessException.class, () -> cache.get(sampleKey()));
    }

    @Test
    void putSerializesValueAndSetsTtl() {
        EffectiveConfiguration value = sampleValue();
        Duration ttl = Duration.ofMinutes(3);

        cache.put(sampleKey(), value, ttl);

        verify(valueOperations).set(anyString(), contains("\"effectiveConfig\""), eq(ttl));
    }

    @Test
    void putRejectsNullZeroAndNegativeTtl() {
        assertThrows(IllegalArgumentException.class, () -> cache.put(sampleKey(), sampleValue(), null));
        assertThrows(IllegalArgumentException.class, () -> cache.put(sampleKey(), sampleValue(), Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> cache.put(sampleKey(), sampleValue(), Duration.ofSeconds(-1)));
    }

    @Test
    void putWrapsRedisFailure() {
        doThrow(new IllegalStateException("Redis unavailable"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        assertThrows(CacheAccessException.class,
                () -> cache.put(sampleKey(), sampleValue(), Duration.ofMinutes(1)));
    }

    @Test
    void evictReturnsTrueWhenRedisDeletesKey() {
        when(redisTemplate.delete(anyString())).thenReturn(true);

        assertTrue(cache.evict(sampleKey()));
    }

    @Test
    void evictReturnsFalseWhenRedisDoesNotDeleteKey() {
        when(redisTemplate.delete(anyString())).thenReturn(false);

        assertFalse(cache.evict(sampleKey()));
    }

    @Test
    void evictWrapsRedisFailure() {
        when(redisTemplate.delete(anyString())).thenThrow(new IllegalStateException("Redis unavailable"));

        assertThrows(CacheAccessException.class, () -> cache.evict(sampleKey()));
    }

    @Test
    void existsReturnsRedisResult() {
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        assertTrue(cache.exists(sampleKey()));

        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        assertFalse(cache.exists(sampleKey()));
    }

    @Test
    void existsWrapsRedisFailure() {
        when(redisTemplate.hasKey(anyString())).thenThrow(new IllegalStateException("Redis unavailable"));

        assertThrows(CacheAccessException.class, () -> cache.exists(sampleKey()));
    }

    private static EffectiveConfigurationCacheKey sampleKey() {
        return new EffectiveConfigurationCacheKey(
                "org-1", "app-1", "PROD", "us-central1", "baseline", "fingerprint123");
    }

    private static EffectiveConfiguration sampleValue() {
        return new EffectiveConfiguration(
                Map.of("logging", Map.of("level", "INFO"), "featureEnabled", true),
                new ConfigurationVersionMetadata("g-v1", "e-v2", "a-v3", "r-v4"),
                Instant.parse("2026-01-01T00:00:00Z"));
    }
}
