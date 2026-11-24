# CMS CLI Consumer (updated)

Interactive Java 21 console application that uses `cms-clientsdk` for CMS management, deployment, rollback, and effective-configuration retrieval.

## Menu

1. Create organization
2. Create project
3. Create application
4. Add Global configuration version
5. Add Environment configuration version
6. Add Application configuration version
7. Add Region configuration version
8. Fetch effective configuration continuously
9. Fetch organization
10. Fetch project
11. Fetch application
12. Create deployment (creates rollout stages but does not start rollout)
13. Initiate staged deployment (starts an explicitly selected stage after approval)
14. Rollback deployment
0. Exit

## Input validation and operation feedback

Each menu option prints the required fields and expected formats before prompting. Inputs are validated locally; invalid UUIDs, names, environment/region values, JSON documents, and rollout percentages are rejected and the CLI asks for corrected input. The CLI prints a wait message before making API calls and prints the returned response as formatted JSON when the operation completes. API failures are shown without terminating the menu.

Rollout percentages default to `10,50,100`; they must be strictly increasing and end at 100. Option 12 creates a deployment and its rollout stages only; it does not start rollout. Option 13 initiates a selected stage using the deployment ID and stage ID returned by option 12. When the last-created deployment is selected, the CLI offers its first stage ID as the default. The API derives the approver from the authenticated principal; the CLI does not accept an approver identity from user input. If stage initiation fails, inspect the deployment status before retrying.

Rollback requires the currently deployed deployment ID and the target stable deployment ID. It creates a new rollback deployment; it does not mutate immutable configuration versions.

## Configuration

Edit `src/main/resources/application.yml` for API URL, token URL, client ID, environment, region, instance ID, polling interval, and timeout. Set the client secret through an environment variable; do not commit it:

PowerShell:

```powershell
$env:CMS_CLIENT_SECRET = "<client-secret>"
$env:CMS_CLIENT_ID = "cms-service-client" # optional override
```

The API base URL should include `/api/v1`, for example `http://localhost:8080/api/v1`.

## Build and run

The bundle includes an updated `cms-clientsdk` module with the fetch, deployment creation, stage initiation, and rollback operations used by this CLI. Ensure both `cms-clientsdk` and `cms-cli-consumer` are listed in the root reactor POM. From the `enterprise-cms` root:

```bash
mvn -pl cms-cli-consumer -am clean package
```

Run:

```bash
java -jar cms-cli-consumer/target/cms-cli-consumer-1.0.0.jar
```

Java 21 is required. The Shade Plugin creates an executable fat JAR; it is not a native Windows `.exe`.
