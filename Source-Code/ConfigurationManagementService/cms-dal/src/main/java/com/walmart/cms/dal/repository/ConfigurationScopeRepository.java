package com.walmart.cms.dal.repository;

import com.walmart.cms.dal.entity.ConfigurationScopeEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.Repository;
import java.util.Optional;
import java.util.UUID;

public interface ConfigurationScopeRepository extends Repository<ConfigurationScopeEntity, UUID>, ConfigurationScopeRepositoryCustom {
    ConfigurationScopeEntity save(ConfigurationScopeEntity scope);
    Optional<ConfigurationScopeEntity> findById(UUID scopeId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ConfigurationScopeEntity> findFirstByScopeId(UUID scopeId);
    Optional<ConfigurationScopeEntity> findByOrganizationIdAndScopeTypeAndEnvironmentNameAndApplicationIdAndRegionCode(
            UUID organizationId, String scopeType, String environmentName, UUID applicationId, String regionCode);
    boolean existsByOrganizationIdAndScopeTypeAndEnvironmentNameAndApplicationIdAndRegionCode(
            UUID organizationId, String scopeType, String environmentName, UUID applicationId, String regionCode);
}
