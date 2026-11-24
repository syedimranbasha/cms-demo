package com.walmart.cms.configapi.controller;

import com.walmart.cms.configapi.dto.CreateConfigurationVersionRequest;
import com.walmart.cms.configapi.dto.CreateConfigurationVersionResponse;
import com.walmart.cms.configapi.service.ConfigurationVersionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}")
public class ConfigurationVersionController {
    private final ConfigurationVersionService service;
    public ConfigurationVersionController(ConfigurationVersionService service) { this.service=service; }

    @PostMapping("/config/versions") @ResponseStatus(HttpStatus.CREATED)
    public CreateConfigurationVersionResponse createGlobal(@PathVariable UUID orgId, @Valid @RequestBody CreateConfigurationVersionRequest request) { return service.createVersion(orgId,null,"GLOBAL",null,null,request); }

    @PostMapping("/environments/{environment}/config/versions") @ResponseStatus(HttpStatus.CREATED)
    public CreateConfigurationVersionResponse createEnvironment(@PathVariable UUID orgId,@PathVariable String environment,@Valid @RequestBody CreateConfigurationVersionRequest request) { return service.createVersion(orgId,null,"ENVIRONMENT",environment,null,request); }

    @PostMapping("/applications/{appId}/config/versions") @ResponseStatus(HttpStatus.CREATED)
    public CreateConfigurationVersionResponse createApplication(@PathVariable UUID orgId,@PathVariable UUID appId,@RequestParam String environment,@Valid @RequestBody CreateConfigurationVersionRequest request) { return service.createVersion(orgId,appId,"APPLICATION",environment,null,request); }

    @PostMapping("/applications/{appId}/environments/{environment}/regions/{region}/config/versions") @ResponseStatus(HttpStatus.CREATED)
    public CreateConfigurationVersionResponse createRegion(@PathVariable UUID orgId,@PathVariable UUID appId,@PathVariable String environment,@PathVariable String region,@Valid @RequestBody CreateConfigurationVersionRequest request) { return service.createVersion(orgId,appId,"REGION",environment,region,request); }
}
