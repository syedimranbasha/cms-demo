# Enterprise CMS – Phase 1 API modules

Modules: `cms-core-api` (read), `cms-config-api` (configuration-version writes), and `cms-deployment-api` (deployment/rollout/rollback). Shared `cms-dal` and `cms-cache` are included as supporting modules. All APIs use Spring Boot + Spring Web MVC (Servlet), synchronous service calls, Spring Data JPA/Hibernate, PostgreSQL and Redis.

## Phase 1 cache behavior

`cms-config-api` creates the immutable configuration version in PostgreSQL. After the transaction commits, it synchronously calls `EffectiveConfigurationCache.evictScope(...)` before the HTTP request completes. `cms-deployment-api` also synchronously invalidates the affected application/environment/region after stage activation or rollback creation, because rollout selection changes the effective version set. The invalidation is scoped by the changed configuration scope: Global invalidates all app/environment/region entries in the organization; Environment invalidates all applications/regions for that environment; Application invalidates all regions for that app/environment; Region invalidates only that app/environment/region. The Redis implementation uses SCAN, not KEYS, and matches every rollout track and version fingerprint.

**Failure semantics:** PostgreSQL may already be committed when the after-commit invalidation fails. In that case the API returns an error explicitly stating that the version was committed but invalidation failed; clients must not blindly retry the version creation. A reconciliation/retry mechanism is a Phase 1 operational requirement.

No Kafka, pub/sub, outbox publisher, SSE, or WebSocket flow is included in this Phase 1 API interaction.

## Build

Run `mvn clean test` from this directory. Maven was not available in the generation environment, so compilation and test execution have not been verified. The project includes the existing DAL and cache tests plus the API unit/integration tests.
