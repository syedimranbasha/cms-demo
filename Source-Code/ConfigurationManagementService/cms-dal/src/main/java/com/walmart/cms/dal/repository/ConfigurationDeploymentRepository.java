package com.walmart.cms.dal.repository;

import com.walmart.cms.dal.entity.ConfigurationDeploymentEntity;
import org.springframework.data.repository.Repository;
import java.util.Optional;
import java.util.UUID;

public interface ConfigurationDeploymentRepository extends Repository<ConfigurationDeploymentEntity, UUID> {
    ConfigurationDeploymentEntity save(ConfigurationDeploymentEntity deployment);
    Optional<ConfigurationDeploymentEntity> findById(UUID deploymentId);
    Optional<ConfigurationDeploymentEntity> findByDeploymentIdAndApplicationId(UUID deploymentId, UUID applicationId);
    Optional<ConfigurationDeploymentEntity> findFirstByApplicationIdAndEnvironmentNameAndRegionCodeAndStatusOrderByDeploymentIdDesc(
            UUID applicationId, String environmentName, String regionCode, String status);
}
