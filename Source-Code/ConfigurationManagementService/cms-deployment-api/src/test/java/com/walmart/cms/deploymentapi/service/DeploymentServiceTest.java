package com.walmart.cms.deploymentapi.service;
import com.walmart.cms.dal.repository.*;
import com.walmart.cms.cache.service.EffectiveConfigurationCache;
import com.walmart.cms.dal.entity.*;
import com.walmart.cms.deploymentapi.dto.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class DeploymentServiceTest {
 ApplicationRepository apps=mock(ApplicationRepository.class); ProjectRepository projects=mock(ProjectRepository.class);
 OrganizationRepository orgs=mock(OrganizationRepository.class); ConfigurationScopeRepository scopes=mock(ConfigurationScopeRepository.class);
 ConfigurationVersionRepository versions=mock(ConfigurationVersionRepository.class);
 ConfigurationDeploymentRepository deployments=mock(ConfigurationDeploymentRepository.class);
 DeploymentStageRepository stages=mock(DeploymentStageRepository.class);
 EffectiveConfigurationCache cache=mock(EffectiveConfigurationCache.class);
 DeploymentService service=new DeploymentService(apps,projects,orgs,scopes,versions,deployments,stages,cache);
 @Test void rejectsCrossTenantApplication() {
  UUID org=UUID.randomUUID(), appId=UUID.randomUUID(), projectId=UUID.randomUUID();
  ApplicationEntity a=mock(ApplicationEntity.class);when(a.getProjectId()).thenReturn(projectId);
  ProjectEntity p=mock(ProjectEntity.class);when(p.getOrganizationId()).thenReturn(UUID.randomUUID());
  when(apps.findById(appId)).thenReturn(Optional.of(a));when(projects.findById(projectId)).thenReturn(Optional.of(p));when(orgs.existsById(org)).thenReturn(true);
  var req=new CreateDeploymentRequest("PROD","us-central1",List.of(new CreateDeploymentRequest.StageRequest(1,100)));
  assertThrows(NoSuchElementException.class,()->service.createDeployment(org,appId,req));
 }
}
