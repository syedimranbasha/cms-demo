package com.walmart.cms.dal.repository;

import com.walmart.cms.dal.entity.ConfigurationScopeEntity;
import com.walmart.cms.dal.entity.ConfigurationVersionEntity;
import com.walmart.cms.dal.entity.TestEntities;
import com.walmart.cms.dal.repository.*;

import com.walmart.cms.dal.exception.StaleConfigurationVersionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ConfigurationVersionRepositoryImplTest {
    @Mock ConfigurationScopeRepository scopes;
    @Mock ConfigurationVersionRepository versions;
    private ConfigurationVersionRepositoryImpl repository;
    private UUID scopeId;

    @BeforeEach void setUp() {
        repository = new ConfigurationVersionRepositoryImpl(scopes, versions);
        scopeId = UUID.randomUUID();
        lenient().when(scopes.findFirstByScopeId(scopeId)).thenReturn(Optional.of(new ConfigurationScopeEntity()));
    }

    @Test void createsFirstVersionWithNumberOne() {
        ConfigurationVersionEntity input = version();
        when(versions.findTopByScopeIdOrderByVersionNumberDesc(scopeId)).thenReturn(Optional.empty());
        when(versions.save(input)).thenReturn(input);
        ConfigurationVersionEntity saved = repository.createNextVersion(scopeId, null, input);
        assertThat(saved.getVersionId()).isNotNull();
        assertThat(saved.getScopeId()).isEqualTo(scopeId);
        assertThat(saved.getVersionNumber()).isEqualTo(1);
        verify(versions).save(input);
    }

    @Test void createsNextVersionWhenExpectedBaseMatchesLatest() {
        UUID latestId = UUID.randomUUID();
        ConfigurationVersionEntity latest = version();
        latest.setVersionId(latestId);
        latest.setVersionNumber(7);
        when(versions.findTopByScopeIdOrderByVersionNumberDesc(scopeId)).thenReturn(Optional.of(latest));
        ConfigurationVersionEntity input = version();
        when(versions.save(input)).thenReturn(input);
        repository.createNextVersion(scopeId, latestId, input);
        assertThat(input.getVersionNumber()).isEqualTo(8);
        assertThat(input.getScopeId()).isEqualTo(scopeId);
    }

    @Test void rejectsStaleExpectedBase() {
        UUID actualId = UUID.randomUUID();
        ConfigurationVersionEntity latest = version();
        latest.setVersionId(actualId);
        latest.setVersionNumber(2);
        when(versions.findTopByScopeIdOrderByVersionNumberDesc(scopeId)).thenReturn(Optional.of(latest));
        assertThatThrownBy(() -> repository.createNextVersion(scopeId, UUID.randomUUID(), version()))
                .isInstanceOf(StaleConfigurationVersionException.class);
        verify(versions, never()).save(any());
    }

    @Test void rejectsNullExpectedBaseWhenVersionAlreadyExists() {
        ConfigurationVersionEntity latest = version();
        latest.setVersionId(UUID.randomUUID());
        latest.setVersionNumber(1);
        when(versions.findTopByScopeIdOrderByVersionNumberDesc(scopeId)).thenReturn(Optional.of(latest));
        assertThatThrownBy(() -> repository.createNextVersion(scopeId, null, version()))
                .isInstanceOf(StaleConfigurationVersionException.class);
    }

    @Test void rejectsUnknownScope() {
        when(scopes.findFirstByScopeId(scopeId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> repository.createNextVersion(scopeId, null, version()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Configuration scope not found");
        verifyNoInteractions(versions);
    }

    @Test void rejectsNullScopeIdAndNullVersion() {
        assertThatThrownBy(() -> repository.createNextVersion(null, null, version()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> repository.createNextVersion(scopeId, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private ConfigurationVersionEntity version() {
        ConfigurationVersionEntity entity = TestEntities.version();
        entity.setConfigDocument(Map.of("enabled", true));
        entity.setChecksum("sha256-test");
        entity.setChangeSummary("Unit test version");
        return entity;
    }
}
