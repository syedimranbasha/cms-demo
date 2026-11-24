package com.walmart.cms.deploymentapi.dto;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record RollbackRequest(@NotNull UUID targetDeploymentId, String changeSummary) {}
