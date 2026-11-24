package com.walmart.cms.dal.repository;

import com.walmart.cms.dal.entity.ApplicationEntity;
import com.walmart.cms.dal.entity.ConfigurationScopeEntity;
import com.walmart.cms.dal.repository.*;

import com.walmart.cms.dal.exception.DuplicateConfigurationScopeException;
import com.walmart.cms.dal.exception.InvalidConfigurationScopeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ConfigurationScopeRepositoryImplTest {
    @Mock OrganizationRepository organizations;
    @Mock ApplicationRepository applications;
    @Mock ProjectRepository projects;
    @Mock ConfigurationScopeRepository scopes;
    private ConfigurationScopeRepositoryImpl repository;
    private UUID organizationId;

    @BeforeEach void setUp() {
        repository = new ConfigurationScopeRepositoryImpl(organizations, applications, projects, scopes);
        organizationId = UUID.randomUUID();
        lenient().when(organizations.existsById(organizationId)).thenReturn(true);
    }

    @Test void createsGlobalScopeAndNormalizesType() {
        ConfigurationScopeEntity scope = scope("global", null, null, null);
        when(scopes.existsByOrganizationIdAndScopeTypeAndEnvironmentNameAndApplicationIdAndRegionCode(
                organizationId, "GLOBAL", null, null, null)).thenReturn(false);
        when(scopes.save(scope)).thenReturn(scope);

        assertThat(repository.createValidated(scope)).isSameAs(scope);
        assertThat(scope.getScopeType()).isEqualTo("GLOBAL");
        assertThat(scope.getScopeId()).isNotNull();
        verify(scopes).save(scope);
    }

    @Test void createsEnvironmentScope() {
        ConfigurationScopeEntity scope = scope("ENVIRONMENT", "PROD", null, null);
        when(scopes.existsByOrganizationIdAndScopeTypeAndEnvironmentNameAndApplicationIdAndRegionCode(
                organizationId, "ENVIRONMENT", "PROD", null, null)).thenReturn(false);
        when(scopes.save(scope)).thenReturn(scope);
        assertThat(repository.createValidated(scope)).isSameAs(scope);
    }

    @Test void createsApplicationScopeWhenApplicationBelongsToOrganization() {
        UUID appId = UUID.randomUUID();
        ApplicationEntity app = new ApplicationEntity();
        UUID projectId = UUID.randomUUID();
        app.setProjectId(projectId);
        ConfigurationScopeEntity scope = scope("APPLICATION", "PROD", appId, null);
        when(applications.findById(appId)).thenReturn(Optional.of(app));
        when(projects.existsByProjectIdAndOrganizationId(projectId, organizationId)).thenReturn(true);
        when(scopes.existsByOrganizationIdAndScopeTypeAndEnvironmentNameAndApplicationIdAndRegionCode(
                organizationId, "APPLICATION", "PROD", appId, null)).thenReturn(false);
        when(scopes.save(scope)).thenReturn(scope);
        assertThat(repository.createValidated(scope)).isSameAs(scope);
    }

    @Test void createsRegionScopeWhenApplicationBelongsToOrganization() {
        UUID appId = UUID.randomUUID();
        ApplicationEntity app = new ApplicationEntity();
        UUID projectId = UUID.randomUUID();
        app.setProjectId(projectId);
        ConfigurationScopeEntity scope = scope("REGION", "PROD", appId, "us-central1");
        when(applications.findById(appId)).thenReturn(Optional.of(app));
        when(projects.existsByProjectIdAndOrganizationId(projectId, organizationId)).thenReturn(true);
        when(scopes.existsByOrganizationIdAndScopeTypeAndEnvironmentNameAndApplicationIdAndRegionCode(
                organizationId, "REGION", "PROD", appId, "us-central1")).thenReturn(false);
        when(scopes.save(scope)).thenReturn(scope);
        assertThat(repository.createValidated(scope)).isSameAs(scope);
    }

    @Test void rejectsNullScope() {
        assertThatThrownBy(() -> repository.createValidated(null))
                .isInstanceOf(InvalidConfigurationScopeException.class);
    }

    @Test void rejectsMissingOrUnknownOrganization() {
        ConfigurationScopeEntity missing = scope("GLOBAL", null, null, null);
        missing.setOrganizationId(null);
        assertThatThrownBy(() -> repository.createValidated(missing))
                .isInstanceOf(InvalidConfigurationScopeException.class);
        when(organizations.existsById(organizationId)).thenReturn(false);
        assertThatThrownBy(() -> repository.createValidated(scope("GLOBAL", null, null, null)))
                .isInstanceOf(InvalidConfigurationScopeException.class);
    }

    @Test void rejectsUnsupportedScopeType() {
        assertThatThrownBy(() -> repository.createValidated(scope("UNKNOWN", null, null, null)))
                .isInstanceOf(InvalidConfigurationScopeException.class);
    }

    @Test void rejectsInvalidScopeFieldCombinations() {
        assertInvalid(scope("GLOBAL", "PROD", null, null));
        assertInvalid(scope("ENVIRONMENT", null, null, null));
        assertInvalid(scope("ENVIRONMENT", "PROD", UUID.randomUUID(), null));
        assertInvalid(scope("APPLICATION", "PROD", null, null));
        assertInvalid(scope("APPLICATION", "PROD", UUID.randomUUID(), "us-central1"));
        assertInvalid(scope("REGION", "PROD", UUID.randomUUID(), " "));
    }

    @Test void rejectsApplicationFromAnotherOrganization() {
        UUID appId = UUID.randomUUID();
        ApplicationEntity app = new ApplicationEntity();
        app.setProjectId(UUID.randomUUID());
        when(applications.findById(appId)).thenReturn(Optional.of(app));
        when(projects.existsByProjectIdAndOrganizationId(app.getProjectId(), organizationId)).thenReturn(false);
        assertThatThrownBy(() -> repository.createValidated(scope("APPLICATION", "PROD", appId, null)))
                .isInstanceOf(InvalidConfigurationScopeException.class);
    }

    @Test void rejectsDuplicateLogicalScope() {
        ConfigurationScopeEntity scope = scope("GLOBAL", null, null, null);
        when(scopes.existsByOrganizationIdAndScopeTypeAndEnvironmentNameAndApplicationIdAndRegionCode(
                organizationId, "GLOBAL", null, null, null)).thenReturn(true);
        assertThatThrownBy(() -> repository.createValidated(scope))
                .isInstanceOf(DuplicateConfigurationScopeException.class);
        verify(scopes, never()).save(any());
    }

    private void assertInvalid(ConfigurationScopeEntity scope) {
        assertThatThrownBy(() -> repository.createValidated(scope))
                .isInstanceOf(InvalidConfigurationScopeException.class);
    }

    private ConfigurationScopeEntity scope(String type, String environment, UUID applicationId, String region) {
        ConfigurationScopeEntity entity = new ConfigurationScopeEntity();
        entity.setOrganizationId(organizationId);
        entity.setScopeType(type);
        entity.setEnvironmentName(environment);
        entity.setApplicationId(applicationId);
        entity.setRegionCode(region);
        return entity;
    }
}
