package com.walmart.cms.managementapi.controller;

import com.walmart.cms.managementapi.dto.ManagementDtos.*;
import com.walmart.cms.managementapi.service.ManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ManagementController {
    private final ManagementService service;
    public ManagementController(ManagementService service) { this.service = service; }

    // An organization is the tenant boundary in the current data model.
    @PostMapping("/organizations") @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse createOrganization(@Valid @RequestBody CreateOrganizationRequest r) { return service.createOrganization(r); }
    @GetMapping("/organizations/{organizationId}")
    public OrganizationResponse getOrganization(@PathVariable UUID organizationId) { return service.getOrganization(organizationId); }
    @GetMapping("/organizations")
    public List<OrganizationResponse> listOrganizations() { return service.listOrganizations(); }

    @PostMapping("/organizations/{organizationId}/projects") @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(@PathVariable UUID organizationId, @Valid @RequestBody CreateProjectRequest r) { return service.createProject(organizationId,r); }
    @GetMapping("/organizations/{organizationId}/projects/{projectId}")
    public ProjectResponse getProject(@PathVariable UUID organizationId,@PathVariable UUID projectId) { return service.getProject(organizationId,projectId); }
    @GetMapping("/organizations/{organizationId}/projects")
    public List<ProjectResponse> listProjects(@PathVariable UUID organizationId) { return service.listProjects(organizationId); }

    @PostMapping("/organizations/{organizationId}/projects/{projectId}/applications") @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse createApplication(@PathVariable UUID organizationId,@PathVariable UUID projectId,@Valid @RequestBody CreateApplicationRequest r) { return service.createApplication(organizationId,projectId,r); }
    @GetMapping("/organizations/{organizationId}/projects/{projectId}/applications/{applicationId}")
    public ApplicationResponse getApplication(@PathVariable UUID organizationId,@PathVariable UUID projectId,@PathVariable UUID applicationId) { return service.getApplication(organizationId,projectId,applicationId); }
    @GetMapping("/organizations/{organizationId}/projects/{projectId}/applications")
    public List<ApplicationResponse> listApplications(@PathVariable UUID organizationId,@PathVariable UUID projectId) { return service.listApplications(organizationId,projectId); }

    @PostMapping("/principals") @ResponseStatus(HttpStatus.CREATED)
    public PrincipalResponse createPrincipal(@Valid @RequestBody CreatePrincipalRequest r) { return service.createPrincipal(r); }
    @PostMapping("/roles") @ResponseStatus(HttpStatus.CREATED)
    public RoleResponse createRole(@Valid @RequestBody CreateRoleRequest r) { return service.createRole(r); }
    @PostMapping("/permissions") @ResponseStatus(HttpStatus.CREATED)
    public PermissionResponse createPermission(@Valid @RequestBody CreatePermissionRequest r) { return service.createPermission(r); }
    @PostMapping("/roles/{roleId}/permissions") @ResponseStatus(HttpStatus.CREATED)
    public RolePermissionResponse addPermissionToRole(@PathVariable UUID roleId,@Valid @RequestBody CreateRolePermissionRequest r) { return service.addPermissionToRole(roleId,r); }
    @PostMapping("/role-bindings") @ResponseStatus(HttpStatus.CREATED)
    public RoleBindingResponse createRoleBinding(@Valid @RequestBody CreateRoleBindingRequest r) { return service.createRoleBinding(r); }
}
