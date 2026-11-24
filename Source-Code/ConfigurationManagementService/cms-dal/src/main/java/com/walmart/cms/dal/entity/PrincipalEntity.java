package com.walmart.cms.dal.entity;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="principals")
public class PrincipalEntity {
 @Id @Column(name="principal_id") private UUID principalId;
 @Column(name="principal_type", nullable=false, length=30) private String principalType;
 @Column(name="external_id", nullable=false, length=300) private String externalId;
 @Column(length=300) private String name;
 public PrincipalEntity() {}
 public UUID getPrincipalId(){return principalId;} public String getPrincipalType(){return principalType;} public String getExternalId(){return externalId;} public String getName(){return name;}
 public void setPrincipalId(UUID v){principalId=v;} public void setPrincipalType(String v){principalType=v;} public void setExternalId(String v){externalId=v;} public void setName(String v){name=v;}
}
