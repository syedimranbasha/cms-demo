package com.walmart.cms.dal.repository;
import com.walmart.cms.dal.entity.RoleEntity;
import org.springframework.data.repository.Repository;
import java.util.Optional;
import java.util.UUID;
public interface RoleRepository extends Repository<RoleEntity, UUID> {
 RoleEntity save(RoleEntity role);
 Optional<RoleEntity> findById(UUID roleId);
 Optional<RoleEntity> findByRoleName(String roleName);
 java.util.List<RoleEntity> findAllByOrderByRoleName();
}
