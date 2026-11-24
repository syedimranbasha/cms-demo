package com.walmart.cms.clientsdk.model;

import java.util.Map;
import java.util.UUID;

/** Request contract matching cms-config-api CreateConfigurationVersionRequest. */
public record CreateConfigurationVersionRequest(
        Map<String, Object> configDocument,
        String changeSummary,
        UUID expectedBaseVersionId) {
}
