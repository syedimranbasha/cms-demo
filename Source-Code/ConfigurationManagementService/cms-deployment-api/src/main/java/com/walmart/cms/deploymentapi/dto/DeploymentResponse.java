package com.walmart.cms.deploymentapi.dto;
import java.util.*;
public record DeploymentResponse(UUID deploymentId, UUID rollbackOfDeploymentId, String status,
 Map<String,String> baselineVersionSet, Map<String,String> candidateVersionSet,
 List<StageResponse> stages) {
 public record StageResponse(UUID stageId,Integer stageNumber,Integer targetPercentage,String status) {}
}
