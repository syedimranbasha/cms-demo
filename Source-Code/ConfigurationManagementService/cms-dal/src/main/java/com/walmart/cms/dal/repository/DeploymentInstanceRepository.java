package com.walmart.cms.dal.repository;
import com.walmart.cms.dal.entity.DeploymentInstanceEntity;
import com.walmart.cms.dal.entity.DeploymentInstanceId;
import org.springframework.data.repository.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface DeploymentInstanceRepository extends Repository<DeploymentInstanceEntity, DeploymentInstanceId> {
 DeploymentInstanceEntity save(DeploymentInstanceEntity instance);
 Optional<DeploymentInstanceEntity> findById(DeploymentInstanceId id);
 Optional<DeploymentInstanceEntity> findByIdDeploymentIdAndIdStageIdAndIdInstanceId(UUID deploymentId,UUID stageId,String instanceId);
 List<DeploymentInstanceEntity> findByIdDeploymentIdAndIdStageId(UUID deploymentId,UUID stageId);
}
