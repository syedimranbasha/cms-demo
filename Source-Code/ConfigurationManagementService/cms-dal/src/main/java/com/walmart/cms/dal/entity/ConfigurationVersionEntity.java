package com.walmart.cms.dal.entity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.Map;
import java.util.UUID;
@Entity @Table(name="config_versions", uniqueConstraints=@UniqueConstraint(name="uq_config_version_scope_number", columnNames={"scope_id","version_number"}))
public class ConfigurationVersionEntity {
 @Id @Column(name="version_id") private UUID versionId;
 @Column(name="scope_id", nullable=false) private UUID scopeId;
 @Column(name="version_number", nullable=false) private Integer versionNumber;
 @JdbcTypeCode(SqlTypes.JSON) @Column(name="config_document", nullable=false, columnDefinition="jsonb") private Map<String,Object> configDocument;
 @Column(nullable=false, length=128) private String checksum;
 @Column(name="change_summary", length=1000) private String changeSummary;
 protected ConfigurationVersionEntity() {}
 public UUID getVersionId(){return versionId;} public UUID getScopeId(){return scopeId;} public Integer getVersionNumber(){return versionNumber;}
 public Map<String,Object> getConfigDocument(){return configDocument;} public String getChecksum(){return checksum;} public String getChangeSummary(){return changeSummary;}
 public void setVersionId(UUID v){versionId=v;} public void setScopeId(UUID v){scopeId=v;} public void setVersionNumber(Integer v){versionNumber=v;}
 public void setConfigDocument(Map<String,Object> v){configDocument=v;} public void setChecksum(String v){checksum=v;} public void setChangeSummary(String v){changeSummary=v;}
}
