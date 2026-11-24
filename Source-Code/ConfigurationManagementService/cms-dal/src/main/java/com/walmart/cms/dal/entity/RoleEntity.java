package com.walmart.cms.dal.entity;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="roles")
public class RoleEntity {
 @Id @Column(name="role_id") private UUID roleId;
 @Column(name="role_name", nullable=false, unique=true, length=100) private String roleName;
 @Column(length=500) private String description;
 public RoleEntity() {}
 public UUID getRoleId(){return roleId;} public String getRoleName(){return roleName;} public String getDescription(){return description;}
 public void setRoleId(UUID v){roleId=v;} public void setRoleName(String v){roleName=v;} public void setDescription(String v){description=v;}
}
