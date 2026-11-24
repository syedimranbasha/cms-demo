package com.walmart.cms.coreapi.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmart.cms.cache.service.*;
import com.walmart.cms.cache.model.*;
import com.walmart.cms.dal.repository.*;
import com.walmart.cms.dal.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class CoreConfigurationServiceTest {
 OrganizationRepository orgs=mock(OrganizationRepository.class);
 ProjectRepository projects=mock(ProjectRepository.class);
 ApplicationRepository apps=mock(ApplicationRepository.class);
 ConfigurationScopeRepository scopes=mock(ConfigurationScopeRepository.class);
 ConfigurationVersionRepository versions=mock(ConfigurationVersionRepository.class);
 ConfigurationDeploymentRepository deployments=mock(ConfigurationDeploymentRepository.class);
 DeploymentStageRepository deploymentStages=mock(DeploymentStageRepository.class);
 EffectiveConfigurationCache cache=mock(EffectiveConfigurationCache.class);
 EffectiveConfigurationCacheKeyFactory keys=mock(EffectiveConfigurationCacheKeyFactory.class);
 ConfigurationMergeResolver merger=new ConfigurationMergeResolver();
 CoreConfigurationService service=new CoreConfigurationService(orgs,projects,apps,scopes,versions,
  deployments,deploymentStages,cache,keys,merger,new ObjectMapper());
 private EffectiveConfigurationCacheKey requestKey(UUID org, UUID appId) {
  return new EffectiveConfigurationCacheKey(org.toString(),appId.toString(),"PROD","us-central1","instance","fingerprint");
 }
 @Test void usesCacheOnlyAfterResolvingCurrentVersionSet() {
  UUID org=UUID.randomUUID(), appId=UUID.randomUUID(), projectId=UUID.randomUUID();
  ApplicationEntity app=new ApplicationEntity(); app.setProjectId(projectId);
  ProjectEntity project=new ProjectEntity(); project.setOrganizationId(org);
  when(apps.findById(appId)).thenReturn(Optional.of(app));
  when(projects.findById(projectId)).thenReturn(Optional.of(project));
  when(orgs.existsById(org)).thenReturn(true);
  when(scopes.findByOrganizationIdAndScopeTypeAndEnvironmentNameAndApplicationIdAndRegionCode(
    org,"GLOBAL",null,null,null)).thenReturn(Optional.empty());
  when(scopes.findByOrganizationIdAndScopeTypeAndEnvironmentNameAndApplicationIdAndRegionCode(
    org,"ENVIRONMENT","PROD",null,null)).thenReturn(Optional.empty());
  when(scopes.findByOrganizationIdAndScopeTypeAndEnvironmentNameAndApplicationIdAndRegionCode(
    org,"APPLICATION","PROD",appId,null)).thenReturn(Optional.empty());
  when(scopes.findByOrganizationIdAndScopeTypeAndEnvironmentNameAndApplicationIdAndRegionCode(
    org,"REGION","PROD",appId,"us-central1")).thenReturn(Optional.empty());
  when(deployments.findFirstByApplicationIdAndEnvironmentNameAndRegionCodeAndStatusOrderByDeploymentIdDesc(
    appId,"PROD","us-central1","IN_PROGRESS")).thenReturn(Optional.empty());
  EffectiveConfigurationCacheKey key=requestKey(org,appId);
  when(keys.create(eq(org),eq(appId),eq("PROD"),eq("us-central1"),eq("instance"),anyMap())).thenReturn(key);
  EffectiveConfiguration cached=new EffectiveConfiguration(Map.of("currency","USD"),
    new ConfigurationVersionMetadata("g1","e1","a1","r1"),java.time.Instant.now());
  when(cache.get(key)).thenReturn(Optional.of(cached));
  var response=service.retrieveEffectiveConfiguration(org,appId,"PROD","us-central1","instance-1");
  assertEquals("USD",response.effectiveConfig().get("currency"));
  verify(apps).findById(appId);
  verify(deployments).findFirstByApplicationIdAndEnvironmentNameAndRegionCodeAndStatusOrderByDeploymentIdDesc(
    appId,"PROD","us-central1","IN_PROGRESS");
  verify(cache,never()).put(any(),any(),any());
 }
 @Test void rejectsApplicationOutsideOrganization() {
  UUID org=UUID.randomUUID(), appId=UUID.randomUUID(), projectId=UUID.randomUUID();
  ApplicationEntity app=mock(ApplicationEntity.class); when(app.getProjectId()).thenReturn(projectId);
  ProjectEntity p=mock(ProjectEntity.class); when(p.getOrganizationId()).thenReturn(UUID.randomUUID());
  when(apps.findById(appId)).thenReturn(Optional.of(app)); when(projects.findById(projectId)).thenReturn(Optional.of(p));
  when(orgs.existsById(org)).thenReturn(true);
  when(keys.create(eq(org),eq(appId),eq("PROD"),eq("us-central1"),eq("instance"),anyMap())).thenReturn(requestKey(org,appId));
  when(cache.get(any())).thenReturn(Optional.empty());
  assertThrows(NoSuchElementException.class,()->service.retrieveEffectiveConfiguration(org,appId,"PROD","us-central1","i-1"));
 }

 @Test void usesCandidateVersionForInstanceInInitiatedRollout() {
  UUID org=UUID.randomUUID(), appId=UUID.randomUUID(), projectId=UUID.randomUUID();
  ApplicationEntity app=new ApplicationEntity(); app.setProjectId(projectId);
  ProjectEntity project=new ProjectEntity(); project.setOrganizationId(org);
  when(apps.findById(appId)).thenReturn(Optional.of(app));
  when(projects.findById(projectId)).thenReturn(Optional.of(project));
  when(orgs.existsById(org)).thenReturn(true);
  when(keys.create(eq(org),eq(appId),eq("PROD"),eq("us-central1"),eq("instance"),anyMap()))
    .thenReturn(requestKey(org,appId));
  when(cache.get(any())).thenReturn(Optional.empty());

  UUID deploymentId=UUID.randomUUID(), stageId=UUID.randomUUID();
  ConfigurationDeploymentEntity deployment=mock(ConfigurationDeploymentEntity.class);
  when(deployment.getDeploymentId()).thenReturn(deploymentId);
  when(deployment.getApplicationId()).thenReturn(appId);
  when(deployment.getEnvironmentName()).thenReturn("PROD");
  when(deployment.getRegionCode()).thenReturn("us-central1");
  when(deployment.getCurrentStageId()).thenReturn(stageId);
  when(deployment.getBaselineVersionSet()).thenReturn(Map.of("global","NONE","environment","NONE","application","NONE","region","NONE"));
  UUID globalId=UUID.randomUUID(), envId=UUID.randomUUID(), appVersionId=UUID.randomUUID(), regionId=UUID.randomUUID();
  when(deployment.getCandidateVersionSet()).thenReturn(Map.of("global",globalId.toString(),"environment",envId.toString(),
    "application",appVersionId.toString(),"region",regionId.toString()));
  when(deployments.findFirstByApplicationIdAndEnvironmentNameAndRegionCodeAndStatusOrderByDeploymentIdDesc(
    appId,"PROD","us-central1","IN_PROGRESS")).thenReturn(Optional.of(deployment));
  DeploymentStageEntity currentStage=mock(DeploymentStageEntity.class);
  when(currentStage.getTargetPercentage()).thenReturn(100);
  when(deploymentStages.findByStageIdAndDeploymentId(stageId,deploymentId))
    .thenReturn(Optional.of(currentStage));
  // Create and stub the version mocks before starting repository stubbing.
  // Calling version(...) inside thenReturn(...) starts nested Mockito stubbing
  // while the outer stubbing is still unfinished.
  ConfigurationVersionEntity globalVersion =
    version(globalId,Map.of("featureFlags",Map.of("global",true)));
  ConfigurationVersionEntity environmentVersion =
    version(envId,Map.of("featureFlags",Map.of("environment",true)));
  ConfigurationVersionEntity applicationVersion =
    version(appVersionId,Map.of("featureFlags",Map.of("application",true)));
  ConfigurationVersionEntity regionVersion =
    version(regionId,Map.of("featureFlags",Map.of("region",true)));

  when(versions.findById(globalId)).thenReturn(Optional.of(globalVersion));
  when(versions.findById(envId)).thenReturn(Optional.of(environmentVersion));
  when(versions.findById(appVersionId)).thenReturn(Optional.of(applicationVersion));
  when(versions.findById(regionId)).thenReturn(Optional.of(regionVersion));

  var response=service.retrieveEffectiveConfiguration(org,appId,"PROD","us-central1","cli-instance-01");
  assertEquals(Map.of("global",true,"environment",true,"application",true,"region",true),
    response.effectiveConfig().get("featureFlags"));
  assertEquals(globalId.toString(),response.versions().global());
 }

 @Test void rolloutSelectionIsStableAndExpandsWithPercentage() {
  String selected=null, notSelected=null;
  for(int i=0;i<10_000 && (selected==null || notSelected==null);i++) {
   String candidate="instance-"+i;
   if(CoreConfigurationService.isInRollout(candidate,10)) selected=candidate;
   else notSelected=candidate;
  }
  assertNotNull(selected);
  assertNotNull(notSelected);
  assertTrue(CoreConfigurationService.isInRollout(selected,10));
  assertTrue(CoreConfigurationService.isInRollout(selected,50));
  assertTrue(CoreConfigurationService.isInRollout(selected,100));
  assertFalse(CoreConfigurationService.isInRollout(notSelected,10));
  assertEquals(CoreConfigurationService.isInRollout(notSelected,10),
    CoreConfigurationService.isInRollout(notSelected,10));
 }

 private ConfigurationVersionEntity version(UUID id, Map<String,Object> document) {
  ConfigurationVersionEntity version=mock(ConfigurationVersionEntity.class);
  when(version.getVersionId()).thenReturn(id);
  when(version.getConfigDocument()).thenReturn(document);
  return version;
 }
}
