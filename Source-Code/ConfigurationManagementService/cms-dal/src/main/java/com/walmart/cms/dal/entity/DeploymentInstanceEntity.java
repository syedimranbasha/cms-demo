package com.walmart.cms.dal.entity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Map;
@Entity @Table(name="deployment_instance")
public class DeploymentInstanceEntity {
 @EmbeddedId private DeploymentInstanceId id;
 @JdbcTypeCode(SqlTypes.JSON) @Column(name="version_set", nullable=false, columnDefinition="jsonb") private Map<String,String> versionSet;
 @Column(nullable=false, length=30) private String status;
 @Column(name="acknowledged_at") private Instant acknowledgedAt;
 protected DeploymentInstanceEntity() {}
 public DeploymentInstanceId getId(){return id;} public Map<String,String> getVersionSet(){return versionSet;} public String getStatus(){return status;} public Instant getAcknowledgedAt(){return acknowledgedAt;}
 public void setId(DeploymentInstanceId v){id=v;} public void setVersionSet(Map<String,String> v){versionSet=v;} public void setStatus(String v){status=v;} public void setAcknowledgedAt(Instant v){acknowledgedAt=v;}
}
