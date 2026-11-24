package com.walmart.cms.dal.repository;

import com.walmart.cms.dal.entity.ConfigurationScopeEntity;

/** Atomic scope creation with persisted-data validation. */
public interface ConfigurationScopeRepositoryCustom {
    ConfigurationScopeEntity createValidated(ConfigurationScopeEntity scope);
}
