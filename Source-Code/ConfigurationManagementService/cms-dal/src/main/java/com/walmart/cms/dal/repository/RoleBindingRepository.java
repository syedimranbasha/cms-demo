package com.walmart.cms.dal.repository;

import com.walmart.cms.dal.entity.RoleBindingEntity;
import org.springframework.data.repository.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public interface RoleBindingRepository extends Repository<RoleBindingEntity, UUID> {
    RoleBindingEntity save(RoleBindingEntity binding);
    List<RoleBindingEntity> findAllByPrincipalId(UUID principalId);
    List<RoleBindingEntity> findByPrincipalIdAndOrganizationIdAndProjectIdIsNullAndApplicationIdIsNull(
            UUID principalId, UUID organizationId);
    List<RoleBindingEntity> findByPrincipalIdAndOrganizationIdAndProjectIdAndApplicationIdIsNull(
            UUID principalId, UUID organizationId, UUID projectId);
    List<RoleBindingEntity> findByPrincipalIdAndOrganizationIdAndProjectIdIsNullAndApplicationId(
            UUID principalId, UUID organizationId, UUID applicationId);
    List<RoleBindingEntity> findByPrincipalIdAndOrganizationIdAndProjectIdAndApplicationId(
            UUID principalId, UUID organizationId, UUID projectId, UUID applicationId);

    default List<RoleBindingEntity> findApplicableBindings(UUID principalId, UUID organizationId,
                                                             UUID projectId, UUID applicationId) {
        List<RoleBindingEntity> result = new ArrayList<>();
        result.addAll(findByPrincipalIdAndOrganizationIdAndProjectIdIsNullAndApplicationIdIsNull(principalId, organizationId));
        result.addAll(findByPrincipalIdAndOrganizationIdAndProjectIdAndApplicationIdIsNull(principalId, organizationId, projectId));
        result.addAll(findByPrincipalIdAndOrganizationIdAndProjectIdIsNullAndApplicationId(principalId, organizationId, applicationId));
        result.addAll(findByPrincipalIdAndOrganizationIdAndProjectIdAndApplicationId(principalId, organizationId, projectId, applicationId));
        return result;
    }
}
