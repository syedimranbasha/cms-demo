package com.walmart.cms.cache.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Cache payload: complete merged configuration plus the version metadata
 * needed by the SDK to identify the configuration it received.
 */
public record EffectiveConfiguration(
        Map<String, Object> effectiveConfig,
        ConfigurationVersionMetadata versions,
        Instant cachedAt) {

    public EffectiveConfiguration {
        Objects.requireNonNull(effectiveConfig, "effectiveConfig must not be null");
        Objects.requireNonNull(versions, "versions must not be null");
        effectiveConfig = Map.copyOf(effectiveConfig);
        cachedAt = cachedAt == null ? Instant.now() : cachedAt;
    }
}
