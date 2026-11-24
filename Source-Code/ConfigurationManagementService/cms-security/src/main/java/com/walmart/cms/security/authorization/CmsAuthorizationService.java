package com.walmart.cms.security.authorization;

/**
 * Contract for CMS resource-level authorization.
 *
 * The implementation should resolve the caller's principal using
 * principals.external_id, then evaluate role_bindings, roles,
 * role_permissions and permissions for the requested resource scope.
 *
 * Scope inheritance and organization/project/application boundary checks
 * belong in the implementation, not in controllers.
 */
public interface CmsAuthorizationService {

    boolean hasPermission(
            String externalPrincipalId,
            String permissionName,
            String organizationId,
            String projectId,
            String applicationId
    );
}
