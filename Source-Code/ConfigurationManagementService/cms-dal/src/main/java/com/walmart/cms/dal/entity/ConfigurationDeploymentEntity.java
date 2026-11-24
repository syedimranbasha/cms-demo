package com.walmart.cms.dal.entity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.Map;
import java.util.UUID;
@Entity @Table(name="config_deployments")
public class ConfigurationDeploymentEntity {
 @Id @Column(name="deployment_id") private UUID deploymentId;
 @Column(name="application_id", nullable=false) private UUID applicationId;
 @Column(name="environment_name", nullable=false, length=100) private String environmentName;
 @Column(name="region_code", nullable=false, length=100) private String regionCode;
 @JdbcTypeCode(SqlTypes.JSON) @Column(name="baseline_version_set", nullable=false, columnDefinition="jsonb") private Map<String,String> baselineVersionSet;
 @JdbcTypeCode(SqlTypes.JSON) @Column(name="candidate_version_set", nullable=false, columnDefinition="jsonb") private Map<String,String> candidateVersionSet;
 @Column(nullable=false, length=30) private String status;
 @Column(name="current_stage_id") private UUID currentStageId;
 protected ConfigurationDeploymentEntity() {}
 public UUID getDeploymentId(){return deploymentId;} public UUID getApplicationId(){return applicationId;} public String getEnvironmentName(){return environmentName;} public String getRegionCode(){return regionCode;}
 public Map<String,String> getBaselineVersionSet(){return baselineVersionSet;} public Map<String,String> getCandidateVersionSet(){return candidateVersionSet;}
 public String getStatus(){return status;} public UUID getCurrentStageId(){return currentStageId;}
 public void setDeploymentId(UUID v){deploymentId=v;} public void setApplicationId(UUID v){applicationId=v;} public void setEnvironmentName(String v){environmentName=v;} public void setRegionCode(String v){regionCode=v;}
 public void setBaselineVersionSet(Map<String,String> v){baselineVersionSet=v;} public void setCandidateVersionSet(Map<String,String> v){candidateVersionSet=v;} public void setStatus(String v){status=v;} public void setCurrentStageId(UUID v){currentStageId=v;}
}
