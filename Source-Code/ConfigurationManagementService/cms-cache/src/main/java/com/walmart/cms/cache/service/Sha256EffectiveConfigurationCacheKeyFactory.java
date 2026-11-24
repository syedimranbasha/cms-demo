package com.walmart.cms.cache.service;

import com.walmart.cms.cache.service.EffectiveConfigurationCacheKeyFactory;
import com.walmart.cms.cache.model.EffectiveConfigurationCacheKey;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/** Fingerprint is stable regardless of the input Map iteration order. */
@Component
public class Sha256EffectiveConfigurationCacheKeyFactory
        implements EffectiveConfigurationCacheKeyFactory {

    @Override
    public EffectiveConfigurationCacheKey create(
            UUID organizationId,
            UUID applicationId,
            String environment,
            String region,
            String rolloutTrack,
            Map<String, String> scopeVersions) {

        if (scopeVersions == null || scopeVersions.isEmpty()) {
            throw new IllegalArgumentException("scopeVersions must not be empty");
        }

        String canonical = new TreeMap<>(scopeVersions).entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        return new EffectiveConfigurationCacheKey(
                organizationId.toString(),
                applicationId.toString(),
                environment,
                region,
                rolloutTrack,
                sha256(canonical));
    }

    private String sha256(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
