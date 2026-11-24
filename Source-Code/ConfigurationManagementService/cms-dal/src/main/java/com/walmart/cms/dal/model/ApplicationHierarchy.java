package com.walmart.cms.dal.model;
import java.util.UUID;
public record ApplicationHierarchy(UUID organizationId, String organizationName,
                                   UUID projectId, String projectName,
                                   UUID applicationId, String applicationName) {}
