# Enterprise Configuration Management System (CMS)

This repository contains the source code for the Enterprise Configuration Management System and its interactive Java CLI consumer. **Executable JAR files are not committed to GitHub.** Build them locally with Maven by following this guide.

## 1. Repository modules

| Module | Responsibility |
|---|---|
| `cms-app` | Executable Spring Boot application and REST API bootstrap |
| `cms-dal` | Persistence entities, repositories, Flyway schema and embedded PostgreSQL configuration |
| `cms-cache` | Cache integration and local embedded Redis configuration |
| `cms-core-api` | Core configuration and effective-configuration logic |
| `cms-config-api` | Configuration API layer |
| `cms-deployment-api` | Deployment, staged rollout and rollback APIs |
| `cms-management-api` | Organization, project and application management APIs |
| `cms-security` | JWT resource-server security and principal integration |
| `cms-clientsdk` | Java client SDK used by consumers |
| `cms-cli-consumer` | Interactive command-line consumer built as an executable JAR |

## 2. Prerequisites

Install the following:

- **JDK 21**
- **Apache Maven 3.8+** (Maven 3.9+ recommended)
- **Docker Desktop with Docker Compose** (used to run Keycloak for local authentication)
- Git

Verify the tools in PowerShell or a terminal:

```powershell
java -version
mvn -version
docker version
docker compose version
```

Make sure Maven is using JDK 21. The `mvn -version` output shows the Java version Maven is using.

## 3. Clone the repository

```bash
git clone <YOUR_GITHUB_REPOSITORY_URL>
cd ConfigurationManagementService
```

Replace the placeholder URL with the repository URL. Run all Maven commands below from the directory containing the root `pom.xml`.

## 4. Build the complete project

Build all modules, including the Spring Boot application and the CLI consumer, from the root directory:

```bash
mvn clean package
```

Maven builds the modules in reactor dependency order. The command also runs the tests configured in the modules.

On successful completion, the two executable JARs are created at:

```text
cms-app/target/cms-app-1.0.0.jar
cms-cli-consumer/target/cms-cli-consumer-1.0.0.jar
```

The CMS application JAR is packaged by the Spring Boot Maven plugin. The CLI JAR is packaged with the Maven Shade plugin and includes its runtime dependencies and main-class manifest.

If you only want to build one executable and its required project modules:

```bash
# Build CMS application and its reactor dependencies
mvn -pl cms-app -am clean package

# Build CLI consumer and its reactor dependencies
mvn -pl cms-cli-consumer -am clean package
```

## 5. Start Keycloak

The CMS local profile validates access tokens issued by Keycloak. Keycloak is a separate service; the local CMS profile starts embedded PostgreSQL and embedded Redis, but it does not start Keycloak.

Create a `docker-compose.yml` file in a convenient local directory (for example, `local-keycloak/`) with:

```yaml
services:
  keycloak:
    image: quay.io/keycloak/keycloak:26.8.2
    container_name: enterprise-cms-keycloak
    command: start-dev
    ports:
      - "127.0.0.1:8081:8080"
    environment:
      KC_BOOTSTRAP_ADMIN_USERNAME: admin
      KC_BOOTSTRAP_ADMIN_PASSWORD: admin
    volumes:
      - keycloak_data:/opt/keycloak/data
    restart: unless-stopped

volumes:
  keycloak_data:
```

Start it from the directory containing that file:

```powershell
docker compose up -d
docker compose ps
```

Open the Keycloak Admin Console at <http://localhost:8081/admin/> and sign in with the local demo bootstrap credentials:

```text
Username: admin
Password: admin
```

> **Local-demo only:** `start-dev` and the bootstrap credentials above are for local development. Do not expose this Keycloak instance to an untrusted network or reuse these credentials in production.

### Create the realm

1. In the Keycloak Admin Console, open the realm selector.
2. Choose **Create realm**.
3. Set the realm name to `enterprise-cms` and create it.

The CMS local profile expects this issuer:

```text
http://localhost:8081/realms/enterprise-cms
```

### Create the CLI OAuth client

In the `enterprise-cms` realm:

1. Go to **Clients** and create a client.
2. Select **OpenID Connect** if prompted.
3. Set the Client ID to `cms-service-client`.
4. Enable **Client authentication**.
5. Enable **Service accounts roles**.
6. Disable **Standard flow** and **Direct access grants** for this machine-to-machine CLI client.
7. Save the client, open its **Credentials** tab and copy the generated client secret.

Keep the secret private. Do not commit it to the repository or paste it into README files.

## 6. Start the CMS application

From the repository root, start the executable JAR built in Step 4:

```powershell
java -jar .\cms-app\target\cms-app-1.0.0.jar --spring.profiles.active=local
```

Keep this terminal running. The application listens on port **8080**. Its local profile uses:

- Embedded PostgreSQL for persistence.
- Embedded Redis for caching.
- Keycloak issuer `http://localhost:8081/realms/enterprise-cms` for JWT validation.

No separate PostgreSQL or Redis installation is required for this local profile. Their lifecycle is tied to the CMS application process; stopping the CMS process stops the embedded services. Treat local demo data as disposable.

The CLI uses this API base URL:

```text
http://localhost:8080/api/v1
```

## 7. Configure and run the CLI consumer

Open a **second terminal** in the repository root. Set the Keycloak client credentials in that terminal. PowerShell:

```powershell
$env:CMS_CLIENT_ID = "cms-service-client"
$env:CMS_CLIENT_SECRET = "<paste-the-client-secret-here>"
```

The CLI configuration defaults to:

| Setting | Default |
|---|---|
| CMS API base URL | `http://localhost:8080/api/v1` |
| Keycloak token URL | `http://localhost:8081/realms/enterprise-cms/protocol/openid-connect/token` |
| Client ID | `cms-service-client` |
| Environment | `DEV` |
| Region | `us-central1` |
| Instance ID | `cms-cli-consumer-01` |
| Polling interval | 120 seconds |
| Request timeout | 10 seconds |

The CLI supports environment overrides for `CMS_CLIENT_SECRET`, `CMS_CLIENT_ID`, `CMS_ENVIRONMENT`, `CMS_REGION`, and `CMS_INSTANCE_ID`. The API base URL and token URL are configured in `cms-cli-consumer/src/main/resources/application.yml` and require a rebuild if changed.

Run the CLI from the repository root:

```powershell
java -jar .\cms-cli-consumer\target\cms-cli-consumer-1.0.0.jar
```

The CLI is an executable Java JAR, not a native Windows `.exe`.

## 8. CLI walkthrough

The interactive CLI menu provides these operations:

```text
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
12. Create deployment
13. Initiate staged deployment
14. Rollback deployment
0. Exit
```

For a first walkthrough:

1. Create an organization, project and application, in that order. Record the UUIDs returned by the CLI.
2. Create configuration versions for Global, Environment, Application and Region scopes.
3. Use environment `DEV` and region `us-central1` consistently.
4. Create a deployment for the same application, environment and region. For a simple demonstration, use a single rollout stage of `100` percent.
5. Record the deployment ID and stage ID returned by the create-deployment operation.
6. Initiate the 100% stage.
7. Fetch effective configuration using the same organization, application, environment and region.

Configuration versions are immutable. Creating a version does not, by itself, activate it; deployment controls which versions are selected for instances.

### Sample configuration documents

Paste each JSON document as a single line when prompted.

**Global**

```json
{"featureFlags":{"newCheckout":false,"darkMode":false,"recommendations":true},"service":{"timeout":30,"maxRetries":3,"pageSize":100},"payment":{"currency":"USD","gateway":"stripe"},"logging":{"level":"INFO"}}
```

**Environment**

```json
{"featureFlags":{"newCheckout":true,"darkMode":true},"service":{"timeout":45,"pageSize":200},"payment":{"gateway":"sandbox"},"logging":{"level":"DEBUG"}}
```

**Application**

```json
{"featureFlags":{"newCheckout":false,"recommendations":false},"service":{"timeout":60,"maxRetries":5},"payment":{"gateway":"adyen"},"application":{"name":"checkout-service","version":"2.5.0"}}
```

**Region**

```json
{"featureFlags":{"darkMode":false},"service":{"timeout":90},"payment":{"currency":"INR"},"region":{"code":"us-central1","dataResidency":"US"}}
```

The merge precedence is **Global → Environment → Application → Region**. More-specific scopes override values from less-specific scopes, with nested objects merged recursively.

The CLI remembers selected organization, project and application IDs only for the current CLI process. If you restart it, enter the previously returned UUIDs again.

The CLI/API exposes stage initiation; the current CLI menu does not include a separate stage-completion operation. For the straightforward demo flow, use a single 100% stage.

## 9. IntelliJ IDEA alternative

You can also run the source directly from IntelliJ:

1. Open the repository's root `pom.xml` as a Maven project.
2. Set the Project SDK and Maven importer JDK to **21**.
3. Wait for Maven dependency import to finish.
4. Start Keycloak and create the realm/client as described above.
5. Run `com.walmart.cms.ConfigurationManagementApplication` from `cms-app`, adding the VM/program argument `--spring.profiles.active=local`.
6. In a separate terminal, set `CMS_CLIENT_ID` and `CMS_CLIENT_SECRET`, then run the CLI JAR command above; or run `com.walmart.cms.consumer.CmsCliConsumerApplication` from IntelliJ with those environment variables configured in its Run Configuration.

## 10. Troubleshooting

### Maven uses the wrong Java version

Check `mvn -version`. Configure `JAVA_HOME` and your PATH so Maven runs with JDK 21, then reopen the terminal or IntelliJ.

### Keycloak token request fails

Check that Keycloak is running, the `enterprise-cms` realm exists, the client ID is `cms-service-client`, client authentication and service accounts are enabled, and the client secret is correct.

### CMS fails to start because of JWT issuer discovery

Start Keycloak first and verify that this discovery URL opens:

```text
http://localhost:8081/realms/enterprise-cms/.well-known/openid-configuration
```

The configured issuer must remain exactly:

```text
http://localhost:8081/realms/enterprise-cms
```

### CLI cannot connect to CMS

Confirm that the CMS application is still running on port 8080 and that the CLI API base URL is `http://localhost:8080/api/v1`.

### Effective configuration shows `NONE` or `{}`

Check that configuration-version creation succeeded, that the deployment response contains the expected candidate version IDs, and that the deployment's application, environment and region match the effective-configuration request. Confirm that the stage initiation succeeded. Use `DEV` and `us-central1` consistently for this sample. Check the CMS application logs for resolver, database or cache errors.

## 11. Stop the demo

- Exit the CLI with option `0`.
- Stop the CMS application with `Ctrl+C` in its terminal. Its embedded PostgreSQL and Redis stop with it.
- Stop Keycloak from the directory containing `docker-compose.yml`:

```powershell
docker compose down
```

This preserves the named `keycloak_data` volume. To remove the local Keycloak realm/client data as well (destructive):

```powershell
docker compose down -v
```

## 12. Keep build artifacts out of Git

The repository contains source code and Maven build instructions. JARs are generated locally under each module's `target/` directory and should not be committed to GitHub. A root `.gitignore` should include:

```gitignore
**/target/
*.jar
.idea/
*.iml
```

After cloning, an interviewer can reproduce the executable JARs with `mvn clean package` and run both applications using the commands in this README.
