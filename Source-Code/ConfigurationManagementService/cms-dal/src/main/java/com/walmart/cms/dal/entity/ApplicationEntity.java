package com.walmart.cms.dal.entity;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="applications")
public class ApplicationEntity {
 @Id @Column(name="application_id") private UUID applicationId;
 @Column(name="project_id", nullable=false) private UUID projectId;
 @Column(nullable=false, length=200) private String name;
 public ApplicationEntity() {}
 public UUID getApplicationId(){return applicationId;} public UUID getProjectId(){return projectId;} public String getName(){return name;}
 public void setApplicationId(UUID v){applicationId=v;} public void setProjectId(UUID v){projectId=v;} public void setName(String v){name=v;}
}
