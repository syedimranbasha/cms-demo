package com.walmart.cms.dal.repository;

import com.walmart.cms.dal.entity.ApplicationEntity;
import org.springframework.data.repository.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends Repository<ApplicationEntity, UUID> {
    ApplicationEntity save(ApplicationEntity application);
    Optional<ApplicationEntity> findById(UUID applicationId);
    List<ApplicationEntity> findAllByOrderByName();
    List<ApplicationEntity> findByProjectIdOrderByName(UUID projectId);
    Optional<ApplicationEntity> findByApplicationIdAndProjectId(UUID applicationId, UUID projectId);
    boolean existsByApplicationIdAndProjectId(UUID applicationId, UUID projectId);
}
