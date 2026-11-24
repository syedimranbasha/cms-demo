package com.walmart.cms.clientsdk.config;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/** Runtime SDK settings. Supply clientSecret from a secret manager or protected runtime configuration. */
public record CmsClientSdkConfig(
        URI apiBaseUrl, URI tokenUri, String clientId, String clientSecret,
        String organizationId, String applicationId, String environment,
        String region, String instanceId, Duration pollingInterval, Duration requestTimeout) {

    public CmsClientSdkConfig {
        Objects.requireNonNull(apiBaseUrl, "apiBaseUrl is required");
        Objects.requireNonNull(tokenUri, "tokenUri is required");
        requireText(clientId, "clientId");
        requireText(clientSecret, "clientSecret");
        requireText(organizationId, "organizationId");
        requireText(applicationId, "applicationId");
        requireText(environment, "environment");
        requireText(region, "region");
        requireText(instanceId, "instanceId");
        pollingInterval = pollingInterval == null ? Duration.ofMinutes(2) : pollingInterval;
        requestTimeout = requestTimeout == null ? Duration.ofSeconds(10) : requestTimeout;
        if (pollingInterval.isZero() || pollingInterval.isNegative()) throw new IllegalArgumentException("pollingInterval must be positive");
        if (requestTimeout.isZero() || requestTimeout.isNegative()) throw new IllegalArgumentException("requestTimeout must be positive");
    }

    public static CmsClientSdkConfig defaults(URI apiBaseUrl, URI tokenUri, String clientId,
            String clientSecret, String organizationId, String applicationId,
            String environment, String region, String instanceId) {
        return new CmsClientSdkConfig(apiBaseUrl, tokenUri, clientId, clientSecret,
                organizationId, applicationId, environment, region, instanceId,
                Duration.ofMinutes(2), Duration.ofSeconds(10));
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }
}
