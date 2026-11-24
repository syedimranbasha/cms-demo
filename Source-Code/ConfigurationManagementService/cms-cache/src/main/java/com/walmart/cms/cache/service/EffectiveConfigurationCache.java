package com.walmart.cms.cache.service;

import com.walmart.cms.cache.model.EffectiveConfiguration;
import com.walmart.cms.cache.model.EffectiveConfigurationCacheKey;

import java.time.Duration;
import java.util.Optional;

/**
 * Cache port consumed by cms-api. API code depends on this interface,
 * not on RedisTemplate or any Redis implementation class.
 */
public interface EffectiveConfigurationCache {

    Optional<EffectiveConfiguration> get(EffectiveConfigurationCacheKey key);

    void put(EffectiveConfigurationCacheKey key,
             EffectiveConfiguration value,
             Duration ttl);

    boolean evict(EffectiveConfigurationCacheKey key);

    /** Evicts all cached rollout tracks/version fingerprints affected by a scope update.
     * Null dimensions act as wildcards according to the scope hierarchy. */
    long evictScope(String organizationId, String applicationId, String environment, String region);

    boolean exists(EffectiveConfigurationCacheKey key);
}
