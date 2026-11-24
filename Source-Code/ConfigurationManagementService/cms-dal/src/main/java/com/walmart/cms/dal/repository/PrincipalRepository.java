package com.walmart.cms.dal.repository;
import com.walmart.cms.dal.entity.PrincipalEntity;
import org.springframework.data.repository.Repository;
import java.util.Optional;
import java.util.UUID;
public interface PrincipalRepository extends Repository<PrincipalEntity, UUID> {
 PrincipalEntity save(PrincipalEntity principal);
 Optional<PrincipalEntity> findById(UUID principalId);
 Optional<PrincipalEntity> findByPrincipalTypeAndExternalId(String principalType,String externalId);
 java.util.List<PrincipalEntity> findAllByOrderByExternalId();
}
