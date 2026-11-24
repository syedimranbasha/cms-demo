package com.walmart.cms.dal.repository;

import com.walmart.cms.dal.entity.ConfigurationVersionEntity;
import com.walmart.cms.dal.exception.StaleConfigurationVersionException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
import java.util.UUID;

/** Atomic immutable-version creation using Spring Data JPA repository methods. */
@Repository
public class ConfigurationVersionRepositoryImpl implements ConfigurationVersionRepositoryCustom {
    private final ConfigurationScopeRepository scopes;
    private final ConfigurationVersionRepository versions;

    public ConfigurationVersionRepositoryImpl(ConfigurationScopeRepository scopes,
                                              @Lazy ConfigurationVersionRepository versions) {
        this.scopes = scopes;
        this.versions = versions;
    }

    @Override
    @Transactional
    public ConfigurationVersionEntity createNextVersion(UUID scopeId, UUID expectedBaseVersionId,
                                                          ConfigurationVersionEntity version) {
        if (scopeId == null) throw new IllegalArgumentException("scopeId is required");
        if (version == null) throw new IllegalArgumentException("version is required");
        scopes.findFirstByScopeId(scopeId)
                .orElseThrow(() -> new IllegalArgumentException("Configuration scope not found: " + scopeId));
        var latest = versions.findTopByScopeIdOrderByVersionNumberDesc(scopeId);
        UUID actual = latest.map(ConfigurationVersionEntity::getVersionId).orElse(null);
        if (!Objects.equals(expectedBaseVersionId, actual)) {
            throw new StaleConfigurationVersionException(scopeId, expectedBaseVersionId, actual);
        }
        version.setVersionId(version.getVersionId() == null ? UUID.randomUUID() : version.getVersionId());
        version.setScopeId(scopeId);
        version.setVersionNumber(latest.map(v -> v.getVersionNumber() + 1).orElse(1));
        return versions.save(version);
    }
}
