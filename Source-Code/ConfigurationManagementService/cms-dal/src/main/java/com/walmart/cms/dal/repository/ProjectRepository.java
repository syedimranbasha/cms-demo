package com.walmart.cms.dal.repository;
import com.walmart.cms.dal.entity.ProjectEntity;
import org.springframework.data.repository.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface ProjectRepository extends Repository<ProjectEntity, UUID> {
 ProjectEntity save(ProjectEntity project);
 Optional<ProjectEntity> findById(UUID projectId);
 List<ProjectEntity> findByOrganizationIdOrderByName(UUID organizationId);
 List<ProjectEntity> findAllByOrderByName();
 Optional<ProjectEntity> findByProjectIdAndOrganizationId(UUID projectId, UUID organizationId);
 boolean existsByProjectIdAndOrganizationId(UUID projectId, UUID organizationId);
}
