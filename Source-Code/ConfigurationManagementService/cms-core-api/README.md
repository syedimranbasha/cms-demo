# cms-core-api

Read-path and configuration-version APIs from the Enterprise CMS design document.

Included:
- Retrieve effective configuration
- Retrieve historical configuration version
- Create immutable configuration version (Global, Environment, Application, and Region scopes).
  The Application-scope endpoint accepts `environment` as a query parameter because the
  current DAL scope identity requires an environment for APPLICATION scopes.

Excluded from this phase:
- Register SDK with Notification Service
- SSE notification stream API

Dependencies:
- `cms-dal` for PostgreSQL persistence and embedded PostgreSQL under the `local`/`test` profiles
- `cms-cache` for the Redis-backed effective configuration cache (embedded Redis starts through its Spring configuration)

The module is designed to be added to the existing `enterprise-cms` parent reactor. Its parent POM must aggregate
`cms-dal`, `cms-cache`, `cms-core-api`, and `cms-deployment-api`.

Run from the repository root:
```bash
mvn -pl cms-core-api -am clean test
```

Run the host application with the `local` profile to activate the embedded PostgreSQL and embedded Redis instances.
The host application must component-scan `com.walmart.cms`.

## Contract alignment notes

- The current DAL `config_versions` schema/entity has no `created_at` column. The historical-version response
  includes `createdAt` as `null` until that column is added to the DAL schema and entity.
- The current Phase 1 DAL migration excludes `audit_logs` and `outbox_events`; these API modules therefore do not
  persist audit records or transactional outbox events yet.
- Staged candidate selection in the read API uses the current deployment's `deployment_instance` assignment for
  the supplied `X-Instance-Id`. A rollout coordinator must populate those rows. Unassigned instances receive the
  baseline version set.
