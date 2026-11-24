package com.walmart.cms.dal.repository;
import com.walmart.cms.dal.entity.PermissionEntity;
import org.springframework.data.repository.Repository;
import java.util.Optional;
import java.util.UUID;
public interface PermissionRepository extends Repository<PermissionEntity, UUID> {
 PermissionEntity save(PermissionEntity permission);
 Optional<PermissionEntity> findById(UUID permissionId);
 Optional<PermissionEntity> findByPermissionName(String permissionName);
 java.util.List<PermissionEntity> findAllByOrderByPermissionName();
}
