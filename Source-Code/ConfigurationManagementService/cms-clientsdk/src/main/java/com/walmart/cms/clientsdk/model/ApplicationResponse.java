package com.walmart.cms.clientsdk.model;

import java.util.UUID;

/** Response returned when an application is created in CMS. */
public record ApplicationResponse(UUID applicationId, UUID projectId, String name) {}
