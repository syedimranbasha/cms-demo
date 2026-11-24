package com.walmart.cms.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.walmart.cms.clientsdk.CmsClientSdk;
import com.walmart.cms.clientsdk.config.CmsClientSdkConfig;
import com.walmart.cms.clientsdk.model.*;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.function.Predicate;

public final class CmsCliConsumerApplication {
    private static final ObjectMapper JSON = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();
    private final Scanner in = new Scanner(System.in);
    private final Settings settings;
    private final CmsClientSdk managementSdk;
    private OrganizationResponse selectedOrganization;
    private ProjectResponse selectedProject;
    private ApplicationResponse selectedApplication;
    private String selectedRegion;
    private DeploymentResponse selectedDeployment;

    private CmsCliConsumerApplication(Settings settings) {
        this.settings = settings;
        this.managementSdk = new CmsClientSdk(settings.sdkConfig("bootstrap-organization", "bootstrap-application",
                settings.region, settings.environment));
    }

    public static void main(String[] args) {
        try {
            new CmsCliConsumerApplication(Settings.load()).run();
        } catch (Exception e) {
            System.err.println("Unable to start CMS CLI consumer: " + e.getMessage());
            System.exit(1);
        }
    }

    private void run() {
        System.out.println("\nEnterprise CMS - Client SDK sample");
        boolean running = true;
        while (running) {
            printMenu();
            String choice = prompt("Select an option [0-14]");
            try {
                switch (choice) {
                    case "1" -> createOrganization();
                    case "2" -> createProject();
                    case "3" -> createApplication();
                    case "4" -> addGlobalVersion();
                    case "5" -> addEnvironmentVersion();
                    case "6" -> addApplicationVersion();
                    case "7" -> addRegionVersion();
                    case "8" -> fetchContinuously();
                    case "9" -> fetchOrganization();
                    case "10" -> fetchProject();
                    case "11" -> fetchApplication();
                    case "12" -> deployConfiguration();
                    case "13" -> initiateStagedDeployment();
                    case "14" -> rollback();
                    case "0" -> running = false;
                    default -> System.out.println("Invalid option. Enter a number from 0 to 14.");
                }
            } catch (Exception e) {
                System.out.println("Operation failed: " + safeMessage(e));
            }
        }
        managementSdk.close();
        System.out.println("CMS CLI consumer stopped.");
    }

    private void printMenu() {
        System.out.printf("""

                ================= CMS MENU =================
                1. Create organization
                2. Create project
                3. Create application
                4. Add configuration version - Global
                5. Add configuration version - Environment
                6. Add configuration version - Application
                7. Add configuration version - Region
                8. Fetch effective configuration continuously (every %d seconds)
                9. Fetch organization
                10. Fetch project
                11. Fetch application
                12. Create deployment (prepare rollout stages; does not start rollout)
                13. Initiate staged deployment (approve/start a selected stage)
                14. Rollback deployment
                0. Exit
                ============================================
                Current organization: %s
                Current project:      %s
                Current application:  %s
                Current region:       %s
                """.formatted(settings.pollingIntervalSeconds, id(selectedOrganization), id(selectedProject),
                id(selectedApplication), selectedRegion == null ? "(not selected)" : selectedRegion));
    }

    private void createOrganization() {
        System.out.println("Enter organization details. Format: organization name (1-200 characters).");
        runOperation("Creating organization", () -> {
            selectedOrganization = managementSdk.createOrganization(promptName("Organization name"));
            selectedProject = null; selectedApplication = null;
            return selectedOrganization;
        });
    }

    private void createProject() {
        System.out.println("Enter project details. Format: organization ID (UUID), project name (1-200 characters).");
        runOperation("Creating project", () -> {
            String org = promptUuid("Organization ID", idOrNull(selectedOrganization));
            selectedProject = managementSdk.createProject(org, promptName("Project name"));
            selectedApplication = null;
            return selectedProject;
        });
    }

    private void createApplication() {
        System.out.println("Enter application details. Format: organization ID (UUID), project ID (UUID), application name (1-200 characters).");
        runOperation("Creating application", () -> {
            String org = promptUuid("Organization ID", idOrNull(selectedOrganization));
            String project = promptUuid("Project ID", idOrNull(selectedProject));
            selectedApplication = managementSdk.createApplication(org, project, promptName("Application name"));
            return selectedApplication;
        });
    }

    private void addGlobalVersion() {
        System.out.println("Enter Global configuration details. Format: organization ID (UUID), JSON object, change summary, optional base version UUID.");
        runOperation("Creating GLOBAL configuration version", () -> managementSdk.createGlobalConfigurationVersion(
                promptUuid("Organization ID", idOrNull(selectedOrganization)), promptConfig(), promptName("Change summary"), promptOptionalUuid("Expected base version ID")));
    }

    private void addEnvironmentVersion() {
        System.out.println("Enter Environment configuration details. Format: organization ID (UUID), environment name, JSON object, change summary, optional base version UUID.");
        runOperation("Creating ENVIRONMENT configuration version", () -> managementSdk.createEnvironmentConfigurationVersion(
                promptUuid("Organization ID", idOrNull(selectedOrganization)), promptEnvironment("Environment", settings.environment),
                promptConfig(), promptName("Change summary"), promptOptionalUuid("Expected base version ID")));
    }

    private void addApplicationVersion() {
        System.out.println("Enter Application configuration details. Format: organization ID (UUID), application ID (UUID), environment, JSON object, change summary, optional base version UUID.");
        runOperation("Creating APPLICATION configuration version", () -> managementSdk.createApplicationConfigurationVersion(
                promptUuid("Organization ID", idOrNull(selectedOrganization)), promptUuid("Application ID", idOrNull(selectedApplication)),
                promptEnvironment("Environment", settings.environment), promptConfig(), promptName("Change summary"), promptOptionalUuid("Expected base version ID")));
    }

    private void addRegionVersion() {
        System.out.println("Enter Region configuration details. Format: organization ID (UUID), application ID (UUID), environment, region code, JSON object, change summary, optional base version UUID.");
        runOperation("Creating REGION configuration version", () -> managementSdk.createRegionConfigurationVersion(
                promptUuid("Organization ID", idOrNull(selectedOrganization)), promptUuid("Application ID", idOrNull(selectedApplication)),
                promptEnvironment("Environment", settings.environment), promptRegion("Region code", selectedRegion),
                promptConfig(), promptName("Change summary"), promptOptionalUuid("Expected base version ID")));
    }

    private void fetchOrganization() {
        System.out.println("Enter organization identifier. Format: organization ID as a UUID.");
        runOperation("Fetching organization", () -> managementSdk.fetchOrganization(promptUuid("Organization ID", idOrNull(selectedOrganization))));
    }

    private void fetchProject() {
        System.out.println("Enter project identifiers. Format: organization ID and project ID, both UUIDs.");
        runOperation("Fetching project", () -> managementSdk.fetchProject(
                promptUuid("Organization ID", idOrNull(selectedOrganization)), promptUuid("Project ID", idOrNull(selectedProject))));
    }

    private void fetchApplication() {
        System.out.println("Enter application identifiers. Format: organization ID, project ID, and application ID, all UUIDs.");
        runOperation("Fetching application", () -> managementSdk.fetchApplication(
                promptUuid("Organization ID", idOrNull(selectedOrganization)), promptUuid("Project ID", idOrNull(selectedProject)),
                promptUuid("Application ID", idOrNull(selectedApplication))));
    }

    private void deployConfiguration() {
        System.out.println("Enter deployment details. Format: organization ID (UUID), application ID (UUID), environment, region code, and rollout percentages as comma-separated increasing integers ending in 100 (example: 10,50,100).");
        runOperation("Creating deployment and its rollout stages (rollout will NOT start yet)", () -> {
            String org = promptUuid("Organization ID", idOrNull(selectedOrganization));
            String app = promptUuid("Application ID", idOrNull(selectedApplication));
            String env = promptEnvironment("Environment", settings.environment);
            String region = promptRegion("Region code", selectedRegion == null ? settings.region : selectedRegion);
            List<CreateDeploymentRequest.StageRequest> stages = promptStages();
            selectedDeployment = managementSdk.createDeployment(org, app,
                    new CreateDeploymentRequest(env, region, stages));
            System.out.println("Deployment created with status " + selectedDeployment.status()
                    + ". Rollout has not started. Use option 14 to initiate a stage.");
            return selectedDeployment;
        });
    }

    private void initiateStagedDeployment() {
        System.out.println("Enter stage initiation details. Format: organization ID (UUID), deployment ID (UUID), stage ID (UUID), and optional approval comment. The API derives the approver from the authenticated identity.");
        runOperation("Initiating staged deployment", () -> {
            String org = promptUuid("Organization ID", idOrNull(selectedOrganization));
            String deploymentId = promptUuid("Deployment ID", selectedDeployment == null
                    ? null : selectedDeployment.deploymentId().toString());
            String defaultStageId = null;
            if (selectedDeployment != null && selectedDeployment.deploymentId().toString().equals(deploymentId)
                    && selectedDeployment.stages() != null && !selectedDeployment.stages().isEmpty()) {
                DeploymentResponse.StageResponse first = selectedDeployment.stages().stream()
                        .min(Comparator.comparing(stage -> stage.stageNumber() == null
                                ? Integer.MAX_VALUE : stage.stageNumber()))
                        .orElse(null);
                if (first != null && first.stageId() != null) defaultStageId = first.stageId().toString();
            }
            String stageId = promptUuid("Stage ID", defaultStageId);
            String comment = prompt("Approval comment (optional)");
            DeploymentResponse result = managementSdk.startDeploymentStage(org, deploymentId, stageId, comment);
            if (result != null) selectedDeployment = result;
            return result;
        });
    }

    private void rollback() {
        System.out.println("Enter rollback details. Format: organization ID (UUID), application ID (UUID), current deployment ID (UUID), target stable deployment ID (UUID), and change summary.");
        runOperation("Creating rollback deployment", () -> managementSdk.rollback(
                promptUuid("Organization ID", idOrNull(selectedOrganization)), promptUuid("Application ID", idOrNull(selectedApplication)),
                promptUuid("Current deployment ID", null), promptUuid("Target stable deployment ID", null), promptName("Rollback change summary")));
    }

    private void fetchContinuously() {
        System.out.println("Enter read context. Format: organization ID (UUID), application ID (UUID), environment, region code.");
        String org = promptUuid("Organization ID", idOrNull(selectedOrganization));
        String app = promptUuid("Application ID", idOrNull(selectedApplication));
        String env = promptEnvironment("Environment", settings.environment);
        String region = promptRegion("Region code", selectedRegion == null ? settings.region : selectedRegion);
        CmsClientSdk readSdk = new CmsClientSdk(settings.sdkConfig(org, app, region, env));
        readSdk.registerCallback(snapshot -> {
            System.out.println("\n========== EFFECTIVE CONFIGURATION ==========");
            System.out.println("Fetched at: " + snapshot.fetchedAt());
            System.out.println("Versions: " + snapshot.versions());
            try { System.out.println(JSON.writeValueAsString(snapshot.effectiveConfig())); }
            catch (Exception ex) { System.out.println(snapshot.effectiveConfig()); }
            System.out.println("=============================================\n");
        });
        System.out.printf("Starting configuration retrieval. Initial request is in progress; subsequent polling every %d seconds. Press ENTER to stop.%n", settings.pollingIntervalSeconds);
        readSdk.start();
        try { in.nextLine(); } finally { readSdk.close(); }
        System.out.println("Polling stopped; returned to main menu.");
    }

    private List<CreateDeploymentRequest.StageRequest> promptStages() {
        while (true) {
            String raw = prompt("Rollout percentages [10,50,100]");
            if (raw.isBlank()) raw = "10,50,100";
            try {
                String[] parts = raw.split(",", -1);
                List<CreateDeploymentRequest.StageRequest> stages = new ArrayList<>();
                int previous = 0;
                for (int i = 0; i < parts.length; i++) {
                    int percentage = Integer.parseInt(parts[i].trim());
                    if (percentage <= previous || percentage > 100)
                        throw new IllegalArgumentException("Percentages must be strictly increasing and between 1 and 100.");
                    stages.add(new CreateDeploymentRequest.StageRequest(i + 1, percentage));
                    previous = percentage;
                }
                if (previous != 100) throw new IllegalArgumentException("The final rollout stage must target 100%.");
                return stages;
            } catch (Exception e) { System.out.println("Validation issue: " + e.getMessage() + " Please re-enter, for example 10,50,100."); }
        }
    }

    private Map<String, Object> promptConfig() {
        while (true) {
            System.out.println("Enter a single-line JSON object, for example: {\"featureFlags\":{\"newCheckout\":true},\"timeout\":30}");
            String raw = prompt("Configuration JSON");
            try {
                Map<String, Object> parsed = JSON.readValue(raw, new TypeReference<LinkedHashMap<String, Object>>() {});
                if (parsed == null) throw new IllegalArgumentException("JSON value must be an object, not null.");
                return parsed;
            } catch (Exception e) { System.out.println("Validation issue: enter a valid JSON object. " + e.getMessage()); }
        }
    }

    private String promptName(String label) {
        return promptValidated(label, null, value -> value.length() <= 200,
                "Value is required and must contain at most 200 characters.");
    }

    private String promptEnvironment(String label, String defaultValue) {
        return promptValidated(label, defaultValue, value -> value.matches("[A-Za-z0-9_-]{1,50}"),
                "Use 1-50 letters, digits, underscores, or hyphens (example: PROD).");
    }

    private String promptRegion(String label, String defaultValue) {
        return promptValidated(label, defaultValue, value -> value.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,62}"),
                "Use 1-63 letters, digits, dots, underscores, or hyphens (example: us-central1).");
    }

    private String promptUuid(String label, String defaultValue) {
        return promptValidated(label, defaultValue, value -> {
            try { UUID.fromString(value); return true; } catch (Exception e) { return false; }
        }, "Enter a valid UUID (example: 550e8400-e29b-41d4-a716-446655440000).");
    }

    private UUID promptOptionalUuid(String label) {
        while (true) {
            String value = prompt(label + " [optional UUID; press ENTER to skip]");
            if (value.isBlank()) return null;
            try { return UUID.fromString(value); }
            catch (Exception e) { System.out.println("Validation issue: enter a valid UUID or leave it blank."); }
        }
    }

    private String promptValidated(String label, String defaultValue, Predicate<String> valid, String error) {
        while (true) {
            String value = prompt(label + (defaultValue == null || defaultValue.isBlank() ? "" : " [" + defaultValue + "]"));
            if (value.isBlank()) value = defaultValue;
            if (value != null && !value.isBlank() && valid.test(value)) return value;
            System.out.println("Validation issue: " + error + " Please re-enter.");
        }
    }

    private String prompt(String label) { System.out.print(label + ": "); return in.nextLine().trim(); }

    private void runOperation(String description, java.util.concurrent.Callable<?> operation) {
        try {
            System.out.println(description + "... Please wait for the operation to complete.");
            Object result = operation.call();
            System.out.println("\nOperation completed successfully. Result:");
            try { System.out.println(JSON.writeValueAsString(result)); }
            catch (Exception e) { System.out.println(String.valueOf(result)); }
        } catch (Exception e) {
            System.out.println("\nOperation failed: " + safeMessage(e));
        }
    }

    private static String safeMessage(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null && (cause.getMessage() == null || cause.getMessage().isBlank())) cause = cause.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    private static String id(Object value) {
        if (value instanceof OrganizationResponse r) return r.organizationId().toString();
        if (value instanceof ProjectResponse r) return r.projectId().toString();
        if (value instanceof ApplicationResponse r) return r.applicationId().toString();
        return "(not selected)";
    }
    private static String idOrNull(Object value) { return value == null ? null : id(value); }

    private record Settings(String apiBaseUrl, String tokenUri, String clientId, String clientSecret,
                            String environment, String region, String instanceId,
                            long pollingIntervalSeconds, long requestTimeoutSeconds) {
        @SuppressWarnings("unchecked")
        static Settings load() {
            try (InputStream stream = CmsCliConsumerApplication.class.getClassLoader().getResourceAsStream("application.yml")) {
                if (stream == null) throw new IllegalStateException("application.yml is missing");
                Map<String, Object> root = new Yaml().load(stream);
                Map<String, Object> cms = (Map<String, Object>) root.get("cms");
                String secret = env("CMS_CLIENT_SECRET", str(cms, "client-secret"));
                if (secret == null || secret.isBlank() || "CHANGE_ME".equals(secret))
                    throw new IllegalStateException("Set CMS_CLIENT_SECRET environment variable.");
                return new Settings(str(cms, "api-base-url"), str(cms, "token-uri"),
                        env("CMS_CLIENT_ID", str(cms, "client-id")), secret,
                        env("CMS_ENVIRONMENT", str(cms, "environment")), env("CMS_REGION", str(cms, "region")),
                        env("CMS_INSTANCE_ID", str(cms, "instance-id")), num(cms, "polling-interval-seconds", 120),
                        num(cms, "request-timeout-seconds", 10));
            } catch (Exception e) { throw new IllegalStateException("Cannot load cms settings from application.yml: " + e.getMessage(), e); }
        }
        CmsClientSdkConfig sdkConfig(String org, String app, String region, String env) {
            return new CmsClientSdkConfig(URI.create(apiBaseUrl), URI.create(tokenUri), clientId, clientSecret,
                    org, app, env, region, instanceId, Duration.ofSeconds(pollingIntervalSeconds), Duration.ofSeconds(requestTimeoutSeconds));
        }
        private static String str(Map<String, Object> m, String k) { Object v = m.get(k); return v == null ? null : v.toString(); }
        private static long num(Map<String, Object> m, String k, long fallback) { Object v = m.get(k); return v instanceof Number n ? n.longValue() : fallback; }
        private static String env(String key, String fallback) { String v = System.getenv(key); return v == null || v.isBlank() ? fallback : v; }
    }
}
