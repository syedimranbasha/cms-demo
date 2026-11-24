package com.walmart.cms.clientsdk.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DeploymentResponse(
        UUID deploymentId,
        UUID rollbackOfDeploymentId,
        String status,
        Map<String, String> baselineVersionSet,
        Map<String, String> candidateVersionSet,
        List<StageResponse> stages) {
    public record StageResponse(UUID stageId, Integer stageNumber, Integer targetPercentage, String status) {}
}
