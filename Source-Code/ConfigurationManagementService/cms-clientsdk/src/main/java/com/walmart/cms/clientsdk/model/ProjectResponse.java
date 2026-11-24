package com.walmart.cms.clientsdk.model;

import java.util.UUID;

/** Response returned when a project is created in CMS. */
public record ProjectResponse(UUID projectId, UUID organizationId, String name) {}
