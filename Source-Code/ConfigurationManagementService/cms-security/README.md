# cms-security

Shared Spring Security and CMS authorization integration module.

## Responsibilities

1. Configure the application as a stateless OAuth2 Resource Server.
2. Validate bearer JWT access tokens using Spring Security and the issuer configured by the application.
3. Expose the authenticated caller through `CmsCurrentUser` and `CmsPrincipal`.
4. Provide the `CmsAuthorizationService` contract for CMS database-backed RBAC checks.
5. Enable method-level security annotations such as `@PreAuthorize`.

## Important boundary

This module does not issue tokens and does not implement an identity provider. Keycloak (or another OIDC provider) authenticates users/services and issues JWTs. This module validates those JWTs.

The included `CmsAuthorizationService` is an interface only. A concrete implementation must be wired to the actual CMS DAL entities/repositories and enforce `principals`, `role_bindings`, `roles`, `role_permissions`, and `permissions`. It is intentionally not faked here because the exact repository/entity APIs in `cms-dal` are project-specific.

## Integration

1. Add `<module>cms-security</module>` to the root POM's `<modules>`.
2. Add this to root `dependencyManagement`:

```xml
<dependency>
    <groupId>com.walmart.cms</groupId>
    <artifactId>cms-security</artifactId>
    <version>${project.version}</version>
</dependency>
```

3. Add the dependency to `cms-app/pom.xml`:

```xml
<dependency>
    <groupId>com.walmart.cms</groupId>
    <artifactId>cms-security</artifactId>
</dependency>
```

4. Keep the issuer URI in the runtime application's configuration, for example in `cms-app/src/main/resources/application-local.yml`:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8081/realms/enterprise-cms
```

5. Ensure `ConfigurationManagementApplication` scans `com.walmart.cms`, so the security configuration is discovered.

## Using the authenticated caller

Inject `CmsCurrentUser` into a service and call `getRequiredPrincipal()` to obtain the validated JWT subject and username. Store/map `externalId` to `principals.external_id`; do not use a client-supplied principal ID as the authenticated identity.

## Using authorization

After implementing and registering `CmsAuthorizationService`, call it from application services or expose a small authorization bean for `@PreAuthorize` expressions. Do not treat a valid JWT as proof that the caller has permission to access every organization/project/application.

## Tests

The module includes a unit test for the `CmsPrincipal` identity value object. Add integration tests for JWT validation and RBAC decisions when the concrete DAL-backed authorization implementation is introduced.
