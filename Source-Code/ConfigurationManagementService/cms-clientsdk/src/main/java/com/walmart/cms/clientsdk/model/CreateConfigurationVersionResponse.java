package com.walmart.cms.clientsdk.model;

import java.util.UUID;

/** Result returned after a configuration version is persisted. */
public record CreateConfigurationVersionResponse(
        UUID scopeId,
        UUID versionId,
        Integer versionNumber,
        String status) {
}
