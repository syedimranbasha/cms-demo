package com.walmart.cms.coreapi.dto;
import java.util.Map;
public record EffectiveConfigurationResponse(Map<String,Object> effectiveConfig, Versions versions) {
    public record Versions(String global, String environment, String application, String region) {}
}
