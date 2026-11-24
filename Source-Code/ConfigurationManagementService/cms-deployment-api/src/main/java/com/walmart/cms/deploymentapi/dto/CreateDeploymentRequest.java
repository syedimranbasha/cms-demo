package com.walmart.cms.deploymentapi.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public record CreateDeploymentRequest(@NotBlank String environmentName,@NotBlank String regionCode,
 @NotEmpty List<@Valid StageRequest> stages) {
 public record StageRequest(@Min(1) int stageNumber,@Min(1) @Max(100) int targetPercentage) {}
}
