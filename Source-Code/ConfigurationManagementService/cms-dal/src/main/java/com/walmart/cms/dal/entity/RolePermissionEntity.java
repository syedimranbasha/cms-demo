package com.walmart.cms.dal.entity;
import jakarta.persistence.*;
@Entity @Table(name="role_permissions")
public class RolePermissionEntity {
 @EmbeddedId private RolePermissionId id;
 public RolePermissionEntity() {}
 public RolePermissionId getId(){return id;} public void setId(RolePermissionId v){id=v;}
}
