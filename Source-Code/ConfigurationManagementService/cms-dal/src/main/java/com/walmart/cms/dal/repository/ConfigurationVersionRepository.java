package com.walmart.cms.dal.repository;

import com.walmart.cms.dal.entity.ConfigurationVersionEntity;
import org.springframework.data.repository.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConfigurationVersionRepository extends Repository<ConfigurationVersionEntity, UUID>, ConfigurationVersionRepositoryCustom {
    ConfigurationVersionEntity save(ConfigurationVersionEntity version);
    Optional<ConfigurationVersionEntity> findById(UUID versionId);
    Optional<ConfigurationVersionEntity> findByVersionIdAndScopeId(UUID versionId, UUID scopeId);
    Optional<ConfigurationVersionEntity> findTopByScopeIdOrderByVersionNumberDesc(UUID scopeId);
    List<ConfigurationVersionEntity> findAllByVersionIdIn(Iterable<UUID> versionIds);
}
