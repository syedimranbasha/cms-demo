package com.walmart.cms.dal.repository;
import com.walmart.cms.dal.entity.DeploymentStageEntity;
import org.springframework.data.repository.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface DeploymentStageRepository extends Repository<DeploymentStageEntity, UUID> {
 DeploymentStageEntity save(DeploymentStageEntity stage);
 Optional<DeploymentStageEntity> findByStageIdAndDeploymentId(UUID stageId, UUID deploymentId);
 Optional<DeploymentStageEntity> findFirstByDeploymentIdAndStatusOrderByStageNumber(UUID deploymentId,String status);
 List<DeploymentStageEntity> findByDeploymentIdOrderByStageNumber(UUID deploymentId);
}
