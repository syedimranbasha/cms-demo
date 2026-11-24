package com.walmart.cms.dal.entity;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="config_scopes")
public class ConfigurationScopeEntity {
 @Id @Column(name="scope_id") private UUID scopeId;
 @Column(name="organization_id", nullable=false) private UUID organizationId;
 @Column(name="scope_type", nullable=false, length=20) private String scopeType;
 @Column(name="environment_name", length=100) private String environmentName;
 @Column(name="application_id") private UUID applicationId;
 @Column(name="region_code", length=100) private String regionCode;
 @Column(name="active_version_id") private UUID activeVersionId;
 public ConfigurationScopeEntity() {}
 public UUID getScopeId(){return scopeId;} public UUID getOrganizationId(){return organizationId;} public String getScopeType(){return scopeType;}
 public String getEnvironmentName(){return environmentName;} public UUID getApplicationId(){return applicationId;} public String getRegionCode(){return regionCode;} public UUID getActiveVersionId(){return activeVersionId;}
 public void setScopeId(UUID v){scopeId=v;} public void setOrganizationId(UUID v){organizationId=v;} public void setScopeType(String v){scopeType=v;}
 public void setEnvironmentName(String v){environmentName=v;} public void setApplicationId(UUID v){applicationId=v;} public void setRegionCode(String v){regionCode=v;} public void setActiveVersionId(UUID v){activeVersionId=v;}
}
