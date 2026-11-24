package com.walmart.cms.clientsdk.model;

import java.util.List;

public record CreateDeploymentRequest(String environmentName, String regionCode, List<StageRequest> stages) {
    public record StageRequest(int stageNumber, int targetPercentage) {}
}
