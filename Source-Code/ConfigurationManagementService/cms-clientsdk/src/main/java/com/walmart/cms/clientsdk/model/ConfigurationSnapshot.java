package com.walmart.cms.clientsdk.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Map;

/** Snapshot of effective configuration and the version metadata returned by CMS. */
public record ConfigurationSnapshot(JsonNode effectiveConfig, Map<String, String> versions, Instant fetchedAt) {
    public ConfigurationSnapshot {
        effectiveConfig = effectiveConfig == null ? null : effectiveConfig.deepCopy();
        versions = versions == null ? Map.of() : Map.copyOf(versions);
    }
    @Override
    public JsonNode effectiveConfig() {
        return effectiveConfig == null ? null : effectiveConfig.deepCopy();
    }
}
