# cms-app

Executable Spring Boot entry-point module for Enterprise CMS.

## Integration

1. Copy this `cms-app` directory into the root `enterprise-cms` directory.
2. Add `<module>cms-app</module>` to the root POM's `<modules>`.
3. Ensure the root POM uses `<packaging>pom</packaging>`, inherits Spring Boot parent 3.3.5, and manages the six internal CMS module versions in `dependencyManagement`.
4. Remove application-level Spring Boot starter dependencies and the Spring Boot Maven plugin from the root aggregator POM; they are declared here.
5. Move any existing bootstrap class and application resources from the root `src` directory into this module to avoid duplicate application entry points.

## Build and run

From the repository root:

```bash
mvn clean package
mvn -pl cms-app spring-boot:run -Dspring-boot.run.profiles=local
```

Or run the packaged application:

```bash
java -jar cms-app/target/cms-app-1.0.0.jar --spring.profiles.active=local
```

The local profile configures JWT issuer validation against Keycloak at `http://localhost:8081/realms/enterprise-cms`. Start the matching Keycloak realm before making authenticated requests.

The application scans `com.walmart.cms`; keep components in the sibling modules under that package hierarchy, or adjust `scanBasePackages` if needed.
