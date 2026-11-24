package com.walmart.cms.dal.repository;
import com.walmart.cms.dal.entity.OrganizationEntity;
import org.springframework.data.repository.Repository;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
public interface OrganizationRepository extends Repository<OrganizationEntity, UUID> {
 OrganizationEntity save(OrganizationEntity organization);
 Optional<OrganizationEntity> findById(UUID organizationId);
 boolean existsById(UUID organizationId);
 java.util.List<OrganizationEntity> findAllByOrderByName();
 Optional<OrganizationEntity> findByName(String name);
}
