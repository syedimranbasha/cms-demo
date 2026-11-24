# Enterprise Configuration Management System (CMS)

## Technical Overview --- Phase 1 and Phase 2

## 1. Overview

The Enterprise CMS is a multi-tenant configuration management service.
It organizes configuration by **Organization → Project → Application**
and supports four configuration scopes: **Global, Environment,
Application and Region**.

The effective configuration is assembled in this precedence order:

``` text
Global → Environment → Application → Region
```

More-specific scopes override values from broader scopes. Configuration
versions are immutable; creating a version does not by itself make it
effective. Deployment controls the activation of candidate versions.

## 2. Phase 1 --- Implemented

### Configuration management

-   Organization, Project and Application creation and retrieval APIs.
-   Configuration version creation for Global, Environment, Application
    and Region scopes.
-   Immutable configuration-version records with change summaries.
-   Expected-base-version validation to detect stale concurrent updates.
-   Retrieval of effective configuration and historical configuration
    versions.
-   Recursive JSON merge using the defined scope precedence.

### Deployment

-   Deployment creation with baseline and candidate version sets.
-   Configurable staged rollout percentages.
-   API to initiate a deployment stage.
-   Rollback deployment API targeting a previously deployed version set.
-   Deployment and stage state represented in the persistence model.

### Serving, cache and client consumption

-   PostgreSQL is the persistence/source-of-truth store, accessed
    through Spring Data JPA repositories.
-   Redis is used as an L2 cache for effective configuration.
-   Cache failures are treated as non-fatal for reads: the service can
    resolve from PostgreSQL when cache access or deserialization fails.
-   A Java Client SDK retrieves effective configuration, caches the
    latest snapshot in-process, and polls periodically (two-minute
    default).
-   The SDK retains the last-known-good configuration when a refresh
    fails and supports callbacks when a configuration snapshot is
    retrieved.
-   OAuth 2.0 Client Credentials token acquisition and token reuse are
    implemented in the SDK.
-   A menu-driven CLI consumer demonstrates management,
    configuration-version, deployment, rollback and
    effective-configuration API flows.

### Authentication

-   Keycloak is used as the external OpenID Connect provider for the
    sample setup.
-   The CMS is configured as a stateless Spring Security OAuth 2.0
    Resource Server and validates JWT access tokens.
-   The authenticated JWT subject and user identity are exposed to
    application code through a CMS principal abstraction and argument
    resolver.

### Local development and testing support

-   A local Spring profile starts embedded PostgreSQL and embedded Redis
    with the CMS application.
-   Unit and integration test coverage exists across selected modules,
    including repository, cache, SDK, controller and service behavior.

## 3. Phase 1 --- Planned but not completed

**Authorization was intended to be part of Phase 1, but I could not
complete it within the available time.** The authentication foundation
and parts of the authorization framework are present, but end-to-end
resource-level RBAC enforcement is not implemented.

The codebase includes: - Principal, Role, Permission, RolePermission and
RoleBinding persistence entities/repositories. - A
`CmsAuthorizationService` contract. - A principal abstraction populated
from the authenticated JWT.

Still required: - Implement the database-backed authorization service. -
Resolve the authenticated Keycloak subject to the CMS principal
record. - Evaluate permissions against organization, project and
application scopes. - Define and enforce scope-inheritance rules
consistently. - Apply authorization checks to management, configuration
and deployment operations. - Add tests for allowed and denied access,
cross-tenant isolation and scope inheritance.

Therefore, **authentication is implemented; complete CMS RBAC
authorization is not**. The current authenticated API setup should not
be represented as complete authorization or production-ready access
control.

## 4. Phase 2 --- Planned

Phase 2 is intended to complete authorization and move configuration
propagation toward an event-driven model.

-   **Complete RBAC authorization:** database-backed permission
    evaluation and enforcement at organization, project and application
    levels.
-   **Event-driven propagation:** transactional outbox and Kafka events
    for configuration/deployment changes.
-   **Asynchronous cache population:** workers consume configuration
    events and refresh affected Redis entries.
-   **Push notifications:** notification service with SSE/WebSocket
    delivery to registered client SDK instances, reducing dependence on
    polling.
-   **Client registration and lifecycle:** registration/heartbeat
    handling and targeted notification delivery.
-   **Audit pipeline:** durable audit-event processing and query
    capabilities for configuration and deployment changes.
-   **Operational hardening:** richer rollout progression/completion
    workflows, retries, reconciliation, metrics, tracing and production
    deployment configuration.

These are planned capabilities; they should not be assumed to be present
in the current Phase 1 executable.

## 5. Technology choices

  -------------------------------------------------------------------------
  Area                    Technology / choice     Rationale
  ----------------------- ----------------------- -------------------------
  Language                Java 21                 LTS Java baseline and
                                                  modern language/runtime
                                                  features

  Application framework   Spring Boot 3.3.5       Dependency injection,
                                                  application lifecycle,
                                                  REST and configuration

  Build                   Maven multi-module      Clear module boundaries
                          reactor                 and dependency-ordered
                                                  builds

  API                     Spring Web MVC / REST   HTTP APIs with
                                                  request/response DTOs

  Persistence             PostgreSQL, Spring Data Relational source of
                          JPA, Hibernate          truth and transaction
                                                  support

  Data access             Repository pattern;     Encapsulates persistence
                          derived Spring Data     operations and keeps
                          queries plus custom     services independent of
                          repository              SQL details
                          implementations where   
                          needed                  

  Cache                   Redis with Spring Data  Fast L2
                          Redis                   effective-configuration
                                                  reads

  Identity provider       Keycloak / OpenID       External token issuance
                          Connect                 and client-credentials
                                                  authentication

  API security            Spring Security OAuth2  Stateless bearer-token
                          Resource Server / JWT   validation

  SDK HTTP client         JDK `HttpClient`        Avoids requiring a
                                                  separate HTTP client
                                                  framework in the SDK

  SDK refresh             Scheduled polling       Simple Phase 1
                          (two-minute default)    configuration refresh
                                                  mechanism

  Tests                   JUnit 5, Mockito and    Unit and integration
                          Spring Boot Test        testing
  -------------------------------------------------------------------------

## 6. Architecture and coding practices

-   **Modular architecture:** responsibilities are separated into
    `cms-dal`, `cms-cache`, `cms-core-api`, `cms-config-api`,
    `cms-deployment-api`, `cms-management-api`, `cms-security`,
    `cms-clientsdk`, `cms-cli-consumer` and the executable `cms-app`
    bootstrap module.
-   **Layered design:** controllers handle HTTP concerns, services
    coordinate business logic, repositories handle persistence, and DTOs
    define API contracts.
-   **Repository pattern:** Spring Data repositories encapsulate data
    access. Custom repository implementations are used for operations
    that need more than derived queries.
-   **Dependency injection:** Spring-managed components are wired
    through constructor injection, making dependencies explicit and
    classes easier to test.
-   **Immutable configuration history:** new configuration changes
    create new version records rather than overwriting historical
    documents.
-   **Optimistic concurrency:** expected-base-version checks prevent a
    caller from unknowingly creating a version against stale state.
-   **Transactional boundaries:** persistence operations that must
    remain consistent are coordinated using Spring transaction support.
-   **Cache resilience:** Redis is an optimization rather than the
    source of truth; cache read/population failures can fall back to
    PostgreSQL.
-   **Last-known-good client behavior:** SDK refresh failures do not
    discard the last successfully retrieved configuration.
-   **DTO boundary:** API request/response models are separated from
    persistence entities.
-   **Input validation and error mapping:** API and SDK layers validate
    inputs and translate failures into explicit exceptions/responses.
-   **Testability:** business logic is separated from HTTP and
    persistence details so unit tests can use mocks; integration tests
    exercise Spring wiring and infrastructure where configured.

## 7. Phase 1 scope statement

Phase 1 delivers the core configuration lifecycle: hierarchical
configuration management, immutable versioning, effective-configuration
resolution, caching, deployment APIs, JWT authentication, and a Java
SDK/CLI demonstration.

The key outstanding Phase 1 commitment is **authorization**. Its
supporting entities and service contract exist, but permission
evaluation and enforcement still need implementation. Phase 2 focuses on
completing that security capability and adding event-driven propagation,
push notifications and operational maturity.
