package com.walmart.cms.dal.entity;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="organizations")
public class OrganizationEntity {
 @Id @Column(name="organization_id") private UUID organizationId;
 @Column(nullable=false, unique=true, length=200) private String name;
 public OrganizationEntity() {}
 public UUID getOrganizationId(){return organizationId;} public String getName(){return name;}
 public void setOrganizationId(UUID v){organizationId=v;} public void setName(String v){name=v;}
}
