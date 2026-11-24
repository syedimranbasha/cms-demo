package com.walmart.cms.coreapi.controller;

import com.walmart.cms.coreapi.dto.*;
import com.walmart.cms.coreapi.service.CoreConfigurationService;
import com.walmart.cms.security.principal.CmsPrincipal;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}")
public class CoreConfigurationController {
    private final CoreConfigurationService service;

    private static final Logger logger =
            LoggerFactory.getLogger(CoreConfigurationController.class);

    public CoreConfigurationController(CoreConfigurationService service) {
        this.service = service;
    }

    @GetMapping("/applications/{appId}/config")
    public EffectiveConfigurationResponse retrieveEffectiveConfiguration(
            @PathVariable UUID orgId,
            @PathVariable UUID appId,
            @RequestParam String environment,
            @RequestParam String region,
            @RequestHeader("X-Instance-Id") String instanceId,
            Optional<CmsPrincipal> principal) {

        principal.ifPresentOrElse(
                user -> logger.info(
                        "Effective configuration requested by user: {}",
                        user.username()),
                () -> logger.debug("Effective configuration requested without an authenticated principal")
        );

        return service.retrieveEffectiveConfiguration(
                orgId, appId, environment, region, instanceId);
    }

    @GetMapping("/applications/{appId}/config/versions/{versionId}")
    public HistoricalConfigurationVersionResponse retrieveHistoricalVersion(
            @PathVariable UUID orgId,
            @PathVariable UUID appId,
            @PathVariable UUID versionId,
            Optional<CmsPrincipal> principal) {

        principal.ifPresentOrElse(
                user -> logger.info(
                        "retrieveHistoricalVersion requested by user: {}",
                        user.username()),
                () -> logger.debug(
                        "retrieveHistoricalVersion requested without an authenticated principal")
        );

        return service.retrieveHistoricalVersion(orgId, appId, versionId);
    }
}
