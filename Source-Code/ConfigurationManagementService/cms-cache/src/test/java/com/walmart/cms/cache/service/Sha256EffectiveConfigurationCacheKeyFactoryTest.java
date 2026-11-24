package com.walmart.cms.cache.service;

import com.walmart.cms.cache.model.EffectiveConfigurationCacheKey;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class Sha256EffectiveConfigurationCacheKeyFactoryTest {

    private final Sha256EffectiveConfigurationCacheKeyFactory factory =
            new Sha256EffectiveConfigurationCacheKeyFactory();

    @Test
    void createsStableFingerprintRegardlessOfMapIterationOrder() {
        UUID orgId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID appId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

        Map<String, String> first = new LinkedHashMap<>();
        first.put("GLOBAL", "g-v5");
        first.put("ENVIRONMENT", "e-v12");
        first.put("APPLICATION", "a-v8");
        first.put("REGION", "r-v3");

        Map<String, String> second = new LinkedHashMap<>();
        second.put("REGION", "r-v3");
        second.put("APPLICATION", "a-v8");
        second.put("GLOBAL", "g-v5");
        second.put("ENVIRONMENT", "e-v12");

        EffectiveConfigurationCacheKey key1 =
                factory.create(orgId, appId, "PROD", "us-central1", "baseline", first);
        EffectiveConfigurationCacheKey key2 =
                factory.create(orgId, appId, "PROD", "us-central1", "baseline", second);

        assertEquals(key1.versionSetFingerprint(), key2.versionSetFingerprint());
        assertEquals(64, key1.versionSetFingerprint().length());
        assertEquals(orgId.toString(), key1.organizationId());
        assertEquals(appId.toString(), key1.applicationId());
    }

    @Test
    void changesFingerprintWhenAnyScopeVersionChanges() {
        UUID orgId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();

        Map<String, String> original = Map.of(
                "GLOBAL", "g-v5",
                "ENVIRONMENT", "e-v12",
                "APPLICATION", "a-v8",
                "REGION", "r-v3");

        Map<String, String> changed = Map.of(
                "GLOBAL", "g-v5",
                "ENVIRONMENT", "e-v13",
                "APPLICATION", "a-v8",
                "REGION", "r-v3");

        var originalKey = factory.create(orgId, appId, "PROD", "us-central1", "baseline", original);
        var changedKey = factory.create(orgId, appId, "PROD", "us-central1", "baseline", changed);

        assertNotEquals(originalKey.versionSetFingerprint(), changedKey.versionSetFingerprint());
    }

    @Test
    void rejectsNullOrEmptyVersionSet() {
        UUID orgId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class,
                () -> factory.create(orgId, appId, "PROD", "us-central1", "baseline", null));
        assertThrows(IllegalArgumentException.class,
                () -> factory.create(orgId, appId, "PROD", "us-central1", "baseline", Map.of()));
    }
}
