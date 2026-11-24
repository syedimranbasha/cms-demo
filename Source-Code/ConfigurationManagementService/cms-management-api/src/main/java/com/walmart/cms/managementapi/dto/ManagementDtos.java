package com.walmart.cms.managementapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class ManagementDtos {
    private ManagementDtos() {}

    public record CreateOrganizationRequest(@NotBlank @Size(max = 200) String name) {}
    public record OrganizationResponse(UUID organizationId, String name) {}
    public record CreateProjectRequest(@NotBlank @Size(max = 200) String name) {}
    public record ProjectResponse(UUID projectId, UUID organizationId, String name) {}
    public record CreateApplicationRequest(@NotBlank @Size(max = 200) String name) {}
    public record ApplicationResponse(UUID applicationId, UUID projectId, String name) {}

    public record CreatePrincipalRequest(@NotBlank @Size(max = 30) String principalType,
                                         @NotBlank @Size(max = 300) String externalId,
                                         @Size(max = 300) String name) {}
    public record PrincipalResponse(UUID principalId, String principalType, String externalId, String name) {}
    public record CreateRoleRequest(@NotBlank @Size(max = 100) String roleName,
                                    @Size(max = 500) String description) {}
    public record RoleResponse(UUID roleId, String roleName, String description) {}
    public record CreatePermissionRequest(@NotBlank @Size(max = 150) String permissionName,
                                          @Size(max = 500) String description) {}
    public record PermissionResponse(UUID permissionId, String permissionName, String description) {}
    public record CreateRolePermissionRequest(@NotNull UUID permissionId) {}
    public record RolePermissionResponse(UUID roleId, UUID permissionId) {}
    public record CreateRoleBindingRequest(@NotNull UUID principalId, @NotNull UUID roleId,
                                           @NotNull UUID organizationId, UUID projectId, UUID applicationId) {}
    public record RoleBindingResponse(UUID bindingId, UUID principalId, UUID roleId,
                                      UUID organizationId, UUID projectId, UUID applicationId) {}
}
