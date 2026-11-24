package com.walmart.cms.coreapi.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmart.cms.cache.model.*;
import com.walmart.cms.cache.exception.CacheAccessException;
import com.walmart.cms.cache.exception.CacheSerializationException;
import com.walmart.cms.cache.service.*;
import com.walmart.cms.coreapi.dto.*;
import com.walmart.cms.dal.entity.*;
import com.walmart.cms.dal.repository.*;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class CoreConfigurationService {
    private static final Logger log = LoggerFactory.getLogger(CoreConfigurationService.class);
    public static final int CONFIG_CACHE_TTL_HOURS = 6;
    private final OrganizationRepository organizations;
    private final ProjectRepository projects;
    private final ApplicationRepository applications;
    private final ConfigurationScopeRepository scopes;
    private final ConfigurationVersionRepository versions;
    private final ConfigurationDeploymentRepository deployments;
    private final DeploymentStageRepository deploymentStages;
    private final EffectiveConfigurationCache cache;
    private final EffectiveConfigurationCacheKeyFactory keyFactory;
    private final ConfigurationMergeResolver merger;
    private final ObjectMapper mapper;

    public CoreConfigurationService(OrganizationRepository organizations, ProjectRepository projects,
        ApplicationRepository applications, ConfigurationScopeRepository scopes,
        ConfigurationVersionRepository versions, ConfigurationDeploymentRepository deployments,
        DeploymentStageRepository deploymentStages,
        EffectiveConfigurationCache cache, EffectiveConfigurationCacheKeyFactory keyFactory,
        ConfigurationMergeResolver merger, ObjectMapper mapper) {
        this.organizations=organizations; this.projects=projects; this.applications=applications;
        this.scopes=scopes; this.versions=versions; this.cache=cache; this.keyFactory=keyFactory;
        this.deployments=deployments; this.deploymentStages=deploymentStages;
        this.merger=merger; this.mapper=mapper;
    }

    @Transactional(readOnly=true)
    public EffectiveConfigurationResponse retrieveEffectiveConfiguration(UUID orgId, UUID appId,
        String environment, String region, String instanceId) {
        if (environment == null || environment.isBlank() || region == null || region.isBlank())
            throw new IllegalArgumentException("environment and region are required");
        if (instanceId == null || instanceId.isBlank())
            throw new IllegalArgumentException("instanceId is required");

        // Resolve the selected version set before consulting Redis. The cache fingerprint
        // must include the actual selected versions; an instance-only fingerprint can keep
        // returning an old empty/baseline result after a deployment changes the version set.
        // PostgreSQL remains the source of truth for deployment state and rollout selection.
        ApplicationEntity app = applications.findById(appId)
            .orElseThrow(() -> new NoSuchElementException("Application not found"));
        ProjectEntity project = projects.findById(app.getProjectId())
            .orElseThrow(() -> new NoSuchElementException("Project not found"));
        if (!organizations.existsById(orgId) || !project.getOrganizationId().equals(orgId))
            throw new NoSuchElementException("Application does not belong to organization");

        ConfigurationScopeEntity global = findScope(orgId,"GLOBAL",null,null,null);
        ConfigurationScopeEntity env = findScope(orgId,"ENVIRONMENT",environment,null,null);
        ConfigurationScopeEntity application = findScope(orgId,"APPLICATION",environment,appId,null);
        ConfigurationScopeEntity regional = findScope(orgId,"REGION",environment,appId,region);

        Map<String,String> versionIds = new LinkedHashMap<>();
        Optional<ConfigurationDeploymentEntity> activeDeployment =
            deployments.findFirstByApplicationIdAndEnvironmentNameAndRegionCodeAndStatusOrderByDeploymentIdDesc(
                appId, environment, region, "IN_PROGRESS");
        if (activeDeployment.isPresent()) {
            ConfigurationDeploymentEntity deployment = activeDeployment.get();
            Map<String,String> selectedSet = deployment.getBaselineVersionSet();

            // A stage becomes active only after it is initiated. Assign instances deterministically
            // using a stable 0-99 bucket so the rollout cohort remains consistent across requests
            // and CMS instances. Increasing the target percentage expands the cohort monotonically.
            if (deployment.getCurrentStageId() != null) {
                DeploymentStageEntity currentStage = deploymentStages
                    .findByStageIdAndDeploymentId(deployment.getCurrentStageId(), deployment.getDeploymentId())
                    .orElseThrow(() -> new IllegalStateException(
                        "Current deployment stage does not exist: " + deployment.getCurrentStageId()));
                if (isInRollout(instanceId, currentStage.getTargetPercentage())) {
                    selectedSet = deployment.getCandidateVersionSet();
                }
            }
            versionIds.putAll(selectedSet);
            log.info("Effective configuration selected deployment {} for application {}, environment {}, region {}, instance {} with stage percentage {}",
                deployment.getDeploymentId(), appId, environment, region, instanceId,
                deployment.getCurrentStageId() == null ? 0 : deploymentStages
                    .findByStageIdAndDeploymentId(deployment.getCurrentStageId(), deployment.getDeploymentId())
                    .map(DeploymentStageEntity::getTargetPercentage).orElse(0));
        } else {
            log.warn("No IN_PROGRESS deployment found for application {}, environment {}, region {}. Active scope pointers will be used.",
                appId, environment, region);
            versionIds.put("global", id(activeVersion(global)));
            versionIds.put("environment", id(activeVersion(env)));
            versionIds.put("application", id(activeVersion(application)));
            versionIds.put("region", id(activeVersion(regional)));
        }
        versionIds.putIfAbsent("global","NONE");
        versionIds.putIfAbsent("environment","NONE");
        versionIds.putIfAbsent("application","NONE");
        versionIds.putIfAbsent("region","NONE");

        Map<String,String> cacheFingerprintInputs = new LinkedHashMap<>(versionIds);
        cacheFingerprintInputs.put("instanceId", instanceId);
        EffectiveConfigurationCacheKey key = keyFactory.create(
            orgId, appId, environment, region, "instance", cacheFingerprintInputs);
        try {
            Optional<EffectiveConfiguration> cached = cache.get(key);
            if (cached.isPresent()) {
                EffectiveConfiguration value = cached.get();
                return new EffectiveConfigurationResponse(value.effectiveConfig(),
                    new EffectiveConfigurationResponse.Versions(value.versions().global(),
                        value.versions().environment(), value.versions().application(),
                        value.versions().region()));
            }
        } catch (CacheAccessException | CacheSerializationException ignored) {
            // Redis is an optimization. Resolve from PostgreSQL if Redis is unavailable/corrupt.
        }

        ConfigurationVersionEntity gv = version(versionIds.get("global"));
        ConfigurationVersionEntity ev = version(versionIds.get("environment"));
        ConfigurationVersionEntity av = version(versionIds.get("application"));
        ConfigurationVersionEntity rv = version(versionIds.get("region"));
        Map<String,Object> merged = merger.merge(document(gv),document(ev),document(av),document(rv));
        EffectiveConfiguration result = new EffectiveConfiguration(merged,
            new ConfigurationVersionMetadata(id(gv),id(ev),id(av),id(rv)), Instant.now());
        try {
            cache.put(key,result,Duration.ofHours(CONFIG_CACHE_TTL_HOURS));
        } catch (CacheAccessException | CacheSerializationException ignored) {
            // A failed cache population must not fail a successful PostgreSQL-backed read.
        }
        return new EffectiveConfigurationResponse(merged,
            new EffectiveConfigurationResponse.Versions(id(gv),id(ev),id(av),id(rv)));
    }

    @Transactional(readOnly=true)
    public HistoricalConfigurationVersionResponse retrieveHistoricalVersion(UUID orgId, UUID appId, UUID versionId) {
        ConfigurationVersionEntity version = versions.findById(versionId)
            .orElseThrow(() -> new NoSuchElementException("Configuration version not found"));
        ConfigurationScopeEntity scope = scopes.findById(version.getScopeId())
            .orElseThrow(() -> new NoSuchElementException("Configuration scope not found"));
        if (!scope.getOrganizationId().equals(orgId) ||
            (scope.getApplicationId()!=null && !scope.getApplicationId().equals(appId)))
            throw new NoSuchElementException("Configuration version not found for organization/application");
        return new HistoricalConfigurationVersionResponse(version.getVersionId(),version.getScopeId(),
            scope.getScopeType(),version.getVersionNumber(),version.getConfigDocument(),
            version.getChecksum(),version.getChangeSummary(),null);
    }


    static boolean isInRollout(String instanceId, int targetPercentage) {
        if (instanceId == null || instanceId.isBlank()) {
            throw new IllegalArgumentException("instanceId is required for rollout selection");
        }
        if (targetPercentage < 1 || targetPercentage > 100) {
            throw new IllegalArgumentException("targetPercentage must be between 1 and 100");
        }
        int bucket = Math.floorMod(UUID.nameUUIDFromBytes(
            instanceId.getBytes(java.nio.charset.StandardCharsets.UTF_8)).hashCode(), 100);
        return bucket < targetPercentage;
    }

    private ConfigurationScopeEntity findScope(UUID org,String type,String env,UUID app,String region) {
        return scopes.findByOrganizationIdAndScopeTypeAndEnvironmentNameAndApplicationIdAndRegionCode(
            org,type,env,app,region).orElse(null);
    }
    private ConfigurationVersionEntity activeVersion(ConfigurationScopeEntity scope) {
        if (scope == null || scope.getActiveVersionId()==null) return null;
        return versions.findById(scope.getActiveVersionId())
            .orElseThrow(() -> new IllegalStateException("Active configuration version is missing"));
    }
    private ConfigurationVersionEntity version(String id) {
        if (id == null || "NONE".equals(id)) return null;
        return versions.findById(UUID.fromString(id))
            .orElseThrow(() -> new IllegalStateException("Selected deployment version does not exist: " + id));
    }
    private Map<String,Object> document(ConfigurationVersionEntity v) { return v==null ? Map.of() : v.getConfigDocument(); }
    private String id(ConfigurationVersionEntity v) { return v==null ? "NONE" : v.getVersionId().toString(); }

}
