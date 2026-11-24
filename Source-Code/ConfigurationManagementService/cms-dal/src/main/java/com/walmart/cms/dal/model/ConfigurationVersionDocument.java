package com.walmart.cms.dal.model;
import java.util.Map;
import java.util.UUID;
public record ConfigurationVersionDocument(UUID versionId, UUID scopeId, Map<String,Object> configDocument) {}
