package com.walmart.cms.clientsdk.model;

import java.util.UUID;

public record RollbackRequest(UUID targetDeploymentId, String changeSummary) {}
