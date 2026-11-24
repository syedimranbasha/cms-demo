package com.walmart.cms.dal.entity;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="role_bindings")
public class RoleBindingEntity {
 @Id @Column(name="binding_id") private UUID bindingId;
 @Column(name="principal_id", nullable=false) private UUID principalId;
 @Column(name="role_id", nullable=false) private UUID roleId;
 @Column(name="organization_id", nullable=false) private UUID organizationId;
 @Column(name="project_id") private UUID projectId;
 @Column(name="application_id") private UUID applicationId;
 public RoleBindingEntity() {}
 public UUID getBindingId(){return bindingId;} public UUID getPrincipalId(){return principalId;} public UUID getRoleId(){return roleId;} public UUID getOrganizationId(){return organizationId;} public UUID getProjectId(){return projectId;} public UUID getApplicationId(){return applicationId;}
 public void setBindingId(UUID v){bindingId=v;} public void setPrincipalId(UUID v){principalId=v;} public void setRoleId(UUID v){roleId=v;} public void setOrganizationId(UUID v){organizationId=v;} public void setProjectId(UUID v){projectId=v;} public void setApplicationId(UUID v){applicationId=v;}
}
