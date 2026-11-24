package com.walmart.cms.dal.repository;
import com.walmart.cms.dal.entity.RolePermissionEntity;
import com.walmart.cms.dal.entity.RolePermissionId;
import org.springframework.data.repository.Repository;
import java.util.List;
import java.util.UUID;
public interface RolePermissionRepository extends Repository<RolePermissionEntity, RolePermissionId> {
 RolePermissionEntity save(RolePermissionEntity rolePermission);
 List<RolePermissionEntity> findByIdRoleId(UUID roleId);
 boolean existsById(RolePermissionId id);
}
