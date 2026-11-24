package com.walmart.cms.configapi.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;
public record CreateConfigurationVersionRequest(@NotNull Map<String,Object> configDocument, @NotBlank String changeSummary, UUID expectedBaseVersionId) {}
