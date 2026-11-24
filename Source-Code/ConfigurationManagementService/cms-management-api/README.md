# CMS Management API

This module provides management-plane REST endpoints for creating and reading the organization/project/application hierarchy and for creating the entities in the Identity and Access Management (IAM) data model.

## Tenant terminology

The current data model does not contain a separate `tenants` table. `organizations` is the tenant boundary, so the API exposes `/organizations` as the tenant/organization resource. It intentionally does not create a duplicate tenant entity.

## Hierarchy creation order

1. Create an organization (tenant).
2. Create a project under that organization.
3. Create an application under that project.

The service validates parent ownership before creating child resources. Application creation is scoped by both `organizationId` and `projectId` so a project from another organization cannot be used accidentally.

## REST endpoints

Base path: `/api/v1`

| Method | Path | Purpose |
|---|---|---|
| POST | `/organizations` | Create organization / tenant |
| GET | `/organizations/{organizationId}` | Retrieve organization |
| GET | `/organizations` | List organizations |
| POST | `/organizations/{organizationId}/projects` | Create project |
| GET | `/organizations/{organizationId}/projects/{projectId}` | Retrieve project |
| GET | `/organizations/{organizationId}/projects` | List projects |
| POST | `/organizations/{organizationId}/projects/{projectId}/applications` | Create application |
| GET | `/organizations/{organizationId}/projects/{projectId}/applications/{applicationId}` | Retrieve application |
| GET | `/organizations/{organizationId}/projects/{projectId}/applications` | List applications |
| POST | `/principals` | Create principal |
| POST | `/roles` | Create role |
| POST | `/permissions` | Create permission |
| POST | `/roles/{roleId}/permissions` | Assign permission to role |
| POST | `/role-bindings` | Bind role to principal at organization/project/application scope |

## Example requests

Create organization:

```http
POST /api/v1/organizations
Content-Type: application/json

{"name":"Acme"}
```

Create project:

```http
POST /api/v1/organizations/{organizationId}/projects
Content-Type: application/json

{"name":"Payments"}
```

Create application:

```http
POST /api/v1/organizations/{organizationId}/projects/{projectId}/applications
Content-Type: application/json

{"name":"Checkout"}
```

Create role binding:

```json
{
  "principalId": "<principal-uuid>",
  "roleId": "<role-uuid>",
  "organizationId": "<organization-uuid>",
  "projectId": "<optional-project-uuid>",
  "applicationId": "<optional-application-uuid>"
}
```

An application-scoped role binding requires `projectId`; the project and application must belong to the supplied organization/project respectively.

## Validation and errors

- Request fields are validated using Jakarta Bean Validation.
- Missing parent resources return HTTP 404.
- Duplicate organization, principal, role, permission, or role-permission assignment returns HTTP 409.
- Invalid hierarchy/scope combinations return HTTP 400.
- Database uniqueness and foreign-key constraints remain the final integrity safeguards.

## Tests

- `ManagementServiceTest` covers organization/project/application creation, IAM entity creation, duplicate handling, and hierarchy/scope validation.
- `ManagementControllerTest` covers HTTP status/response mapping and request validation.

Build from the repository root:

```bash
mvn -U clean package -pl cms-management-api -am
```

This module is a library module in the existing multi-module application; it does not create a separate Spring Boot process.

> Security note: these management endpoints are intended to be protected by the platform's authentication/authorization layer when deployed. The module implements the resource and hierarchy validations, not an independent authentication mechanism.
