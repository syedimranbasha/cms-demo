package com.walmart.cms.deploymentapi.controller;
import com.walmart.cms.deploymentapi.dto.*;
import com.walmart.cms.deploymentapi.service.DeploymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import java.security.Principal;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}")
public class DeploymentController {
 private final DeploymentService service;
 public DeploymentController(DeploymentService service){this.service=service;}

 @PostMapping("/applications/{appId}/deployments")
 @ResponseStatus(HttpStatus.CREATED)
 public DeploymentResponse create(@PathVariable UUID orgId,@PathVariable UUID appId,
  @Valid @RequestBody CreateDeploymentRequest request){return service.createDeployment(orgId,appId,request);}

 @PostMapping("/deployments/{deploymentId}/stages/{stageId}/start")
 public DeploymentResponse initiate(@PathVariable UUID orgId,@PathVariable UUID deploymentId,@PathVariable UUID stageId,
  @RequestBody(required=false) InitiateStageRequest request,
  Principal authenticatedPrincipal) {
  String actor=authenticatedPrincipal==null?null:authenticatedPrincipal.getName();
  return service.initiateStage(orgId,deploymentId,stageId,actor,request==null?null:request.approvalComment());
 }

 @PostMapping("/applications/{appId}/deployments/{deploymentId}/rollback")
 @ResponseStatus(HttpStatus.CREATED)
 public DeploymentResponse rollback(@PathVariable UUID orgId,@PathVariable UUID appId,@PathVariable UUID deploymentId,
  @Valid @RequestBody RollbackRequest request){return service.rollback(orgId,appId,deploymentId,request);}

 @GetMapping("/deployments/{deploymentId}")
 public DeploymentResponse fetch(@PathVariable UUID orgId,@PathVariable UUID deploymentId){
  return service.fetchDeployment(orgId,deploymentId);
 }
}
