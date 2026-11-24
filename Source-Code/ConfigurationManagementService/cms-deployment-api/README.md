# cms-deployment-api

Admin deployment APIs from the Enterprise CMS design document:
- Create deployment
- Initiate staged deployment
- Rollback
- Fetch staged deployment

Depends on `cms-dal` for PostgreSQL and embedded PostgreSQL under `local`/`test`, and `cms-cache` for the shared cache configuration.
The module does not include notification registration or SSE APIs.

The design document says Create deployment does not begin rollout; Initiate Staged deployment activates the selected stage.
Rollback creates a new deployment that points to a previously deployed version set; immutable versions are not modified.

Run:
```bash
mvn -pl cms-deployment-api -am clean test
```

## Contract alignment notes

- The current DAL deployment-stage schema has no approval actor/comment or health-evaluation columns. The start
  endpoint obtains the actor from the authenticated `java.security.Principal`, but approval metadata and health
  evaluation cannot be persisted until the DAL schema/model is extended.
- The current Phase 1 DAL migration excludes `audit_logs` and `outbox_events`; audit and outbox publication are
  not implemented by this API module yet.
