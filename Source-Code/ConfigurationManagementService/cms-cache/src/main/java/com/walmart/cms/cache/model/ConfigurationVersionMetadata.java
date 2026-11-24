package com.walmart.cms.cache.model;

import java.util.Objects;

/** Version IDs for the four CMS configuration scopes. */
public record ConfigurationVersionMetadata(
        String global,
        String environment,
        String application,
        String region) {

    public ConfigurationVersionMetadata {
        Objects.requireNonNull(global, "global version is required");
        Objects.requireNonNull(environment, "environment version is required");
        Objects.requireNonNull(application, "application version is required");
        Objects.requireNonNull(region, "region version is required");
    }
}
