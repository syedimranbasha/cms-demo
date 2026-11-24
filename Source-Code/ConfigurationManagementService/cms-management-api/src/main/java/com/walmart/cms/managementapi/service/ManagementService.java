package com.walmart.cms.managementapi.service;

import com.walmart.cms.dal.entity.*;
import com.walmart.cms.dal.repository.*;
import com.walmart.cms.managementapi.dto.ManagementDtos.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ManagementService {
    private final OrganizationRepository organizations;
    private final ProjectRepository projects;
    private final ApplicationRepository applications;
    private final PrincipalRepository principals;
    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final RolePermissionRepository rolePermissions;
    private final RoleBindingRepository roleBindings;

    public ManagementService(OrganizationRepository organizations, ProjectRepository projects,
                             ApplicationRepository applications, PrincipalRepository principals,
                             RoleRepository roles, PermissionRepository permissions,
                             RolePermissionRepository rolePermissions, RoleBindingRepository roleBindings) {
        this.organizations = organizations; this.projects = projects; this.applications = applications;
        this.principals = principals; this.roles = roles; this.permissions = permissions;
        this.rolePermissions = rolePermissions; this.roleBindings = roleBindings;
    }

    public OrganizationResponse createOrganization(CreateOrganizationRequest request) {
        if (organizations.findByName(request.name().trim()).isPresent()) throw new ConflictException("Organization name already exists");
        OrganizationEntity e = new OrganizationEntity(); e.setOrganizationId(UUID.randomUUID()); e.setName(request.name().trim());
        OrganizationEntity saved = organizations.save(e); return new OrganizationResponse(saved.getOrganizationId(), saved.getName());
    }
    @Transactional(readOnly = true)
    public OrganizationResponse getOrganization(UUID id) {
        OrganizationEntity e = organizations.findById(id).orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + id));
        return new OrganizationResponse(e.getOrganizationId(), e.getName());
    }
    @Transactional(readOnly = true)
    public List<OrganizationResponse> listOrganizations() { return organizations.findAllByOrderByName().stream().map(e -> new OrganizationResponse(e.getOrganizationId(),e.getName())).toList(); }

    public ProjectResponse createProject(UUID organizationId, CreateProjectRequest request) {
        requireOrganization(organizationId);
        ProjectEntity e = new ProjectEntity(); e.setProjectId(UUID.randomUUID()); e.setOrganizationId(organizationId); e.setName(request.name().trim());
        ProjectEntity saved = projects.save(e); return new ProjectResponse(saved.getProjectId(), saved.getOrganizationId(), saved.getName());
    }
    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID organizationId, UUID projectId) {
        ProjectEntity e = projects.findByProjectIdAndOrganizationId(projectId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found in organization: " + projectId));
        return new ProjectResponse(e.getProjectId(),e.getOrganizationId(),e.getName());
    }
    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjects(UUID organizationId) {
        requireOrganization(organizationId);
        return projects.findByOrganizationIdOrderByName(organizationId).stream().map(e -> new ProjectResponse(e.getProjectId(),e.getOrganizationId(),e.getName())).toList();
    }

    public ApplicationResponse createApplication(UUID organizationId, UUID projectId, CreateApplicationRequest request) {
        requireProject(organizationId, projectId);
        ApplicationEntity e = new ApplicationEntity(); e.setApplicationId(UUID.randomUUID()); e.setProjectId(projectId); e.setName(request.name().trim());
        ApplicationEntity saved = applications.save(e); return new ApplicationResponse(saved.getApplicationId(),saved.getProjectId(),saved.getName());
    }
    @Transactional(readOnly = true)
    public ApplicationResponse getApplication(UUID organizationId, UUID projectId, UUID applicationId) {
        requireProject(organizationId, projectId);
        ApplicationEntity e = applications.findByApplicationIdAndProjectId(applicationId,projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found in project: " + applicationId));
        return new ApplicationResponse(e.getApplicationId(),e.getProjectId(),e.getName());
    }
    @Transactional(readOnly = true)
    public List<ApplicationResponse> listApplications(UUID organizationId, UUID projectId) {
        requireProject(organizationId,projectId);
        return applications.findByProjectIdOrderByName(projectId).stream().map(e -> new ApplicationResponse(e.getApplicationId(),e.getProjectId(),e.getName())).toList();
    }

    public PrincipalResponse createPrincipal(CreatePrincipalRequest request) {
        if (principals.findByPrincipalTypeAndExternalId(request.principalType().trim(),request.externalId().trim()).isPresent()) throw new ConflictException("Principal already exists for principalType and externalId");
        PrincipalEntity e=new PrincipalEntity(); e.setPrincipalId(UUID.randomUUID()); e.setPrincipalType(request.principalType().trim()); e.setExternalId(request.externalId().trim()); e.setName(request.name());
        PrincipalEntity saved=principals.save(e); return new PrincipalResponse(saved.getPrincipalId(),saved.getPrincipalType(),saved.getExternalId(),saved.getName());
    }
    public RoleResponse createRole(CreateRoleRequest request) {
        if(roles.findByRoleName(request.roleName().trim()).isPresent()) throw new ConflictException("Role name already exists");
        RoleEntity e=new RoleEntity(); e.setRoleId(UUID.randomUUID()); e.setRoleName(request.roleName().trim()); e.setDescription(request.description());
        RoleEntity saved=roles.save(e); return new RoleResponse(saved.getRoleId(),saved.getRoleName(),saved.getDescription());
    }
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        if(permissions.findByPermissionName(request.permissionName().trim()).isPresent()) throw new ConflictException("Permission name already exists");
        PermissionEntity e=new PermissionEntity(); e.setPermissionId(UUID.randomUUID()); e.setPermissionName(request.permissionName().trim()); e.setDescription(request.description());
        PermissionEntity saved=permissions.save(e); return new PermissionResponse(saved.getPermissionId(),saved.getPermissionName(),saved.getDescription());
    }
    public RolePermissionResponse addPermissionToRole(UUID roleId, CreateRolePermissionRequest request) {
        if(roles.findById(roleId).isEmpty()) throw new ResourceNotFoundException("Role not found: " + roleId);
        if(permissions.findById(request.permissionId()).isEmpty()) throw new ResourceNotFoundException("Permission not found: " + request.permissionId());
        RolePermissionId id=new RolePermissionId(roleId,request.permissionId());
        if(rolePermissions.existsById(id)) throw new ConflictException("Permission is already assigned to role");
        RolePermissionEntity e=new RolePermissionEntity(); e.setId(id); rolePermissions.save(e);
        return new RolePermissionResponse(roleId,request.permissionId());
    }
    public RoleBindingResponse createRoleBinding(CreateRoleBindingRequest request) {
        if(principals.findById(request.principalId()).isEmpty()) throw new ResourceNotFoundException("Principal not found: " + request.principalId());
        if(roles.findById(request.roleId()).isEmpty()) throw new ResourceNotFoundException("Role not found: " + request.roleId());
        requireOrganization(request.organizationId());
        if(request.applicationId()!=null && request.projectId()==null) throw new IllegalArgumentException("projectId is required when applicationId is supplied");
        if(request.projectId()!=null) requireProject(request.organizationId(),request.projectId());
        if(request.applicationId()!=null && !applications.existsByApplicationIdAndProjectId(request.applicationId(),request.projectId())) throw new ResourceNotFoundException("Application not found in project: " + request.applicationId());
        RoleBindingEntity e=new RoleBindingEntity(); e.setBindingId(UUID.randomUUID()); e.setPrincipalId(request.principalId()); e.setRoleId(request.roleId()); e.setOrganizationId(request.organizationId()); e.setProjectId(request.projectId()); e.setApplicationId(request.applicationId());
        RoleBindingEntity saved=roleBindings.save(e);
        return new RoleBindingResponse(saved.getBindingId(),saved.getPrincipalId(),saved.getRoleId(),saved.getOrganizationId(),saved.getProjectId(),saved.getApplicationId());
    }

    private OrganizationEntity requireOrganization(UUID id) { return organizations.findById(id).orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + id)); }
    private ProjectEntity requireProject(UUID organizationId, UUID projectId) {
        requireOrganization(organizationId);
        return projects.findByProjectIdAndOrganizationId(projectId,organizationId).orElseThrow(() -> new ResourceNotFoundException("Project not found in organization: " + projectId));
    }

    @SuppressWarnings("serial")
    public static class ResourceNotFoundException extends RuntimeException { public ResourceNotFoundException(String m){super(m);} }
    @SuppressWarnings("serial")
    public static class ConflictException extends RuntimeException { public ConflictException(String m){super(m);} }
}
