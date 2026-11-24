package com.walmart.cms.cache.service;

import com.walmart.cms.cache.model.EffectiveConfigurationCacheKey;

import java.util.Map;
import java.util.UUID;

/** Creates deterministic keys from the exact version set selected by cms-api. */
public interface EffectiveConfigurationCacheKeyFactory {

    EffectiveConfigurationCacheKey create(
            UUID organizationId,
            UUID applicationId,
            String environment,
            String region,
            String rolloutTrack,
            Map<String, String> scopeVersions);
}
