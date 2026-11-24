package com.walmart.cms.dal.repository;

import com.walmart.cms.dal.entity.ConfigurationScopeEntity;
import com.walmart.cms.dal.exception.DuplicateConfigurationScopeException;
import com.walmart.cms.dal.exception.InvalidConfigurationScopeException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

/** Validates persisted-data constraints and creates a scope atomically. */
@Repository
public class ConfigurationScopeRepositoryImpl implements ConfigurationScopeRepositoryCustom {

    private final OrganizationRepository organizationRepository;
    private final ApplicationRepository applicationRepository;
    private final ProjectRepository projectRepository;
    private final ConfigurationScopeRepository scopeRepository;

    public ConfigurationScopeRepositoryImpl(OrganizationRepository organizationRepository,
                                            ApplicationRepository applicationRepository,
                                            ProjectRepository projectRepository,
                                            @Lazy ConfigurationScopeRepository scopeRepository) {
        this.organizationRepository = organizationRepository;
        this.applicationRepository = applicationRepository;
        this.projectRepository = projectRepository;
        this.scopeRepository = scopeRepository;
    }

    @Override
    @Transactional
    public ConfigurationScopeEntity createValidated(ConfigurationScopeEntity scope) {
        if (scope == null) {
            throw new InvalidConfigurationScopeException("Scope is required");
        }

        UUID organizationId = scope.getOrganizationId();
        if (organizationId == null || !organizationRepository.existsById(organizationId)) {
            throw new InvalidConfigurationScopeException("A valid organizationId is required");
        }

        String type = scope.getScopeType() == null
                ? "" : scope.getScopeType().toUpperCase(Locale.ROOT);

        switch (type) {
            case "GLOBAL" -> {
                requireNull(scope.getEnvironmentName(), "environmentName");
                requireNull(scope.getApplicationId(), "applicationId");
                requireNull(scope.getRegionCode(), "regionCode");
            }
            case "ENVIRONMENT" -> {
                requireValue(scope.getEnvironmentName(), "environmentName");
                requireNull(scope.getApplicationId(), "applicationId");
                requireNull(scope.getRegionCode(), "regionCode");
            }
            case "APPLICATION" -> {
                requireValue(scope.getEnvironmentName(), "environmentName");
                requireApplication(scope);
                requireNull(scope.getRegionCode(), "regionCode");
            }
            case "REGION" -> {
                requireValue(scope.getEnvironmentName(), "environmentName");
                requireApplication(scope);
                requireValue(scope.getRegionCode(), "regionCode");
            }
            default -> throw new InvalidConfigurationScopeException(
                    "Unsupported scopeType: " + scope.getScopeType());
        }

        scope.setScopeType(type);
        boolean duplicate = scopeRepository.existsByOrganizationIdAndScopeTypeAndEnvironmentNameAndApplicationIdAndRegionCode(
                organizationId, type, scope.getEnvironmentName(),
                scope.getApplicationId(), scope.getRegionCode());
        if (duplicate) {
            throw new DuplicateConfigurationScopeException(
                    "A scope with the same logical identity already exists");
        }

        if (scope.getScopeId() == null) {
            scope.setScopeId(UUID.randomUUID());
        }
        return scopeRepository.save(scope);
    }

    private void requireApplication(ConfigurationScopeEntity scope) {
        UUID applicationId = scope.getApplicationId();
        var application = applicationId == null ? java.util.Optional.<com.walmart.cms.dal.entity.ApplicationEntity>empty()
                : applicationRepository.findById(applicationId);
        boolean belongsToOrganization = application.isPresent()
                && projectRepository.existsByProjectIdAndOrganizationId(
                        application.get().getProjectId(), scope.getOrganizationId());
        if (!belongsToOrganization) {
            throw new InvalidConfigurationScopeException(
                    "applicationId must identify an application in the specified organization");
        }
    }

    private void requireNull(Object value, String field) {
        if (value != null) {
            throw new InvalidConfigurationScopeException(
                    field + " must be null for this scope type");
        }
    }

    private void requireValue(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidConfigurationScopeException(field + " is required");
        }
    }
}
