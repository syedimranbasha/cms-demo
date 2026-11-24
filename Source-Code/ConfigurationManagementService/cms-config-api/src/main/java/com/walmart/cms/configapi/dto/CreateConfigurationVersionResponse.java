package com.walmart.cms.configapi.dto;
import java.util.UUID;
public record CreateConfigurationVersionResponse(UUID scopeId, UUID versionId, Integer versionNumber, String status) {}
