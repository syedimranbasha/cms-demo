# cms-clientsdk

Java SDK for authenticated Enterprise CMS operations.

## Capabilities

- OAuth 2.0 Client Credentials authentication; access tokens are cached until shortly before expiry.
- Retrieve effective application configuration, cache the last-known-good snapshot in memory, and poll every two minutes by default.
- Register a callback invoked after each successful configuration retrieval.
- Create organizations, projects, and applications through the CMS Management API.
- Reuse the same authenticated HTTP client and cached access token for reads and management operations.

## Maven dependency

```xml
<dependency>
  <groupId>com.walmart.cms</groupId>
  <artifactId>cms-clientsdk</artifactId>
  <version>${project.version}</version>
</dependency>
```

## Configuration retrieval example

```java
CmsClientSdk sdk = new CmsClientSdk(sdkConfig);
sdk.registerCallback(snapshot -> applicationConfigReference.set(snapshot.effectiveConfig()));
sdk.start(); // immediate fetch, then configured polling interval (2 minutes by default)
```

## Management API examples

The methods map to the existing CMS ManagementController routes and DTO contracts:

```java
OrganizationResponse organization = sdk.createOrganization("Acme");

ProjectResponse project = sdk.createProject(
    organization.organizationId().toString(), "Payments");

ApplicationResponse application = sdk.createApplication(
    organization.organizationId().toString(),
    project.projectId().toString(),
    "Checkout");
```

Endpoints (with `apiBaseUrl` configured as `http://localhost:8080/api/v1`):

| SDK method | HTTP endpoint | Request | Response |
|---|---|---|---|
| `createOrganization(name)` | `POST /organizations` | `{"name":"Acme"}` | `OrganizationResponse` |
| `createProject(organizationId, name)` | `POST /organizations/{organizationId}/projects` | `{"name":"Payments"}` | `ProjectResponse` |
| `createApplication(organizationId, projectId, name)` | `POST /organizations/{organizationId}/projects/{projectId}/applications` | `{"name":"Checkout"}` | `ApplicationResponse` |

All three endpoints use the bearer token obtained from the configured token endpoint. The CMS authorization layer must grant the calling principal the corresponding create permissions. The SDK does not bypass CMS authorization.

Management methods are synchronous and return the deserialized response DTO. They throw `CmsClientSdkException` for non-2xx responses or transport/serialization failures, and `IllegalArgumentException` for invalid local arguments. A 401 invalidates the cached token; the failed operation is surfaced and is not automatically replayed.

## Effective configuration endpoint

The SDK calls:
`GET {apiBaseUrl}/organizations/{orgId}/applications/{appId}/config?environment={environment}&region={region}`

Expected response:

```json
{
  "effective_config": { "database": { "connection_pool_size": 100 } },
  "versions": {
    "global": "ver-001", "environment": "ver-002",
    "application": "ver-003", "region": "ver-004"
  }
}
```

## Polling and callback semantics

- `start()` retrieves immediately, then schedules fixed-delay polling.
- The default polling interval is two minutes; it is configurable in `CmsClientSdkConfig`.
- Callback fires after every successful retrieval, even if the payload is unchanged.
- Failed retrievals preserve the last-known-good snapshot and do not invoke the callback.
- Callback runs on the SDK polling thread; keep it short and non-blocking.
- Callback exceptions are logged and do not terminate future polling.
- `close()` stops the poller.

## Credentials and security

Supply client ID and secret at runtime from a secret manager or protected local configuration. Do not commit secrets or log access tokens. The authenticated client must be authorized by CMS to create organizations, projects, and applications, as well as read the configured application's effective configuration.

## Build and tests

From the root Maven reactor:

```bash
mvn -pl cms-clientsdk -am clean test
```

Tests use an in-process HTTP server and test-only credentials; they do not require Keycloak or a live CMS instance.

## Creating configuration versions

The SDK also exposes typed methods for creating immutable configuration versions at each supported scope. These methods match the `ConfigurationVersionController` routes in `cms-config-api`:

```java
Map<String, Object> document = Map.of(
    "featureFlags", Map.of("newCheckout", true),
    "limits", Map.of("maxRequestsPerMinute", 500)
);

// Global scope (organization-wide)
var global = sdk.createGlobalConfigurationVersion(
    organizationId, document, "Set organization defaults", null);

// Environment scope
var environment = sdk.createEnvironmentConfigurationVersion(
    organizationId, "PROD", document, "Set production defaults", null);

// Application scope (environment is a query parameter)
var application = sdk.createApplicationConfigurationVersion(
    organizationId, applicationId, "PROD", document, "Configure checkout", null);

// Region scope
var region = sdk.createRegionConfigurationVersion(
    organizationId, applicationId, "PROD", "us-central1",
    document, "Set regional override", null);
```

Each method returns `CreateConfigurationVersionResponse` containing `scopeId`, `versionId`, `versionNumber`, and `status`.

`expectedBaseVersionId` is optional and supports optimistic concurrency when supplied. Pass `null` when creating the next version without an expected-base check. Configuration documents must be non-null and change summaries must be non-blank. The SDK sends the request using the same cached OAuth bearer token as its other management operations.

| SDK method | HTTP route |
|---|---|
| `createGlobalConfigurationVersion` | `POST /api/v1/organizations/{orgId}/config/versions` |
| `createEnvironmentConfigurationVersion` | `POST /api/v1/organizations/{orgId}/environments/{environment}/config/versions` |
| `createApplicationConfigurationVersion` | `POST /api/v1/organizations/{orgId}/applications/{appId}/config/versions?environment={environment}` |
| `createRegionConfigurationVersion` | `POST /api/v1/organizations/{orgId}/applications/{appId}/environments/{environment}/regions/{region}/config/versions` |

Creating a version does not itself activate or deploy it; deployment/activation remains a separate operation in the CMS deployment workflow.


## Management and deployment operations

The SDK also exposes `fetchOrganization`, `fetchProject`, and `fetchApplication`; `createDeployment`; `startDeploymentStage`; `fetchDeployment`; and `rollback`. These methods use the Management API and Deployment API routes defined by the Enterprise CMS design. Deployment creation only creates the deployment and rollout stages. Starting a stage is a separate call. Rollback creates a new deployment targeting a previous stable deployment/version set.


## Unit tests

The SDK tests use JUnit 5 and an in-process JDK `HttpServer`; they do not require a running CMS service, Keycloak instance, PostgreSQL, or Redis. The suite covers runtime configuration retrieval/caching, OAuth token reuse, management GET/POST operations, deployment creation, stage initiation, rollback, deployment retrieval, request validation, and API error handling.

Run the tests from the `enterprise-cms` reactor root:

```bash
mvn -pl cms-clientsdk -am clean test
```
