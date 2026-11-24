package com.walmart.cms.clientsdk.model;

import java.util.UUID;

/** Response returned when an organization is created in CMS. */
public record OrganizationResponse(UUID organizationId, String name) {}
