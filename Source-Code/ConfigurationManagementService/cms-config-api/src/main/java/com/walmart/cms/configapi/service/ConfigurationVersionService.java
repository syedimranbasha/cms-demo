package com.walmart.cms.configapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmart.cms.cache.exception.CacheAccessException;
import com.walmart.cms.cache.exception.CacheSerializationException;
import com.walmart.cms.cache.service.EffectiveConfigurationCache;
import com.walmart.cms.configapi.dto.CreateConfigurationVersionRequest;
import com.walmart.cms.configapi.dto.CreateConfigurationVersionResponse;
import com.walmart.cms.dal.entity.ConfigurationScopeEntity;
import com.walmart.cms.dal.entity.ConfigurationVersionEntity;
import com.walmart.cms.dal.repository.ConfigurationScopeRepository;
import com.walmart.cms.dal.repository.ConfigurationVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ConfigurationVersionService {
    private final ConfigurationScopeRepository scopes;
    private final ConfigurationVersionRepository versions;
    private final EffectiveConfigurationCache cache;
    private final ObjectMapper mapper;

    public ConfigurationVersionService(ConfigurationScopeRepository scopes, ConfigurationVersionRepository versions, EffectiveConfigurationCache cache, ObjectMapper mapper) {
        this.scopes=scopes; this.versions=versions; this.cache=cache; this.mapper=mapper;
    }

    @Transactional
    public CreateConfigurationVersionResponse createVersion(UUID orgId, UUID appId, String scopeType, String environment, String region, CreateConfigurationVersionRequest request) {
        ConfigurationScopeEntity scope = scopes.findByOrganizationIdAndScopeTypeAndEnvironmentNameAndApplicationIdAndRegionCode(
                orgId, scopeType, environment, ("APPLICATION".equals(scopeType) || "REGION".equals(scopeType)) ? appId : null,
                "REGION".equals(scopeType) ? region : null).orElseThrow(() -> new NoSuchElementException("Configuration scope not found"));
        ConfigurationVersionEntity entity = instantiateVersion();
        entity.setConfigDocument(request.configDocument());
        entity.setChangeSummary(request.changeSummary());
        entity.setChecksum(checksum(request));
        ConfigurationVersionEntity created = versions.createNextVersion(scope.getScopeId(), request.expectedBaseVersionId(), entity);

        // Execute after PostgreSQL commit but synchronously, before the HTTP request returns.
        // Scope dimensions deliberately widen invalidation to all affected applications/regions.
        String invalidationApp = switch (scopeType) { case "APPLICATION", "REGION" -> appId.toString(); default -> null; };
        String invalidationEnvironment = switch (scopeType) { case "ENVIRONMENT", "APPLICATION", "REGION" -> environment; default -> null; };
        String invalidationRegion = "REGION".equals(scopeType) ? region : null;
        Runnable invalidate = () -> {
            try { cache.evictScope(orgId.toString(), invalidationApp, invalidationEnvironment, invalidationRegion); }
            catch (CacheAccessException | CacheSerializationException e) {
                throw new IllegalStateException("Configuration version was committed, but synchronous cache invalidation failed; retry/reconciliation is required", e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { invalidate.run(); }
            });
        } else {
            invalidate.run();
        }
        return new CreateConfigurationVersionResponse(scope.getScopeId(), created.getVersionId(), created.getVersionNumber(), "CREATED");
    }

    private ConfigurationVersionEntity instantiateVersion() {
        try { var c=ConfigurationVersionEntity.class.getDeclaredConstructor(); c.setAccessible(true); return c.newInstance(); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("Unable to instantiate configuration version entity",e); }
    }
    private String checksum(CreateConfigurationVersionRequest request) {
        try { byte[] json=mapper.writeValueAsBytes(request.configDocument()); return "sha256:"+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json)); }
        catch (Exception e) { throw new IllegalStateException("Unable to calculate configuration checksum",e); }
    }
}
