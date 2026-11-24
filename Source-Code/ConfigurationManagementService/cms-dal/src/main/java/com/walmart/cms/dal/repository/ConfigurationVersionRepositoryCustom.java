package com.walmart.cms.dal.repository;
import com.walmart.cms.dal.entity.ConfigurationVersionEntity;
import java.util.UUID;
public interface ConfigurationVersionRepositoryCustom {
    ConfigurationVersionEntity createNextVersion(UUID scopeId, UUID expectedBaseVersionId, ConfigurationVersionEntity version);
}
