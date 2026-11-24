# cms-dal — Phase 1

PostgreSQL/JPA persistence module for the Enterprise Configuration Management System.

## Included data-model areas

- Organization and Application Hierarchy
- Configuration Management and Deployment
- Identity and Access Management

## Excluded for Phase 1

- Registration storage (`notification_registrations`)
- Audit and Event Processing (`audit_logs`, `outbox_events`)

The migration creates exactly the 13 tables in the included areas. It does not add business tables.

## Folder structure

```text
cms-dal/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/walmart/cms/dal/
    │   │   ├── repository/
    │   │   ├── entity/
    │   │   ├── mapper/
    │   │   ├── model/
    │   │   ├── config/
    │   │   └── exception/
    │   └── resources/db/migration/
    └── test/java/com/walmart/cms/dal/repository/
```

## DAL boundary and method selection

Repositories expose only operations required by the Phase 1 APIs/use cases described in the design:
- Create and validate organization/project/application hierarchy.
- Resolve application hierarchy and tenant ownership for reads/writes.
- Create and retrieve immutable configuration versions; lock a scope row while assigning the next version.
- Retrieve the exact configuration documents for a selected version set.
- Create/fetch deployments, retrieve the active serving deployment, advance stages, and track instance acknowledgements.
- Resolve principals, roles, permissions, and role bindings for authorization.

There are deliberately no generic `update`, `delete`, or `findAll` methods where no Phase 1 API requires them. Spring Data's inherited CRUD surface is unavoidable when extending `JpaRepository`; the CMS API should use only the declared use-case methods.

Business orchestration and transaction boundaries belong in `cms-api`. For example, create a deployment and its stages in one `@Transactional` service operation. The DAL repositories participate in that transaction.

## Build/integration

This is a child module and expects the root `enterprise-cms/pom.xml` to manage Java/Spring Boot versions and aggregate `cms-api`, `cms-dal`, and `cms-cache`.

```bash
mvn -pl cms-dal -am clean test
```

Add this dependency to `cms-api`:

```xml
<dependency>
  <groupId>com.walmart.cms</groupId>
  <artifactId>cms-dal</artifactId>
</dependency>
```

Datasource example for the deployable application:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/enterprise_cms
    username: ${CMS_DB_USERNAME}
    password: ${CMS_DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
```

Hibernate validates mappings; Flyway owns schema creation and evolution.

## Important implementation notes

- Configuration scope precedence and JSON merge remain in `cms-api`; DAL returns the requested JSONB documents.
- Configuration version creation uses optimistic concurrency semantics: the caller
  supplies `expectedBaseVersionId` (the version it read/edited). `null` means the
  caller expects no version to exist yet.
- `createNextVersion` acquires a `PESSIMISTIC_WRITE` lock on the `config_scopes` row,
  reads the latest version, and compares its ID with `expectedBaseVersionId`.
  A mismatch raises `StaleConfigurationVersionException`; no version is inserted.
- When the base matches, the DAL assigns the next version number and inserts the new
  immutable version in the same transaction. The existing unique
  `(scope_id, version_number)` constraint is the final database integrity guard.
- `cms-api` should translate `StaleConfigurationVersionException` to HTTP `409 Conflict`
  and return the current latest version ID so the client can reload/rebase its change.
- The scope lock is held for the duration of the transaction. Do not call the
  `findByIdForUpdate` repository method outside a transaction.
- The design's `deployment_instance` primary key is `(deployment_id, stage_id, instance_id)`.
- The `config_deployments` JSONB version sets preserve the four scope version IDs.
- No registration, audit, or outbox entities/repositories are included.


## Embedded PostgreSQL for local demo and tests

The module includes Zonky Embedded PostgreSQL `2.2.2` as a **runtime dependency**.
This is intentional for this demo: the `local` profile starts a real PostgreSQL
server inside the application process, without requiring a separately installed
database. The `test` profile uses the same configuration for repository
integration tests. Zonky runs native PostgreSQL binaries; it is not an H2
compatibility layer.

`EmbeddedPostgresConfiguration` is profile-gated to `local` and `test`:
- `local`: embedded server is started when the application context starts.
- `test`: embedded server is started for the Spring test context and closed when
  that context shuts down.
- other profiles: no embedded server is started; configure an external
  PostgreSQL datasource instead.

The configuration chooses an ephemeral free port (`setPort(0)`) and exposes
the embedded server's `postgres` database as the Spring `DataSource`. Flyway
then applies `V1__create_phase1_schema.sql`. The embedded database is intended
for a local demo and tests; treat it as disposable demo data rather than a
production persistence service.

Run local demo:

```bash
mvn -pl cms-api -am spring-boot:run -Dspring-boot.run.profiles=local
```

Run DAL tests:

```bash
mvn -pl cms-dal -am test
```

The included `OrganizationRepositoryIntegrationTest` demonstrates a real
repository round-trip against embedded PostgreSQL. The `test` profile is
activated by the test itself.

**Platform note:** Zonky ships native PostgreSQL binaries. The project documents
support for Windows, macOS, Linux and several architectures; check the library's
architecture artifacts if your development machine is not amd64.


## Repository test coverage

`RepositoryIntegrationTests` exercises all 13 repository interfaces against the
embedded PostgreSQL instance (not an in-memory substitute). Coverage includes:

- Organization, project and application lookups and tenant isolation.
- Configuration scope lookup and the derived-query pessimistic-lock method `findFirstByScopeId`.
- Configuration version history and retrieval of version entities by ID collection.
- Deployment lookup by identity and serving context; stage ordering and status
  lookup; deployment-instance composite-key queries.
- Principal, role and permission identity lookups.
- Role-permission assignment lookup and hierarchical role-binding applicability.

These are repository integration tests rather than Mockito-only unit tests:
Spring Data derived-query generation, Hibernate mappings, PostgreSQL JSONB,
composite keys, constraints and Flyway schema are exercised together. Each
`@DataJpaTest` test method runs in a transaction that Spring rolls back.

The DAL test bootstrap is `CmsDalTestApplication`; the test profile imports
`EmbeddedPostgresConfiguration`. Run the complete repository suite with:

```bash
mvn -pl cms-dal -am clean test
```

Maven execution was not available in the generation environment, so the tests
are included but have not been compiled or executed here.


## ORM and query approach

`spring-boot-starter-data-jpa` supplies Spring Data JPA and the JPA integration.
Hibernate is the ORM provider. Repository lookups use Spring Data derived query
methods; Hibernate generates the SQL. There are no handwritten JPQL queries,
native SQL queries, or direct `EntityManager` query calls in the Java source.
Flyway migration SQL remains intentionally responsible for schema creation.

The DAL has no separate service layer. Data-dependent validation and atomic
persistence workflows are custom repository operations, while ordinary reads
and writes are exposed as derived repository methods.

Build/test command:

```bash
mvn -pl cms-dal -am clean test
```

Maven was not available in the generation environment, so the updated suite
has not been compiled or executed here.

## Configuration version concurrency contract

Normal writes should call:

```java
configurationVersionRepository.createNextVersion(
    scopeId,
    expectedBaseVersionId, // null only when no version existed when editing began
    newVersionEntity
);
```

The operation is transactional and performs these steps in order:

1. Lock the existing `config_scopes` row using `SELECT ... FOR UPDATE` (via
   `@Lock(PESSIMISTIC_WRITE)`).
2. Read the latest immutable version for that scope.
3. Compare its `version_id` with `expectedBaseVersionId`.
4. If they differ, throw `StaleConfigurationVersionException` and roll back.
5. If they match, assign `version_number = latest + 1` (or `1` for the first version)
   and insert the new version.
6. Let the unique `(scope_id, version_number)` constraint provide a final race/integrity
   safeguard.

This is optimistic concurrency control at the API contract level (compare-and-set using
the caller's base version), combined with a short pessimistic row lock to make the
check-and-insert sequence atomic. It does not use JPA `@Version`: configuration versions
are immutable rows, and the scope-row lock serializes sequence allocation. The CMS API
must not use the low-level `save` method for ordinary user edits; use `createNextVersion`.


## Repository-owned data validation and atomic operations

This module intentionally has no DAL service layer. Data-dependent validation and transactional persistence workflows are implemented as custom repository operations:

- `ConfigurationScopeRepository.createValidated(scope)` validates organization existence, scope-type field requirements, application ownership, and duplicate logical scope identity before inserting.
- `ConfigurationVersionRepository.createNextVersion(scopeId, expectedBaseVersionId, version)` locks the scope row, checks the caller's expected latest version, assigns the next version number, and inserts atomically. A stale base throws `StaleConfigurationVersionException`.
- Other repositories expose focused persistence/query operations directly to `cms-api`; use-case orchestration and effective-configuration merging remain in `cms-api`.

The database constraints remain the final integrity guard. The schema does not contain environment or region reference tables, so those values are checked for presence/shape only; validating them against an authoritative registry requires such a data source.

### Custom repository unit tests

Two Mockito-based unit test classes cover the custom repository implementations
without starting Spring or PostgreSQL:

- `ConfigurationScopeRepositoryImplTest`: valid GLOBAL, ENVIRONMENT,
  APPLICATION and REGION scope creation; type normalization; missing/unknown
  organization; unsupported scope type; invalid scope-field combinations;
  cross-organization application references; duplicate scope rejection; and
  prevention of persistence after validation failure.
- `ConfigurationVersionRepositoryImplTest`: first-version creation, sequential
  version numbering, matching and stale base-version behavior, null expected
  base when a version already exists, missing scope, and invalid arguments.

The embedded-PostgreSQL `RepositoryIntegrationTests` complements these unit
tests by exercising the Spring Data-derived repository methods for all 13
 repository interfaces, including query derivation, entity mappings, composite
 identifiers, and JSONB persistence.
