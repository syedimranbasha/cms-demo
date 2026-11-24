package com.walmart.cms.dal.entity;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="permissions")
public class PermissionEntity {
 @Id @Column(name="permission_id") private UUID permissionId;
 @Column(name="permission_name", nullable=false, unique=true, length=150) private String permissionName;
 @Column(length=500) private String description;
 public PermissionEntity() {}
 public UUID getPermissionId(){return permissionId;} public String getPermissionName(){return permissionName;} public String getDescription(){return description;}
 public void setPermissionId(UUID v){permissionId=v;} public void setPermissionName(String v){permissionName=v;} public void setDescription(String v){description=v;}
}
