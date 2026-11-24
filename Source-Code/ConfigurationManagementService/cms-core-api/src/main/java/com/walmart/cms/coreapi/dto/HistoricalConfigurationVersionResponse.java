package com.walmart.cms.coreapi.dto;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
public record HistoricalConfigurationVersionResponse(UUID versionId, UUID scopeId, String scopeType,
 Integer versionNumber, Map<String,Object> configDocument, String checksum, String changeSummary, Instant createdAt) {}
