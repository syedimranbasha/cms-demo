package com.walmart.cms.dal.entity;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
@Embeddable
public class DeploymentInstanceId implements Serializable {
 @Column(name="deployment_id") private UUID deploymentId;
 @Column(name="stage_id") private UUID stageId;
 @Column(name="instance_id", length=200) private String instanceId;
 public DeploymentInstanceId() {}
 public DeploymentInstanceId(UUID deploymentId, UUID stageId, String instanceId){this.deploymentId=deploymentId;this.stageId=stageId;this.instanceId=instanceId;}
 public UUID getDeploymentId(){return deploymentId;} public UUID getStageId(){return stageId;} public String getInstanceId(){return instanceId;}
 @Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof DeploymentInstanceId that))return false;return Objects.equals(deploymentId,that.deploymentId)&&Objects.equals(stageId,that.stageId)&&Objects.equals(instanceId,that.instanceId);}
 @Override public int hashCode(){return Objects.hash(deploymentId,stageId,instanceId);}
}
