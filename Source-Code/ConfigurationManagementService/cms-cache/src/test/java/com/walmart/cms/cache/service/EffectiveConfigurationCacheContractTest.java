package com.walmart.cms.cache.service;

import com.walmart.cms.cache.model.EffectiveConfiguration;
import com.walmart.cms.cache.model.EffectiveConfigurationCacheKey;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the public cache contract independently of the Redis adapter.
 * The Redis adapter's behavior is covered by EffectiveConfigurationCacheImplTest.
 */
class EffectiveConfigurationCacheContractTest {

    @Test
    void contractSupportsPutGetExistsAndEvict() {
        EffectiveConfigurationCache cache = new InMemoryCache();
        EffectiveConfigurationCacheKey key = new EffectiveConfigurationCacheKey(
                "org", "app", "PROD", "region-1", "baseline", "fingerprint");
        EffectiveConfiguration value = new EffectiveConfiguration(
                Map.of("feature", true), nullSafeVersions(), null);

        assertFalse(cache.exists(key));
        assertTrue(cache.get(key).isEmpty());

        cache.put(key, value, Duration.ofMinutes(1));

        assertTrue(cache.exists(key));
        assertEquals(value.effectiveConfig(), cache.get(key).orElseThrow().effectiveConfig());

        assertTrue(cache.evict(key));
        assertFalse(cache.exists(key));
        assertTrue(cache.get(key).isEmpty());
    }

    private static com.walmart.cms.cache.model.ConfigurationVersionMetadata nullSafeVersions() {
        return new com.walmart.cms.cache.model.ConfigurationVersionMetadata("g1", "e1", "a1", "r1");
    }

    private static final class InMemoryCache implements EffectiveConfigurationCache {
        private final Map<EffectiveConfigurationCacheKey, EffectiveConfiguration> values = new HashMap<>();

        @Override
        public Optional<EffectiveConfiguration> get(EffectiveConfigurationCacheKey key) {
            return Optional.ofNullable(values.get(key));
        }

        @Override
        public void put(EffectiveConfigurationCacheKey key, EffectiveConfiguration value, Duration ttl) {
            values.put(key, value);
        }

        @Override
        public boolean evict(EffectiveConfigurationCacheKey key) {
            return values.remove(key) != null;
        }

        @Override
        public long evictScope(String organizationId, String applicationId, String environment, String region) {
            int before = values.size();
            values.keySet().removeIf(k -> k.organizationId().equals(organizationId)
                    && (applicationId == null || k.applicationId().equals(applicationId))
                    && (environment == null || k.environment().equalsIgnoreCase(environment))
                    && (region == null || k.region().equalsIgnoreCase(region)));
            return before - values.size();
        }

        @Override
        public boolean exists(EffectiveConfigurationCacheKey key) {
            return values.containsKey(key);
        }
    }
}
