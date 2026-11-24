package com.walmart.cms.dal.entity;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="deployment_stages", uniqueConstraints=@UniqueConstraint(name="uq_deployment_stage_number", columnNames={"deployment_id","stage_number"}))
public class DeploymentStageEntity {
 @Id @Column(name="stage_id") private UUID stageId;
 @Column(name="deployment_id", nullable=false) private UUID deploymentId;
 @Column(name="stage_number", nullable=false) private Integer stageNumber;
 @Column(name="target_percentage", nullable=false) private Integer targetPercentage;
 @Column(nullable=false, length=30) private String status;
 protected DeploymentStageEntity() {}
 public UUID getStageId(){return stageId;} public UUID getDeploymentId(){return deploymentId;} public Integer getStageNumber(){return stageNumber;} public Integer getTargetPercentage(){return targetPercentage;} public String getStatus(){return status;}
 public void setStageId(UUID v){stageId=v;} public void setDeploymentId(UUID v){deploymentId=v;} public void setStageNumber(Integer v){stageNumber=v;} public void setTargetPercentage(Integer v){targetPercentage=v;} public void setStatus(String v){status=v;}
}
