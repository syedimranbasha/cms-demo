package com.walmart.cms.dal.entity;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="projects")
public class ProjectEntity {
 @Id @Column(name="project_id") private UUID projectId;
 @Column(name="organization_id", nullable=false) private UUID organizationId;
 @Column(nullable=false, length=200) private String name;
 public ProjectEntity() {}
 public UUID getProjectId(){return projectId;} public UUID getOrganizationId(){return organizationId;} public String getName(){return name;}
 public void setProjectId(UUID v){projectId=v;} public void setOrganizationId(UUID v){organizationId=v;} public void setName(String v){name=v;}
}
