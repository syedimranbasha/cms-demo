package com.walmart.cms.deploymentapi.service;
import com.walmart.cms.deploymentapi.dto.*;
import com.walmart.cms.dal.entity.*;
import com.walmart.cms.dal.repository.*;
import com.walmart.cms.cache.service.EffectiveConfigurationCache;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class DeploymentService {
 private final ApplicationRepository applications;
 private final ProjectRepository projects;
 private final OrganizationRepository organizations;
 private final ConfigurationScopeRepository scopes;
 private final ConfigurationVersionRepository versions;
 private final ConfigurationDeploymentRepository deployments;
 private final DeploymentStageRepository stages;
 private final EffectiveConfigurationCache cache;

 public DeploymentService(ApplicationRepository applications,ProjectRepository projects,OrganizationRepository organizations,
  ConfigurationScopeRepository scopes,ConfigurationVersionRepository versions,ConfigurationDeploymentRepository deployments,
  DeploymentStageRepository stages, EffectiveConfigurationCache cache) {
  this.applications=applications;this.projects=projects;this.organizations=organizations;this.scopes=scopes;
  this.versions=versions;this.deployments=deployments;this.stages=stages;this.cache=cache;
 }

 @Transactional
 public DeploymentResponse createDeployment(UUID orgId,UUID appId,CreateDeploymentRequest request) {
  verifyApplication(orgId,appId);
  Map<String,String> baseline=versionSet(orgId,appId,request.environmentName(),request.regionCode(),false);
  Map<String,String> candidate=versionSet(orgId,appId,request.environmentName(),request.regionCode(),true);
  ConfigurationDeploymentEntity deployment=instantiate(ConfigurationDeploymentEntity.class);
  deployment.setDeploymentId(UUID.randomUUID());deployment.setApplicationId(appId);
  deployment.setEnvironmentName(request.environmentName());deployment.setRegionCode(request.regionCode());
  deployment.setBaselineVersionSet(baseline);deployment.setCandidateVersionSet(candidate);deployment.setStatus("PENDING");
  deployments.save(deployment);
  List<DeploymentStageEntity> saved=new ArrayList<>();
  int previous=0;
  for (CreateDeploymentRequest.StageRequest sr:request.stages().stream().sorted(Comparator.comparingInt(CreateDeploymentRequest.StageRequest::stageNumber)).toList()) {
   if(sr.stageNumber()<=previous || sr.targetPercentage()<1 || sr.targetPercentage()>100)
    throw new IllegalArgumentException("Stages must have unique increasing stage numbers and target percentages from 1 to 100");
   previous=sr.stageNumber();
   DeploymentStageEntity stage=instantiate(DeploymentStageEntity.class);stage.setStageId(UUID.randomUUID());
   stage.setDeploymentId(deployment.getDeploymentId());stage.setStageNumber(sr.stageNumber());
   stage.setTargetPercentage(sr.targetPercentage());stage.setStatus("PENDING");saved.add(stages.save(stage));
  }
  if(saved.get(saved.size()-1).getTargetPercentage()!=100)
   throw new IllegalArgumentException("Final rollout stage must target 100%");
  return response(deployment,saved,null);
 }

 @Transactional
 public DeploymentResponse initiateStage(UUID orgId,UUID deploymentId,UUID stageId,String authenticatedPrincipal,String comment) {
  ConfigurationDeploymentEntity deployment=deployments.findById(deploymentId)
   .orElseThrow(()->new NoSuchElementException("Deployment not found"));
  verifyApplication(orgId,deployment.getApplicationId());
  DeploymentStageEntity stage=stages.findByStageIdAndDeploymentId(stageId,deploymentId)
   .orElseThrow(()->new NoSuchElementException("Deployment stage not found"));
  if(!"PENDING".equals(deployment.getStatus()) && !"IN_PROGRESS".equals(deployment.getStatus()))
   throw new IllegalStateException("Deployment is not eligible for stage activation");
  List<DeploymentStageEntity> ordered=stages.findByDeploymentIdOrderByStageNumber(deploymentId);
  for(DeploymentStageEntity earlier:ordered) {
   if(earlier.getStageNumber()<stage.getStageNumber() && !"COMPLETED".equals(earlier.getStatus()))
    throw new IllegalStateException("Previous rollout stage must be completed before activating this stage");
  }
  stage.setStatus("IN_PROGRESS");stages.save(stage);
  deployment.setCurrentStageId(stageId);deployment.setStatus("IN_PROGRESS");deployments.save(deployment);
  evictAfterCommit(orgId, deployment.getApplicationId(), deployment.getEnvironmentName(), deployment.getRegionCode());
  return response(deployment,stages.findByDeploymentIdOrderByStageNumber(deploymentId),null);
 }

 @Transactional
 public DeploymentResponse rollback(UUID orgId,UUID appId,UUID deploymentId,RollbackRequest request) {
  verifyApplication(orgId,appId);
  ConfigurationDeploymentEntity current=deployments.findByDeploymentIdAndApplicationId(deploymentId,appId)
   .orElseThrow(()->new NoSuchElementException("Deployment not found"));
  ConfigurationDeploymentEntity target=deployments.findByDeploymentIdAndApplicationId(request.targetDeploymentId(),appId)
   .orElseThrow(()->new NoSuchElementException("Target deployment not found"));
  ConfigurationDeploymentEntity rollback=instantiate(ConfigurationDeploymentEntity.class);
  rollback.setDeploymentId(UUID.randomUUID());rollback.setApplicationId(appId);
  rollback.setEnvironmentName(current.getEnvironmentName());rollback.setRegionCode(current.getRegionCode());
  rollback.setBaselineVersionSet(current.getCandidateVersionSet());
  rollback.setCandidateVersionSet(target.getCandidateVersionSet());rollback.setStatus("PENDING");
  deployments.save(rollback);
  evictAfterCommit(orgId, appId, rollback.getEnvironmentName(), rollback.getRegionCode());
  return response(rollback,List.of(),current.getDeploymentId());
 }

 @Transactional(readOnly=true)
 public DeploymentResponse fetchDeployment(UUID orgId,UUID deploymentId) {
  ConfigurationDeploymentEntity d=deployments.findById(deploymentId)
   .orElseThrow(()->new NoSuchElementException("Deployment not found"));
  verifyApplication(orgId,d.getApplicationId());
  return response(d,stages.findByDeploymentIdOrderByStageNumber(deploymentId),null);
 }

 private void evictAfterCommit(UUID orgId, UUID appId, String environment, String region) {
  Runnable eviction = () -> cache.evictScope(orgId.toString(), appId.toString(), environment, region);
  if (TransactionSynchronizationManager.isSynchronizationActive()) {
   TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
    @Override public void afterCommit() { eviction.run(); }
   });
  } else { eviction.run(); }
 }
 private <T> T instantiate(Class<T> type) {
  try {
   var constructor=type.getDeclaredConstructor(); constructor.setAccessible(true); return constructor.newInstance();
  } catch (ReflectiveOperationException e) {
   throw new IllegalStateException("Unable to instantiate DAL entity "+type.getSimpleName(),e);
  }
 }
 private void verifyApplication(UUID orgId,UUID appId) {
  ApplicationEntity app=applications.findById(appId).orElseThrow(()->new NoSuchElementException("Application not found"));
  ProjectEntity p=projects.findById(app.getProjectId()).orElseThrow(()->new NoSuchElementException("Project not found"));
  if(!organizations.existsById(orgId)||!p.getOrganizationId().equals(orgId))
   throw new NoSuchElementException("Application does not belong to organization");
 }
 private Map<String,String> versionSet(UUID orgId,UUID appId,String env,String region,boolean latest) {
  Map<String,String> result=new LinkedHashMap<>();
  putVersion(result,"global",findScope(orgId,"GLOBAL",null,null,null),latest);
  putVersion(result,"environment",findScope(orgId,"ENVIRONMENT",env,null,null),latest);
  putVersion(result,"application",findScope(orgId,"APPLICATION",env,appId,null),latest);
  putVersion(result,"region",findScope(orgId,"REGION",env,appId,region),latest);
  return result;
 }
 private ConfigurationScopeEntity findScope(UUID org,String type,String env,UUID app,String region) {
  return scopes.findByOrganizationIdAndScopeTypeAndEnvironmentNameAndApplicationIdAndRegionCode(org,type,env,app,region).orElse(null);
 }
 private void putVersion(Map<String,String> result,String key,ConfigurationScopeEntity scope,boolean latest) {
  if(scope==null) { result.put(key,"NONE"); return; }
  UUID versionId=latest?versions.findTopByScopeIdOrderByVersionNumberDesc(scope.getScopeId())
    .map(ConfigurationVersionEntity::getVersionId).orElse(null):scope.getActiveVersionId();
  result.put(key,versionId==null?"NONE":versionId.toString());
 }
 private DeploymentResponse response(ConfigurationDeploymentEntity d,List<DeploymentStageEntity> stageList,UUID rollbackOf) {
  return new DeploymentResponse(d.getDeploymentId(),rollbackOf,d.getStatus(),d.getBaselineVersionSet(),d.getCandidateVersionSet(),
   stageList.stream().map(s->new DeploymentResponse.StageResponse(s.getStageId(),s.getStageNumber(),s.getTargetPercentage(),s.getStatus())).toList());
 }
}
