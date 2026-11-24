# CMS Cache Module

Maven module for the Enterprise Configuration Management System.

Package prefix: `com.walmart.cms.cache`.

## Package structure

- `service/`
  - `EffectiveConfigurationCache`: cache contract consumed by `cms-api`.
  - `EffectiveConfigurationCacheImpl`: Redis-backed implementation.
  - `EffectiveConfigurationCacheKeyFactory`: cache-key factory contract.
  - `Sha256EffectiveConfigurationCacheKeyFactory`: deterministic key factory. It sorts scope/version entries, creates a canonical representation, and calculates a SHA-256 fingerprint.
- `config/`: Spring Redis configuration and cache properties.
- `model/`: effective configuration, version metadata, and cache-key models.
- `exception/`: cache access and serialization exceptions.

## Fingerprint calculation and usage

`Sha256EffectiveConfigurationCacheKeyFactory.create(...)` receives the selected scope-to-version mapping. It sorts the entries to ensure deterministic ordering, hashes the canonical representation using SHA-256, and places the fingerprint in `EffectiveConfigurationCacheKey`.

The factory is a Spring component. `cms-api` should inject `EffectiveConfigurationCacheKeyFactory` and call `create(...)` after selecting the baseline or candidate version set. It should inject `EffectiveConfigurationCache` to read and write cache entries.

## Embedded Redis for local demo

This module starts an embedded Redis server as part of the CMS Spring Boot application.
The embedded Redis dependency is a normal runtime dependency (not test-only).

- `CmsCacheConfiguration` creates an embedded `RedisServer` bean with `start`/`stop` lifecycle callbacks.
- The Redis server starts with the Spring application context and stops when the context closes.
- `RedisConnectionFactory` is created after the embedded server bean has started.
- `StringRedisTemplate` is created from that connection factory and injected into `EffectiveConfigurationCacheImpl`.
- Embedded Redis is enabled by default for the local demo.

Configuration in `src/main/resources/application-local.yml`:

```yaml
cms:
  cache:
    embedded-redis:
      enabled: true
      host: 127.0.0.1
      port: 6379
```

The port must be available on the local machine. Override it if another Redis process is already using it.
Set `cms.cache.embedded-redis.enabled=false` only when intentionally supplying an alternative Redis configuration; in that case the application must provide its own `RedisConnectionFactory` and `StringRedisTemplate` beans.

## Dependency injection

Add this module as a dependency of `cms-api`. Ensure the Spring Boot application scans `com.walmart.cms`.

## Build

From the parent Maven project:

```bash
mvn clean install
```

This module is packaged into the same Spring Boot application as `cms-api`; it is not a separate deployable service.

## Notes

- PostgreSQL fallback and configuration merging belong in `cms-api`/`cms-dal`, not in this cache module.
- Redis stores the fully merged effective configuration and version metadata supplied by the caller.
- The integration test uses Embedded Redis on its configured test port.

## Unit tests

The service package has tests for all four source files:

- `EffectiveConfigurationCacheContractTest`: exercises the public cache interface contract.
- `EffectiveConfigurationCacheImplTest`: tests Redis adapter GET/SET/DELETE/EXISTS behavior, serialization, corrupt entries, TTL validation, and Redis failures using Mockito.
- `Sha256EffectiveConfigurationCacheKeyFactoryTest`: tests deterministic ordering, version-change fingerprint invalidation, and invalid input handling.
- `EffectiveConfigurationCacheKeyFactory` is exercised through its contract implementation in the fingerprint factory tests and the cache service contract wiring.

Run the module tests from the parent project:

```bash
mvn -pl cms-cache -am test
```

The Mockito-based unit tests do not require a running Redis server. `EmbeddedRedisRuntimeIntegrationTest` starts the embedded Redis process and verifies actual `StringRedisTemplate` connectivity.
